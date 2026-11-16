package com.lifeos.finance.service;

import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.finance.domain.Recurrence;
import com.lifeos.finance.entity.FinancialAccount;
import com.lifeos.finance.entity.FinancialCategory;
import com.lifeos.finance.entity.FinancialTransaction;
import com.lifeos.finance.entity.RecurringTransaction;
import com.lifeos.finance.repository.FinanceJdbc;
import com.lifeos.finance.repository.FinancialAccountRepository;
import com.lifeos.finance.repository.FinancialCategoryRepository;
import com.lifeos.finance.repository.RecurringTransactionRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sinh giao dịch từ định kỳ (UC-FIN-05, D110, PHASE_3 §7). Mỗi lần xuất hiện là MỘT transaction ngắn:
 * <ol>
 *   <li>Khóa bản ghi định kỳ FOR UPDATE SKIP LOCKED (luồng/instance khác đang giữ → bỏ qua).</li>
 *   <li>Còn ACTIVE, nextRunOn ≤ hôm nay theo múi giờ chủ và ≤ endOn.</li>
 *   <li>INSERT giao dịch (recurring_transaction_id, recurrence_date) ON CONFLICT DO NOTHING — idempotent khi chạy lặp,
 *       song song, hoặc khi giao dịch đã sinh bị xóa (khóa UNIQUE gồm cả tombstone, FIN-11).</li>
 *   <li>nextRunOn = lần xuất hiện kế tiếp (tính từ startOn), version +1; ngưỡng ngân sách nếu là khoản chi.</li>
 *   <li>ChangeRecorder sau cùng: giao dịch CREATE và định kỳ UPDATE, mutation_id NULL (D86).</li>
 * </ol>
 * Tài khoản đã lưu trữ/không còn: bản ghi định kỳ được chuyển PAUSED kèm thông báo ({@link RecurringPauser}),
 * nextRunOn giữ nguyên — không lần xuất hiện nào bị bỏ lặng (đợt sửa F6, thay hành vi "bỏ qua" cũ). Mỗi lần chạy bù
 * tối đa 31 lần xuất hiện cho mỗi bản ghi.
 */
@Service
public class RecurringRunService {

    /** §7: tối đa 31 lần xuất hiện bù mỗi lần chạy. */
    public static final int MAX_CATCH_UP = 31;
    private static final int BATCH = 500;
    private static final Logger log = LoggerFactory.getLogger(RecurringRunService.class);

    private final RecurringTransactionRepository recurring;
    private final FinancialAccountRepository accounts;
    private final FinancialCategoryRepository categories;
    private final FinanceJdbc jdbc;
    private final BudgetAlerts alerts;
    private final RecurringPauser pauser;
    private final ChangeRecorder changeRecorder;
    private final UserCalendar calendar;
    private final TransactionTemplate tx;
    private final Clock clock;

    RecurringRunService(RecurringTransactionRepository recurring, FinancialAccountRepository accounts,
            FinancialCategoryRepository categories, FinanceJdbc jdbc, BudgetAlerts alerts, RecurringPauser pauser,
            ChangeRecorder changeRecorder, UserCalendar calendar, TransactionTemplate tx, Clock clock) {
        this.pauser = pauser;
        this.recurring = recurring;
        this.accounts = accounts;
        this.categories = categories;
        this.jdbc = jdbc;
        this.alerts = alerts;
        this.changeRecorder = changeRecorder;
        this.calendar = calendar;
        this.tx = tx;
        this.clock = clock;
    }

    /** Một lượt job: mọi bản ghi đến hạn, mỗi bản ghi tối đa {@link #MAX_CATCH_UP} lần. @return số giao dịch đã sinh */
    public int runOnce() {
        // Ngày muộn nhất có thể là "hôm nay" ở mọi múi giờ: UTC + 1 ngày (UTC+14 là sớm nhất).
        LocalDate latest = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC).plusDays(1);
        List<UUID> due = recurring.findDueIds(latest, Limit.of(BATCH));
        int generated = 0;
        for (UUID id : due) {
            generated += catchUp(id);
        }
        if (generated > 0) {
            log.atInfo().addKeyValue("event", "recurring_generated").addKeyValue("count", generated)
                    .log("recurring_generated");
        }
        return generated;
    }

    /** Bù tối đa 31 lần xuất hiện của một bản ghi. @return số giao dịch đã sinh */
    public int catchUp(UUID recurringId) {
        int generated = 0;
        for (int i = 0; i < MAX_CATCH_UP; i++) {
            Step step = tx.execute(status -> runOne(recurringId));
            if (step == null || step == Step.NOTHING) {
                break;
            }
            if (step == Step.GENERATED) {
                generated++;
            }
        }
        return generated;
    }

    enum Step {
        NOTHING, GENERATED, SKIPPED
    }

    private Step runOne(UUID recurringId) {
        RecurringTransaction r = recurring.lockForJob(recurringId).orElse(null);
        if (r == null || !"ACTIVE".equals(r.getStatus())) {
            return Step.NOTHING;
        }
        UUID userId = r.getUserId();
        LocalDate date = r.getNextRunOn();
        if (date.isAfter(calendar.today(userId)) || (r.getEndOn() != null && date.isAfter(r.getEndOn()))) {
            return Step.NOTHING;
        }
        FinancialAccount account = accounts.findActiveForShare(r.getAccountId(), userId).orElse(null);
        if (account == null || account.isArchived()) {
            // Đợt sửa F6: tài khoản không ghi được → TẠM DỪNG + báo người dùng, KHÔNG tiến nextRunOn (không mất lặng).
            RecurringTransaction paused = pauser.pause(r);
            changeRecorder.record(userId, RecurringTransaction.ENTITY_TYPE, paused.getId(), ChangeOperation.UPDATE,
                    paused.getVersion(), null);
            return Step.NOTHING;
        }
        UUID transactionId = UUID.randomUUID();
        FinancialCategory category = r.getCategoryId() == null ? null
                : categories.findActiveForShare(r.getCategoryId(), userId).orElse(null);
        // false chỉ khi lần xuất hiện này ĐÃ từng được sinh (ON CONFLICT, kể cả tombstone) — tiến con trỏ là đúng.
        boolean inserted = jdbc.insertOccurrence(new FinanceJdbc.Occurrence(transactionId, userId, r.getAccountId(),
                category == null ? null : category.getId(), r.getKind(), r.getAmount(), account.getCurrency(),
                date, r.getNote(), r.getId()), clock.instant());
        r.setNextRunOn(new Recurrence(r.getFrequency(), r.getIntervalCount(), r.getStartOn()).firstAfter(date));
        RecurringTransaction saved = recurring.saveAndFlush(r);
        if (inserted) {
            alerts.afterExpense(userId, r.getKind(), account.getCurrency(), category, date);
            changeRecorder.record(userId, FinancialTransaction.ENTITY_TYPE, transactionId, ChangeOperation.CREATE, 1L,
                    null);
        }
        changeRecorder.record(userId, RecurringTransaction.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                saved.getVersion(), null);
        return inserted ? Step.GENERATED : Step.SKIPPED;
    }
}
