package com.lifeos.finance.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.finance.dto.TransactionResponse;
import com.lifeos.finance.entity.FinancialAccount;
import com.lifeos.finance.entity.FinancialCategory;
import com.lifeos.finance.entity.FinancialTransaction;
import com.lifeos.finance.mapper.FinanceMapper;
import com.lifeos.finance.repository.FinancialTransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi giao dịch thu/chi (FR-FIN-04, D101, D103, D107, D108). currency luôn lấy từ tài khoản (gửi lên bị bỏ qua);
 * recurringTransactionId/recurrenceDate chỉ do job đặt. Thứ tự: tồn tại → version → giá trị → tham chiếu (tài khoản
 * FOR SHARE, danh mục FOR SHARE) → áp dụng + flush → ngưỡng ngân sách → ChangeRecorder (D86, D96).
 */
@Service
public class TransactionCommandService {

    private static final String PK = "pk_financial_transactions";

    private final FinancialTransactionRepository transactions;
    private final FinanceRefs refs;
    private final BudgetAlerts alerts;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final UserCalendar calendar;
    private final FinanceMapper mapper;
    private final Clock clock;

    public TransactionCommandService(FinancialTransactionRepository transactions, FinanceRefs refs,
            BudgetAlerts alerts, ChangeRecorder changeRecorder, VersionedWrites versionedWrites, UserCalendar calendar,
            FinanceMapper mapper, Clock clock) {
        this.transactions = transactions;
        this.refs = refs;
        this.alerts = alerts;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.calendar = calendar;
        this.mapper = mapper;
        this.clock = clock;
    }

    public TransactionResponse create(UUID userId, FinanceCommands.CreateTransaction cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (transactions.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String kind = FinanceValues.oneOf(cmd.kind().value(), FinanceValues.KINDS, "kind");
                UUID accountId = FinanceValues.requiredId(cmd.accountId().value(), "accountId");
                UUID categoryId = cmd.categoryId().value();
                String rawAmount = cmd.amount().value();
                LocalDate occurredOn = FinanceValues.businessDate(cmd.occurredOn().value(), "occurredOn",
                        calendar.today(userId));
                String note = FinanceValues.note(cmd.note().value());
                FinancialAccount account = refs.writableAccount(userId, accountId);
                BigDecimal amount = FinanceValues.amount(rawAmount, account.getCurrency(), "amount");
                FinancialCategory category = refs.category(userId, categoryId, kind);
                FinancialTransaction tx = new FinancialTransaction(cmd.id(), userId);
                tx.setAccountId(accountId);
                tx.setCategoryId(categoryId);
                tx.setKind(kind);
                tx.setAmount(amount);
                tx.setCurrency(account.getCurrency());
                tx.setOccurredOn(occurredOn);
                tx.setNote(note);
                FinancialTransaction saved = transactions.saveAndFlush(tx);
                alerts.afterExpense(userId, kind, saved.getCurrency(), category, occurredOn);
                changeRecorder.record(userId, FinancialTransaction.ENTITY_TYPE, saved.getId(),
                        ChangeOperation.CREATE, saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (FinanceValues.mentions(e, PK)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    /**
     * JSON Merge Patch. Đổi tài khoản: tài khoản mới không được lưu trữ và PHẢI cùng tiền tệ với giao dịch — khác tiền
     * tệ bị từ chối 422 FINANCE_CURRENCY_MISMATCH kể cả khi gửi kèm amount (D103, đợt sửa F1). Sửa giao dịch của tài khoản đã lưu trữ (không đổi tài khoản) vẫn được phép [PROPOSED].
     */
    public TransactionResponse update(UUID userId, UUID transactionId, FinanceCommands.UpdateTransaction cmd,
            UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            FinancialTransaction tx = transactions.findActive(transactionId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_TRANSACTION_NOT_FOUND));
            VersionedWrites.requireVersion(tx, expectedVersion(cmd));
            String kind = cmd.kind().present()
                    ? FinanceValues.oneOf(cmd.kind().value(), FinanceValues.KINDS, "kind") : tx.getKind();
            UUID accountId = cmd.accountId().present()
                    ? FinanceValues.requiredId(cmd.accountId().value(), "accountId") : tx.getAccountId();
            UUID categoryId = FinanceValues.pick(cmd.categoryId(), tx.getCategoryId());
            String rawAmount = cmd.amount().present() ? cmd.amount().value() : null;
            if (cmd.amount().present() && rawAmount == null) {
                throw FinanceValues.invalid("amount", "REQUIRED", "Số tiền là bắt buộc.");
            }
            LocalDate occurredOn = cmd.occurredOn().present()
                    ? FinanceValues.businessDate(cmd.occurredOn().value(), "occurredOn", calendar.today(userId))
                    : tx.getOccurredOn();
            String note = cmd.note().present() ? FinanceValues.note(cmd.note().value()) : tx.getNote();
            boolean accountChanged = !accountId.equals(tx.getAccountId());
            FinancialAccount account = accountChanged ? refs.writableAccount(userId, accountId)
                    : refs.account(userId, accountId);
            if (accountChanged && !account.getCurrency().equals(tx.getCurrency())) {
                // D103 (đợt sửa F1): không tái định giá ngầm — 100 USD không được thành 100 VND.
                throw FinanceValues.business(ErrorCode.FINANCE_CURRENCY_MISMATCH);
            }
            BigDecimal amount;
            if (rawAmount != null) {
                amount = FinanceValues.amount(rawAmount, account.getCurrency(), "amount");
            } else {
                amount = tx.getAmount();
                FinanceValues.requireFits(amount, account.getCurrency(), "amount");
            }
            FinancialCategory category = refs.category(userId, categoryId, kind);
            boolean changed = tx.setAccountId(accountId);
            changed |= tx.setCategoryId(categoryId);
            changed |= tx.setKind(kind);
            changed |= tx.setAmount(amount);
            changed |= tx.setCurrency(account.getCurrency());
            changed |= tx.setOccurredOn(occurredOn);
            changed |= tx.setNote(note);
            if (!changed) {
                return mapper.toResponse(tx);
            }
            FinancialTransaction saved = transactions.saveAndFlush(tx);
            alerts.afterExpense(userId, kind, saved.getCurrency(), category, occurredOn);
            changeRecorder.record(userId, FinancialTransaction.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, transactionId));
    }

    /** @return version tombstone */
    public long delete(UUID userId, UUID transactionId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            FinancialTransaction tx = transactions.findActive(transactionId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_TRANSACTION_NOT_FOUND));
            VersionedWrites.requireVersion(tx, expectedVersion);
            tx.softDelete(clock.instant());
            FinancialTransaction saved = transactions.saveAndFlush(tx);
            changeRecorder.record(userId, FinancialTransaction.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, transactionId));
    }

    private static long expectedVersion(FinanceCommands.UpdateTransaction cmd) {
        return cmd.expectedVersion();
    }

    private long currentVersion(UUID userId, UUID transactionId) {
        return transactions.findVersion(transactionId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_TRANSACTION_NOT_FOUND));
    }
}
