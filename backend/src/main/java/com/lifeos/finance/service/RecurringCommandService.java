package com.lifeos.finance.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.common.util.Patch;
import com.lifeos.finance.domain.Recurrence;
import com.lifeos.finance.dto.RecurringResponse;
import com.lifeos.finance.entity.FinancialAccount;
import com.lifeos.finance.entity.RecurringTransaction;
import com.lifeos.finance.mapper.FinanceMapper;
import com.lifeos.finance.repository.RecurringTransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi giao dịch định kỳ (FR-FIN-07, D110, PHASE_3 §7). Client không tự sinh giao dịch; nextRunOn do server quản:
 * <ul>
 *   <li>Tạo: nextRunOn = startOn.</li>
 *   <li>Đổi lịch (frequency/intervalCount/startOn): nextRunOn = lần xuất hiện đầu tiên của lịch MỚI ≥ max(startOn,
 *       nextRunOn cũ) — không sinh lại các ngày đã qua [PROPOSED].</li>
 *   <li>Tiếp tục (PAUSED → ACTIVE): nextRunOn đẩy tới lần xuất hiện đầu tiên ≥ hôm nay (không bù lần bị lỡ).</li>
 * </ul>
 * Đổi amount/category/note/kind/account chỉ ảnh hưởng lần xuất hiện sau (giao dịch đã sinh giữ nguyên).
 */
@Service
public class RecurringCommandService {

    static final Set<String> STATUSES = Set.of("ACTIVE", "PAUSED");
    private static final String PK = "pk_recurring_transactions";
    private static final int INTERVAL_MAX = 365;

    private final RecurringTransactionRepository recurring;
    private final FinanceRefs refs;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final UserCalendar calendar;
    private final FinanceMapper mapper;
    private final Clock clock;

    public RecurringCommandService(RecurringTransactionRepository recurring, FinanceRefs refs,
            ChangeRecorder changeRecorder, VersionedWrites versionedWrites, UserCalendar calendar, FinanceMapper mapper,
            Clock clock) {
        this.recurring = recurring;
        this.refs = refs;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.calendar = calendar;
        this.mapper = mapper;
        this.clock = clock;
    }

