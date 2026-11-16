package com.lifeos.finance.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.finance.dto.BudgetResponse;
import com.lifeos.finance.entity.Budget;
import com.lifeos.finance.mapper.FinanceMapper;
import com.lifeos.finance.repository.BudgetRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi ngân sách (FR-FIN-06, D109): tổng chi (categoryId null) hoặc một danh mục CHI, theo một tiền tệ. Duy nhất một
 * ngân sách đang hoạt động cho mỗi (user, danh mục hoặc tổng, tiền tệ) → 409 FINANCE_BUDGET_EXISTS (kiểm trước, chỉ mục
 * ux_budgets_active chặn khi đua).
 */
@Service
public class BudgetCommandService {

    private static final String PK = "pk_budgets";
    private static final String ACTIVE_UNIQUE = "ux_budgets_active";

    private final BudgetRepository budgets;
    private final FinanceRefs refs;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final FinanceMapper mapper;
    private final Clock clock;

    public BudgetCommandService(BudgetRepository budgets, FinanceRefs refs, ChangeRecorder changeRecorder,
            VersionedWrites versionedWrites, FinanceMapper mapper, Clock clock) {
        this.budgets = budgets;
        this.refs = refs;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
    }

    public BudgetResponse create(UUID userId, FinanceCommands.CreateBudget cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (budgets.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String currency = FinanceValues.currency(cmd.currency().value(), "currency");
                BigDecimal amount = FinanceValues.amount(cmd.amount().value(), currency, "amount");
                UUID categoryId = cmd.categoryId().value();
                refs.category(userId, categoryId, FinanceValues.EXPENSE);
                if (budgets.activeExists(userId, categoryId, currency, cmd.id())) {
                    throw FinanceValues.conflict(ErrorCode.FINANCE_BUDGET_EXISTS);
                }
                Budget budget = new Budget(cmd.id(), userId);
                budget.setCategoryId(categoryId);
                budget.setCurrency(currency);
                budget.setAmount(amount);
                Budget saved = budgets.saveAndFlush(budget);
                changeRecorder.record(userId, Budget.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    public BudgetResponse update(UUID userId, UUID budgetId, FinanceCommands.UpdateBudget cmd, UUID mutationIdOrNull) {
        try {
            return writeExisting(userId, budgetId, cmd.expectedVersion(), mutationIdOrNull, budget -> {
                String currency = cmd.currency().present()
                        ? FinanceValues.currency(cmd.currency().value(), "currency") : budget.getCurrency();
                BigDecimal amount;
                if (cmd.amount().present()) {
                    amount = FinanceValues.amount(cmd.amount().value(), currency, "amount");
                } else {
                    amount = budget.getAmount();
                    FinanceValues.requireFits(amount, currency, "amount");
                }
                UUID categoryId = FinanceValues.pick(cmd.categoryId(), budget.getCategoryId());
                if (cmd.categoryId().present()) {
                    refs.category(userId, categoryId, FinanceValues.EXPENSE);
                }
                if (cmd.archived().isExplicitNull()) {
                    throw FinanceValues.invalid("archived", "REQUIRED", "archived phải là true hoặc false.");
                }
                boolean changed = budget.setCurrency(currency);
                changed |= budget.setAmount(amount);
                changed |= budget.setCategoryId(categoryId);
                if (cmd.archived().present()) {
                    changed |= cmd.archived().value() ? budget.archive(clock.instant()) : budget.unarchive();
                }
                if (changed && !budget.isArchived()
                        && budgets.activeExists(userId, budget.getCategoryId(), budget.getCurrency(), budgetId)) {
                    throw FinanceValues.conflict(ErrorCode.FINANCE_BUDGET_EXISTS);
                }
                return changed;
            });
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    public BudgetResponse archive(UUID userId, UUID budgetId, long expectedVersion) {
        return writeExisting(userId, budgetId, expectedVersion, null, b -> b.archive(clock.instant()));
    }

    public BudgetResponse unarchive(UUID userId, UUID budgetId, long expectedVersion) {
        try {
            return writeExisting(userId, budgetId, expectedVersion, null, budget -> {
                if (!budget.unarchive()) {
                    return false;
                }
                if (budgets.activeExists(userId, budget.getCategoryId(), budget.getCurrency(), budgetId)) {
                    throw FinanceValues.conflict(ErrorCode.FINANCE_BUDGET_EXISTS);
                }
                return true;
            });
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    /** @return version tombstone. Lịch sử mức dùng tính lại từ giao dịch nên không có gì để cascade. */
    public long delete(UUID userId, UUID budgetId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            Budget budget = budgets.findActive(budgetId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_BUDGET_NOT_FOUND));
            VersionedWrites.requireVersion(budget, expectedVersion);
            budget.softDelete(clock.instant());
            Budget saved = budgets.saveAndFlush(budget);
            changeRecorder.record(userId, Budget.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, budgetId));
    }

    private BudgetResponse writeExisting(UUID userId, UUID budgetId, long expectedVersion, UUID mutationIdOrNull,
            Function<Budget, Boolean> change) {
        return versionedWrites.write(() -> {
            Budget budget = budgets.findActive(budgetId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_BUDGET_NOT_FOUND));
            VersionedWrites.requireVersion(budget, expectedVersion);
            if (!change.apply(budget)) {
                return mapper.toResponse(budget);
            }
            Budget saved = budgets.saveAndFlush(budget);
            changeRecorder.record(userId, Budget.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, budgetId));
    }

    private long currentVersion(UUID userId, UUID budgetId) {
        return budgets.findVersion(budgetId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_BUDGET_NOT_FOUND));
    }

    private static RuntimeException translate(DataIntegrityViolationException e) {
        if (FinanceValues.mentions(e, PK)) {
            return new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
        }
        if (FinanceValues.mentions(e, ACTIVE_UNIQUE)) {
            return FinanceValues.conflict(ErrorCode.FINANCE_BUDGET_EXISTS);
        }
        return e;
    }
}
