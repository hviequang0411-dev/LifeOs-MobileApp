package com.lifeos.finance.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.finance.dto.AccountResponse;
import com.lifeos.finance.entity.FinancialAccount;
import com.lifeos.finance.entity.RecurringTransaction;
import com.lifeos.finance.mapper.FinanceMapper;
import com.lifeos.finance.repository.AccountTransferRepository;
import com.lifeos.finance.repository.FinancialAccountRepository;
import com.lifeos.finance.repository.FinancialTransactionRepository;
import com.lifeos.finance.repository.RecurringTransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi tài khoản (FR-FIN-01/02, D103, D107). Mọi lệnh khóa dòng tài khoản FOR UPDATE: lệnh ghi giao dịch/chuyển
 * khoản/định kỳ giữ FOR SHARE trên tài khoản đến khi commit, nên kiểm "đã có giao dịch" (xóa, đổi tiền tệ) không đua.
 * Thứ tự D96: tồn tại → version → giá trị → quy tắc → áp dụng; ChangeRecorder sau cùng (D86).
 */
@Service
public class AccountCommandService {

    static final int NAME_MAX = 60;
    private static final String PK = "pk_financial_accounts";
    private static final String NAME_UNIQUE = "ux_financial_accounts_user_name";

    private final FinancialAccountRepository accounts;
    private final FinancialTransactionRepository transactions;
    private final AccountTransferRepository transfers;
    private final RecurringTransactionRepository recurring;
    private final RecurringPauser pauser;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final FinanceMapper mapper;
    private final Clock clock;

    AccountCommandService(FinancialAccountRepository accounts, FinancialTransactionRepository transactions,
            AccountTransferRepository transfers, RecurringTransactionRepository recurring, RecurringPauser pauser,
            ChangeRecorder changeRecorder, VersionedWrites versionedWrites, FinanceMapper mapper, Clock clock) {
        this.pauser = pauser;
        this.accounts = accounts;
        this.transactions = transactions;
        this.transfers = transfers;
        this.recurring = recurring;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
    }