    public RecurringResponse create(UUID userId, FinanceCommands.CreateRecurring cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (recurring.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                LocalDate today = calendar.today(userId);
                String kind = FinanceValues.oneOf(cmd.kind().value(), FinanceValues.KINDS, "kind");
                UUID accountId = FinanceValues.requiredId(cmd.accountId().value(), "accountId");
                UUID categoryId = cmd.categoryId().value();
                String rawAmount = cmd.amount().value();
                String note = FinanceValues.note(cmd.note().value());
                String frequency = FinanceValues.oneOf(cmd.frequency().value(), FinanceValues.FREQUENCIES,
                        "frequency");
                int interval = cmd.intervalCount().present() && cmd.intervalCount().value() == null ? 1
                        : interval(cmd.intervalCount(), 1);
                LocalDate startOn = FinanceValues.businessDate(cmd.startOn().value(), "startOn", today);
                LocalDate endOn = endOn(FinanceValues.date(cmd.endOn().value(), "endOn"), startOn);
                String status = cmd.status().value() == null ? "ACTIVE"
                        : FinanceValues.oneOf(cmd.status().value(), STATUSES, "status");
                FinancialAccount account = refs.writableAccount(userId, accountId);
                BigDecimal amount = FinanceValues.amount(rawAmount, account.getCurrency(), "amount");
                refs.category(userId, categoryId, kind);
                RecurringTransaction r = new RecurringTransaction(cmd.id(), userId);
                r.setAccountId(accountId);
                r.setCategoryId(categoryId);
                r.setKind(kind);
                r.setAmount(amount);
                r.setCurrency(account.getCurrency());
                r.setNote(note);
                r.setFrequency(frequency);
                r.setIntervalCount((short) interval);
                r.setStartOn(startOn);
                r.setEndOn(endOn);
                r.setStatus(status);
                r.setNextRunOn("ACTIVE".equals(status) ? startOn
                        : new Recurrence(frequency, interval, startOn).firstOnOrAfter(today));
                RecurringTransaction saved = recurring.saveAndFlush(r);
                changeRecorder.record(userId, RecurringTransaction.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (FinanceValues.mentions(e, PK)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    public RecurringResponse update(UUID userId, UUID recurringId, FinanceCommands.UpdateRecurring cmd,
            UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            RecurringTransaction r = recurring.findActiveForUpdate(recurringId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_RECURRING_NOT_FOUND));
            VersionedWrites.requireVersion(r, cmd.expectedVersion());
            LocalDate today = calendar.today(userId);
            String kind = cmd.kind().present()
                    ? FinanceValues.oneOf(cmd.kind().value(), FinanceValues.KINDS, "kind") : r.getKind();
            UUID accountId = cmd.accountId().present()
                    ? FinanceValues.requiredId(cmd.accountId().value(), "accountId") : r.getAccountId();
            UUID categoryId = FinanceValues.pick(cmd.categoryId(), r.getCategoryId());
            if (cmd.amount().isExplicitNull()) {
                throw FinanceValues.invalid("amount", "REQUIRED", "Số tiền là bắt buộc.");
            }
            String note = cmd.note().present() ? FinanceValues.note(cmd.note().value()) : r.getNote();
            String frequency = cmd.frequency().present()
                    ? FinanceValues.oneOf(cmd.frequency().value(), FinanceValues.FREQUENCIES, "frequency")
                    : r.getFrequency();
            int interval = interval(cmd.intervalCount(), r.getIntervalCount());
            LocalDate startOn = cmd.startOn().present()
                    ? FinanceValues.businessDate(cmd.startOn().value(), "startOn", today) : r.getStartOn();
            LocalDate endOn = endOn(cmd.endOn().present() ? FinanceValues.date(cmd.endOn().value(), "endOn")
                    : r.getEndOn(), startOn);
            String status = cmd.status().present()
                    ? FinanceValues.oneOf(cmd.status().value(), STATUSES, "status") : r.getStatus();
            boolean accountChanged = !accountId.equals(r.getAccountId());
            FinancialAccount account = accountChanged ? refs.writableAccount(userId, accountId)
                    : refs.account(userId, accountId);
            if (accountChanged && !account.getCurrency().equals(r.getCurrency())) {
                // D103 (đợt sửa F1): đổi sang tài khoản khác tiền tệ bị từ chối, không tái định giá ngầm.
                throw FinanceValues.business(ErrorCode.FINANCE_CURRENCY_MISMATCH);
            }
            if ("PAUSED".equals(r.getStatus()) && "ACTIVE".equals(status) && account.isArchived()) {
                // Đợt sửa F6: không tiếp tục định kỳ trên tài khoản đã lưu trữ (mở lại tài khoản trước).
                throw FinanceValues.business(ErrorCode.FINANCE_ACCOUNT_ARCHIVED);
            }
            BigDecimal amount;
            if (cmd.amount().present()) {
                amount = FinanceValues.amount(cmd.amount().value(), account.getCurrency(), "amount");
            } else {
                amount = r.getAmount();
                FinanceValues.requireFits(amount, account.getCurrency(), "amount");
            }
            refs.category(userId, categoryId, kind);

            Recurrence rule = new Recurrence(frequency, interval, startOn);
            boolean scheduleChanged = !frequency.equals(r.getFrequency()) || interval != r.getIntervalCount()
                    || !startOn.equals(r.getStartOn());
            LocalDate next = r.getNextRunOn();
            if (scheduleChanged) {
                next = rule.firstOnOrAfter(startOn.isAfter(next) ? startOn : next);
            }
            if ("PAUSED".equals(r.getStatus()) && "ACTIVE".equals(status)) {
                LocalDate firstFromToday = rule.firstOnOrAfter(today);
                next = firstFromToday.isAfter(next) ? firstFromToday : next;
            }
            boolean changed = r.setAccountId(accountId);
            changed |= r.setCategoryId(categoryId);
            changed |= r.setKind(kind);
            changed |= r.setAmount(amount);
            changed |= r.setCurrency(account.getCurrency());
            changed |= r.setNote(note);
            changed |= r.setFrequency(frequency);
            changed |= r.setIntervalCount((short) interval);
            changed |= r.setStartOn(startOn);
            changed |= r.setEndOn(endOn);
            changed |= r.setStatus(status);
            changed |= r.setNextRunOn(next);
            if (!changed) {
                return mapper.toResponse(r);
            }
            RecurringTransaction saved = recurring.saveAndFlush(r);
            changeRecorder.record(userId, RecurringTransaction.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, recurringId));
    }

    public RecurringResponse pause(UUID userId, UUID recurringId, long expectedVersion) {
        return update(userId, recurringId, statusOnly(expectedVersion, "PAUSED"), null);
    }

    public RecurringResponse resume(UUID userId, UUID recurringId, long expectedVersion) {
        return update(userId, recurringId, statusOnly(expectedVersion, "ACTIVE"), null);
    }

    /** Xóa mềm bản ghi định kỳ; giao dịch đã sinh giữ nguyên (§7). @return version tombstone */
    public long delete(UUID userId, UUID recurringId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            RecurringTransaction r = recurring.findActiveForUpdate(recurringId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_RECURRING_NOT_FOUND));
            VersionedWrites.requireVersion(r, expectedVersion);
            r.softDelete(clock.instant());
            RecurringTransaction saved = recurring.saveAndFlush(r);
            changeRecorder.record(userId, RecurringTransaction.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, recurringId));
    }

    private static FinanceCommands.UpdateRecurring statusOnly(long expectedVersion, String status) {
        return new FinanceCommands.UpdateRecurring(expectedVersion, Patch.absent(), Patch.absent(), Patch.absent(),
                Patch.absent(), Patch.absent(), Patch.absent(), Patch.absent(), Patch.absent(), Patch.absent(),
                Patch.of(status));
    }

    private static int interval(Patch<Integer> patch, int fallback) {
        if (!patch.present()) {
            return fallback;
        }
        Integer value = patch.value();
        if (value == null) {
            throw FinanceValues.invalid("intervalCount", "REQUIRED", "intervalCount là bắt buộc.");
        }
        if (value < 1 || value > INTERVAL_MAX) {
            throw FinanceValues.invalid("intervalCount", "OUT_OF_RANGE", "intervalCount từ 1 đến " + INTERVAL_MAX + ".");
        }
        return value;
    }

    private static LocalDate endOn(LocalDate endOn, LocalDate startOn) {
        if (endOn != null && endOn.isBefore(startOn)) {
            throw FinanceValues.invalid("endOn", "OUT_OF_RANGE", "endOn không được trước startOn.");
        }
        return endOn;
    }

    private long currentVersion(UUID userId, UUID recurringId) {
        return recurring.findVersion(recurringId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_RECURRING_NOT_FOUND));
    }
}
