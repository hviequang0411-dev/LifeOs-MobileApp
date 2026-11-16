package com.lifeos.finance.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.finance.dto.TransferResponse;
import com.lifeos.finance.entity.AccountTransfer;
import com.lifeos.finance.entity.FinancialAccount;
import com.lifeos.finance.mapper.FinanceMapper;
import com.lifeos.finance.repository.AccountTransferRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi chuyển khoản (FR-FIN-05, D104, D107): hai tài khoản khác nhau, cùng tiền tệ, chưa lưu trữ; không là thu/chi
 * (không vào ngân sách, báo cáo). Hai tài khoản bất biến sau khi tạo. Khóa FOR SHARE hai tài khoản theo id tăng dần.
 */
@Service
public class TransferCommandService {

    private static final String PK = "pk_account_transfers";

    private final AccountTransferRepository transfers;
    private final FinanceRefs refs;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final UserCalendar calendar;
    private final FinanceMapper mapper;
    private final Clock clock;

    public TransferCommandService(AccountTransferRepository transfers, FinanceRefs refs, ChangeRecorder changeRecorder,
            VersionedWrites versionedWrites, UserCalendar calendar, FinanceMapper mapper, Clock clock) {
        this.transfers = transfers;
        this.refs = refs;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.calendar = calendar;
        this.mapper = mapper;
        this.clock = clock;
    }

    public TransferResponse create(UUID userId, FinanceCommands.CreateTransfer cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (transfers.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                UUID fromId = FinanceValues.requiredId(cmd.fromAccountId().value(), "fromAccountId");
                UUID toId = FinanceValues.requiredId(cmd.toAccountId().value(), "toAccountId");
                if (fromId.equals(toId)) {
                    throw FinanceValues.invalid("toAccountId", "INVALID_VALUE",
                            "Tài khoản nguồn và đích phải khác nhau.");
                }
                String rawAmount = cmd.amount().value();
                LocalDate occurredOn = FinanceValues.businessDate(cmd.occurredOn().value(), "occurredOn",
                        calendar.today(userId));
                String note = FinanceValues.note(cmd.note().value());
                // Thứ tự khóa cố định: id nhỏ trước.
                boolean fromFirst = fromId.compareTo(toId) < 0;
                FinancialAccount first = refs.writableAccount(userId, fromFirst ? fromId : toId);
                FinancialAccount second = refs.writableAccount(userId, fromFirst ? toId : fromId);
                FinancialAccount from = fromFirst ? first : second;
                FinancialAccount to = fromFirst ? second : first;
                if (!from.getCurrency().equals(to.getCurrency())) {
                    throw FinanceValues.business(ErrorCode.FINANCE_CURRENCY_MISMATCH);
                }
                BigDecimal amount = FinanceValues.amount(rawAmount, from.getCurrency(), "amount");
                AccountTransfer transfer = AccountTransfer.create(cmd.id(), userId, fromId, toId);
                transfer.setAmount(amount);
                transfer.setCurrency(from.getCurrency());
                transfer.setOccurredOn(occurredOn);
                transfer.setNote(note);
                AccountTransfer saved = transfers.saveAndFlush(transfer);
                changeRecorder.record(userId, AccountTransfer.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
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

    public TransferResponse update(UUID userId, UUID transferId, FinanceCommands.UpdateTransfer cmd,
            UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            AccountTransfer transfer = transfers.findActive(transferId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_TRANSFER_NOT_FOUND));
            VersionedWrites.requireVersion(transfer, cmd.expectedVersion());
            FinanceValues.immutable(cmd.fromAccountId(), transfer.getFromAccountId(), "fromAccountId");
            FinanceValues.immutable(cmd.toAccountId(), transfer.getToAccountId(), "toAccountId");
            boolean changed = false;
            if (cmd.amount().present()) {
                changed |= transfer.setAmount(FinanceValues.amount(cmd.amount().value(), transfer.getCurrency(),
                        "amount"));
            }
            if (cmd.occurredOn().present()) {
                changed |= transfer.setOccurredOn(FinanceValues.businessDate(cmd.occurredOn().value(), "occurredOn",
                        calendar.today(userId)));
            }
            if (cmd.note().present()) {
                changed |= transfer.setNote(FinanceValues.note(cmd.note().value()));
            }
            if (!changed) {
                return mapper.toResponse(transfer);
            }
            AccountTransfer saved = transfers.saveAndFlush(transfer);
            changeRecorder.record(userId, AccountTransfer.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, transferId));
    }

    /** @return version tombstone */
    public long delete(UUID userId, UUID transferId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            AccountTransfer transfer = transfers.findActive(transferId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_TRANSFER_NOT_FOUND));
            VersionedWrites.requireVersion(transfer, expectedVersion);
            transfer.softDelete(clock.instant());
            AccountTransfer saved = transfers.saveAndFlush(transfer);
            changeRecorder.record(userId, AccountTransfer.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, transferId));
    }

    private long currentVersion(UUID userId, UUID transferId) {
        return transfers.findVersion(transferId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_TRANSFER_NOT_FOUND));
    }
}