    public AccountResponse create(UUID userId, FinanceCommands.CreateAccount cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (accounts.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String name = FinanceValues.name(cmd.name().value(), NAME_MAX);
                String type = FinanceValues.oneOf(cmd.accountType().value(), FinanceValues.ACCOUNT_TYPES,
                        "accountType");
                String currency = FinanceValues.currency(cmd.currency().value(), "currency");
                String rawOpening = cmd.openingBalance().value();
                BigDecimal opening = rawOpening == null ? BigDecimal.ZERO
                        : FinanceValues.signedAmount(rawOpening, currency, "openingBalance");
                if (accounts.nameTaken(userId, name, cmd.id())) {
                    throw FinanceValues.conflict(ErrorCode.FINANCE_NAME_TAKEN);
                }
                FinancialAccount account = new FinancialAccount(cmd.id(), userId);
                account.setName(name);
                account.setAccountType(type);
                account.setCurrency(currency);
                account.setOpeningBalance(opening);
                FinancialAccount saved = accounts.saveAndFlush(account);
                changeRecorder.record(userId, FinancialAccount.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    /** JSON Merge Patch; archived chỉ có ở payload sync (REST dùng /archive, /unarchive). */
    public AccountResponse update(UUID userId, UUID accountId, FinanceCommands.UpdateAccount cmd,
            UUID mutationIdOrNull) {
        try {
            boolean archiving = cmd.archived().present() && Boolean.TRUE.equals(cmd.archived().value());
            return writeExisting(userId, accountId, cmd.expectedVersion(), mutationIdOrNull, archiving, account -> {
                boolean changed = false;
                if (cmd.name().present()) {
                    String name = FinanceValues.name(cmd.name().value(), NAME_MAX);
                    if (!name.equals(account.getName()) && accounts.nameTaken(userId, name, accountId)) {
                        throw FinanceValues.conflict(ErrorCode.FINANCE_NAME_TAKEN);
                    }
                    changed |= account.setName(name);
                }
                if (cmd.accountType().present()) {
                    changed |= account.setAccountType(FinanceValues.oneOf(cmd.accountType().value(),
                            FinanceValues.ACCOUNT_TYPES, "accountType"));
                }
                String currency = account.getCurrency();
                if (cmd.currency().present()) {
                    currency = FinanceValues.currency(cmd.currency().value(), "currency");
                }
                BigDecimal opening = cmd.openingBalance().present()
                        ? FinanceValues.signedAmount(cmd.openingBalance().value(), currency, "openingBalance")
                        : account.getOpeningBalance();
                if (!currency.equals(account.getCurrency())) {
                    // FR-FIN-01 / FIN-08: tiền tệ không đổi khi đã có giao dịch, chuyển khoản hoặc định kỳ còn sống.
                    if (inUse(userId, accountId)) {
                        throw FinanceValues.conflict(ErrorCode.FINANCE_CURRENCY_LOCKED);
                    }
                    FinanceValues.requireFits(opening, currency, "openingBalance");
                    changed |= account.setCurrency(currency);
                }
                changed |= account.setOpeningBalance(opening);
                if (cmd.archived().isExplicitNull()) {
                    throw FinanceValues.invalid("archived", "REQUIRED", "archived phải là true hoặc false.");
                }
                if (cmd.archived().present()) {
                    changed |= cmd.archived().value() ? account.archive(clock.instant()) : account.unarchive();
                }
                return changed;
            });
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    public AccountResponse archive(UUID userId, UUID accountId, long expectedVersion) {
        return writeExisting(userId, accountId, expectedVersion, null, true, a -> a.archive(clock.instant()));
    }

    public AccountResponse unarchive(UUID userId, UUID accountId, long expectedVersion) {
        return writeExisting(userId, accountId, expectedVersion, null, false, FinancialAccount::unarchive);
    }

    /**
     * FR-FIN-02: chỉ xóa khi không còn giao dịch, chuyển khoản hay giao dịch định kỳ nào CHƯA xóa; ngược lại 409
     * FINANCE_ACCOUNT_HAS_TRANSACTIONS (gợi ý lưu trữ).
     *
     * @return version tombstone
     */
    public long delete(UUID userId, UUID accountId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            FinancialAccount account = accounts.findActiveForUpdate(accountId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_ACCOUNT_NOT_FOUND));
            VersionedWrites.requireVersion(account, expectedVersion);
            if (inUse(userId, accountId)) {
                throw FinanceValues.conflict(ErrorCode.FINANCE_ACCOUNT_HAS_TRANSACTIONS);
            }
            account.softDelete(clock.instant());
            FinancialAccount saved = accounts.saveAndFlush(account);
            changeRecorder.record(userId, FinancialAccount.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, accountId));
    }

    private boolean inUse(UUID userId, UUID accountId) {
        return transactions.existsLiveForAccount(userId, accountId) || transfers.existsLiveForAccount(userId, accountId)
                || recurring.existsLiveForAccount(userId, accountId);
    }

    /**
     * {@code archiving}: lệnh có thể lưu trữ tài khoản. Khi đó các định kỳ ACTIVE của tài khoản được khóa TRƯỚC khóa
     * tài khoản (thứ tự "định kỳ → tài khoản" như lệnh sửa định kỳ và job, tránh deadlock) và được TẠM DỪNG kèm thông
     * báo trong cùng transaction (đợt sửa F6). Định kỳ tạo/chuyển vào đúng lúc đua qua được lưới an toàn của job xử lý.
     */
    private AccountResponse writeExisting(UUID userId, UUID accountId, long expectedVersion, UUID mutationIdOrNull,
            boolean archiving, Function<FinancialAccount, Boolean> change) {
        return versionedWrites.write(() -> {
            List<RecurringTransaction> dependents = archiving ? recurring.lockActiveForAccount(userId, accountId)
                    : List.of();
            FinancialAccount account = accounts.findActiveForUpdate(accountId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_ACCOUNT_NOT_FOUND));
            VersionedWrites.requireVersion(account, expectedVersion);
            if (!change.apply(account)) {
                return mapper.toResponse(account);
            }
            FinancialAccount saved = accounts.saveAndFlush(account);
            List<RecurringTransaction> paused = new ArrayList<>();
            if (saved.isArchived()) {
                for (RecurringTransaction r : dependents) {
                    paused.add(pauser.pause(r));
                }
            }
            // D86: mọi ghi nghiệp vụ (tài khoản, định kỳ, outbox) xong rồi mới tới ChangeRecorder.
            changeRecorder.record(userId, FinancialAccount.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            for (RecurringTransaction r : paused) {
                changeRecorder.record(userId, RecurringTransaction.ENTITY_TYPE, r.getId(), ChangeOperation.UPDATE,
                        r.getVersion(), null);
            }
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, accountId));
    }

    private long currentVersion(UUID userId, UUID accountId) {
        return accounts.findVersion(accountId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_ACCOUNT_NOT_FOUND));
    }

    private static RuntimeException translate(DataIntegrityViolationException e) {
        if (FinanceValues.mentions(e, PK)) {
            return new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
        }
        if (FinanceValues.mentions(e, NAME_UNIQUE)) {
            return FinanceValues.conflict(ErrorCode.FINANCE_NAME_TAKEN);
        }
        return e;
    }
}
