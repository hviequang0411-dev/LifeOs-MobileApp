package com.lifeos.finance.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.finance.dto.FinanceCategoryResponse;
import com.lifeos.finance.entity.FinancialCategory;
import com.lifeos.finance.mapper.FinanceMapper;
import com.lifeos.finance.repository.BudgetRepository;
import com.lifeos.finance.repository.FinancialCategoryRepository;
import com.lifeos.finance.repository.FinancialTransactionRepository;
import com.lifeos.finance.repository.RecurringTransactionRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi danh mục thu/chi (FR-FIN-03, D106). kind bất biến sau khi tạo. Danh mục đang được giao dịch, ngân sách hoặc
 * định kỳ (chưa xóa) tham chiếu chỉ được lưu trữ: xóa → 409 FINANCE_CATEGORY_IN_USE (UC-FIN-07). Lệnh ghi tham chiếu
 * giữ FOR SHARE trên danh mục, xóa khóa FOR UPDATE rồi mới kiểm, nên không đua.
 */
@Service
public class FinanceCategoryCommandService {

    static final int NAME_MAX = 50;
    private static final String PK = "pk_financial_categories";
    private static final String NAME_UNIQUE = "ux_financial_categories_user_kind_name";

    private final FinancialCategoryRepository categories;
    private final FinancialTransactionRepository transactions;
    private final BudgetRepository budgets;
    private final RecurringTransactionRepository recurring;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final FinanceMapper mapper;
    private final Clock clock;

    public FinanceCategoryCommandService(FinancialCategoryRepository categories, FinancialTransactionRepository transactions,
            BudgetRepository budgets, RecurringTransactionRepository recurring, ChangeRecorder changeRecorder,
            VersionedWrites versionedWrites, FinanceMapper mapper, Clock clock) {
        this.categories = categories;
        this.transactions = transactions;
        this.budgets = budgets;
        this.recurring = recurring;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
    }

    public FinanceCategoryResponse create(UUID userId, FinanceCommands.CreateCategory cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (categories.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String name = FinanceValues.name(cmd.name().value(), NAME_MAX);
                String kind = FinanceValues.oneOf(cmd.kind().value(), FinanceValues.KINDS, "kind");
                if (categories.nameTaken(userId, kind, name, cmd.id())) {
                    throw FinanceValues.conflict(ErrorCode.FINANCE_NAME_TAKEN);
                }
                FinancialCategory category = FinancialCategory.create(cmd.id(), userId, name, kind, null);
                FinancialCategory saved = categories.saveAndFlush(category);
                changeRecorder.record(userId, FinancialCategory.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    public FinanceCategoryResponse update(UUID userId, UUID categoryId, FinanceCommands.UpdateCategory cmd,
            UUID mutationIdOrNull) {
        try {
            return writeExisting(userId, categoryId, cmd.expectedVersion(), mutationIdOrNull, category -> {
                FinanceValues.immutable(cmd.kind(), category.getKind(), "kind");
                boolean changed = false;
                if (cmd.name().present()) {
                    String name = FinanceValues.name(cmd.name().value(), NAME_MAX);
                    if (!name.equals(category.getName())
                            && categories.nameTaken(userId, category.getKind(), name, categoryId)) {
                        throw FinanceValues.conflict(ErrorCode.FINANCE_NAME_TAKEN);
                    }
                    changed |= category.setName(name);
                }
                if (cmd.archived().isExplicitNull()) {
                    throw FinanceValues.invalid("archived", "REQUIRED", "archived phải là true hoặc false.");
                }
                if (cmd.archived().present()) {
                    changed |= cmd.archived().value() ? category.archive(clock.instant()) : category.unarchive();
                }
                return changed;
            });
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    public FinanceCategoryResponse archive(UUID userId, UUID categoryId, long expectedVersion) {
        return writeExisting(userId, categoryId, expectedVersion, null, c -> c.archive(clock.instant()));
    }

    public FinanceCategoryResponse unarchive(UUID userId, UUID categoryId, long expectedVersion) {
        return writeExisting(userId, categoryId, expectedVersion, null, FinancialCategory::unarchive);
    }

    /** @return version tombstone */
    public long delete(UUID userId, UUID categoryId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            FinancialCategory category = categories.findActiveForUpdate(categoryId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_CATEGORY_NOT_FOUND));
            VersionedWrites.requireVersion(category, expectedVersion);
            if (transactions.existsLiveForCategory(userId, categoryId)
                    || budgets.existsLiveForCategory(userId, categoryId)
                    || recurring.existsLiveForCategory(userId, categoryId)) {
                throw FinanceValues.conflict(ErrorCode.FINANCE_CATEGORY_IN_USE);
            }
            category.softDelete(clock.instant());
            FinancialCategory saved = categories.saveAndFlush(category);
            changeRecorder.record(userId, FinancialCategory.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, categoryId));
    }

    private FinanceCategoryResponse writeExisting(UUID userId, UUID categoryId, long expectedVersion, UUID mutationIdOrNull,
            Function<FinancialCategory, Boolean> change) {
        return versionedWrites.write(() -> {
            FinancialCategory category = categories.findActive(categoryId, userId)
                    .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_CATEGORY_NOT_FOUND));
            VersionedWrites.requireVersion(category, expectedVersion);
            if (!change.apply(category)) {
                return mapper.toResponse(category);
            }
            FinancialCategory saved = categories.saveAndFlush(category);
            changeRecorder.record(userId, FinancialCategory.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, categoryId));
    }

    private long currentVersion(UUID userId, UUID categoryId) {
        return categories.findVersion(categoryId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_CATEGORY_NOT_FOUND));
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
