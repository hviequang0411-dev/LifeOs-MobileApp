package com.lifeos.finance.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.common.web.Cursors;
import com.lifeos.finance.domain.Money;
import com.lifeos.finance.api.FinanceAiQueryApi;
import com.lifeos.finance.dto.AccountView;
import com.lifeos.finance.dto.BalanceResponse;
import com.lifeos.finance.dto.BudgetResponse;
import com.lifeos.finance.dto.BudgetUsageResponse;
import com.lifeos.finance.dto.FinanceCategoryResponse;
import com.lifeos.finance.dto.ContributionResponse;
import com.lifeos.finance.dto.FinanceSyncState;
import com.lifeos.finance.dto.GoalView;
import com.lifeos.finance.dto.ReconcileResponse;
import com.lifeos.finance.dto.RecurringResponse;
import com.lifeos.finance.dto.TransactionResponse;
import com.lifeos.finance.dto.TransferResponse;
import com.lifeos.finance.entity.AccountTransfer;
import com.lifeos.finance.entity.Budget;
import com.lifeos.finance.entity.FinancialAccount;
import com.lifeos.finance.entity.FinancialCategory;
import com.lifeos.finance.entity.FinancialTransaction;
import com.lifeos.finance.entity.RecurringTransaction;
import com.lifeos.finance.entity.SavingContribution;
import com.lifeos.finance.entity.SavingGoal;
import com.lifeos.finance.mapper.FinanceMapper;
import com.lifeos.finance.repository.AccountTransferRepository;
import com.lifeos.finance.repository.BudgetRepository;
import com.lifeos.finance.repository.FinanceJdbc;
import com.lifeos.finance.repository.FinancialAccountRepository;
import com.lifeos.finance.repository.FinancialCategoryRepository;
import com.lifeos.finance.repository.FinancialTransactionRepository;
import com.lifeos.finance.repository.RecurringTransactionRepository;
import com.lifeos.finance.repository.SavingContributionRepository;
import com.lifeos.finance.repository.SavingGoalRepository;
import com.lifeos.common.persistence.SyncedEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc dữ liệu Finance của user hiện tại (PHASE_3 §6): chỉ bản ghi chưa xóa; số dư, mức dùng ngân sách và tiến độ mục
 * tiêu tính khi đọc (D102, D109, D111). Danh sách dùng cursor (D59, D79).
 */
@Service
public class FinanceQueryService implements FinanceAiQueryApi {

    static final String CREATED_SORT = "createdAt:desc";
    static final String OCCURRED_SORT = "occurredOn:desc";
    private static final Set<String> ARCHIVED_MODES = Set.of("false", "true", "all");
    private static final Set<String> GOAL_STATUSES = Set.of("ACTIVE", "ACHIEVED", "ARCHIVED");

    private final FinancialAccountRepository accounts;
    private final FinancialCategoryRepository categories;
    private final FinancialTransactionRepository transactions;
    private final AccountTransferRepository transfers;
    private final BudgetRepository budgets;
    private final RecurringTransactionRepository recurring;
    private final SavingGoalRepository goals;
    private final SavingContributionRepository contributions;
    private final FinanceJdbc jdbc;
    private final UserCalendar calendar;
    private final FinanceMapper mapper;

    public FinanceQueryService(FinancialAccountRepository accounts, FinancialCategoryRepository categories,
            FinancialTransactionRepository transactions, AccountTransferRepository transfers, BudgetRepository budgets,
            RecurringTransactionRepository recurring, SavingGoalRepository goals,
            SavingContributionRepository contributions, FinanceJdbc jdbc, UserCalendar calendar, FinanceMapper mapper) {
        this.accounts = accounts;
        this.categories = categories;
        this.transactions = transactions;
        this.transfers = transfers;
        this.budgets = budgets;
        this.recurring = recurring;
        this.goals = goals;
        this.contributions = contributions;
        this.jdbc = jdbc;
        this.calendar = calendar;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- tài khoản

    @Transactional(readOnly = true)
    public PageResponse<AccountView> accounts(UUID userId, String archived, Integer limit, String cursor) {
        String mode = archivedMode(archived);
        Page<FinancialAccount> page = createdPage(limit, cursor, (after, size) -> accounts.page(userId, mode,
                after == null, after == null ? null : after.at(), after == null ? null : after.id(), Limit.of(size)));
        return new PageResponse<>(accountViews(userId, page.items()), page.next(), page.hasMore());
    }

    @Transactional(readOnly = true)
    public AccountView account(UUID userId, UUID accountId) {
        return accountViews(userId, List.of(findAccount(userId, accountId))).get(0);
    }

    @Transactional(readOnly = true)
    public BalanceResponse balance(UUID userId, UUID accountId, String rawAsOf) {
        FinancialAccount account = findAccount(userId, accountId);
        LocalDate asOf = rawAsOf == null ? calendar.today(userId) : FinanceValues.date(rawAsOf, "asOf");
        BigDecimal balance = jdbc.balances(userId, List.of(accountId), asOf).get(accountId);
        return new BalanceResponse(accountId, account.getCurrency(), asOf, Money.format(balance));
    }

    /** D112: chỉ đọc; trả số dư tính được và chênh lệch so với số dư sao kê. */
    @Transactional(readOnly = true)
    public ReconcileResponse reconcile(UUID userId, UUID accountId, String rawStatement, String rawAsOf) {
        FinancialAccount account = findAccount(userId, accountId);
        BigDecimal statement = FinanceValues.signedAmount(rawStatement, account.getCurrency(), "statementBalance");
        LocalDate asOf = FinanceValues.requiredDate(rawAsOf, "asOfDate");
        BigDecimal computed = jdbc.balances(userId, List.of(accountId), asOf).get(accountId);
        return new ReconcileResponse(accountId, account.getCurrency(), asOf, Money.format(statement),
                Money.format(computed), Money.format(statement.subtract(computed)));
    }

    private List<AccountView> accountViews(UUID userId, List<FinancialAccount> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, BigDecimal> balances = jdbc.balances(userId, list.stream().map(FinancialAccount::getId).toList(),
                calendar.today(userId));
        return list.stream()
                .map(a -> AccountView.of(mapper.toResponse(a), Money.format(balances.get(a.getId()))))
                .toList();
    }

    private FinancialAccount findAccount(UUID userId, UUID accountId) {
        return accounts.findActive(accountId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_ACCOUNT_NOT_FOUND));
    }

    // ---------------------------------------------------------------- danh mục

    @Transactional(readOnly = true)
    public PageResponse<FinanceCategoryResponse> categories(UUID userId, String kind, String archived, Integer limit,
            String cursor) {
        String mode = archivedMode(archived);
        if (kind != null) {
            FinanceValues.oneOf(kind, FinanceValues.KINDS, "kind");
        }
        Page<FinancialCategory> page = createdPage(limit, cursor, (after, size) -> categories.page(userId, mode, kind,
                after == null, after == null ? null : after.at(), after == null ? null : after.id(), Limit.of(size)));
        return page.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public FinanceCategoryResponse category(UUID userId, UUID categoryId) {
        return mapper.toResponse(categories.findActive(categoryId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_CATEGORY_NOT_FOUND)));
    }

    // ---------------------------------------------------------------- giao dịch

    /**
     * FR-FIN-11: lọc kind, accountId, categoryId, khoảng ngày, khoảng số tiền, q (≤ 100 ký tự, tìm trong note bằng
     * ILIKE có escape). Sắp occurredOn giảm dần rồi id giảm dần; keyset nên phân trang không sót/lặp.
     */
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> transactions(UUID userId, String kind, UUID accountId, UUID categoryId,
            String from, String to, String minAmount, String maxAmount, String q, Integer limit, String cursor) {
        if (kind != null) {
            FinanceValues.oneOf(kind, FinanceValues.KINDS, "kind");
        }
        String query = FinanceValues.clean(q);
        if (query != null && query.codePointCount(0, query.length()) > FinanceValues.QUERY_MAX) {
            throw FinanceValues.invalid("q", "SIZE", "q tối đa " + FinanceValues.QUERY_MAX + " ký tự.");
        }
        FinanceJdbc.SearchFilter filter = new FinanceJdbc.SearchFilter(kind, accountId, categoryId,
                FinanceValues.date(from, "from"), FinanceValues.date(to, "to"),
                FinanceValues.filterAmount(minAmount, "minAmount"), FinanceValues.filterAmount(maxAmount, "maxAmount"),
                query == null || query.isEmpty() ? null : FinanceJdbc.likeContains(query));
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, OCCURRED_SORT);
        LocalDate afterDate = after == null ? null : parseDate(after.key());
        List<UUID> ids = jdbc.searchTransactionIds(userId, filter, afterDate, after == null ? null : after.id(),
                size + 1);
        boolean hasMore = ids.size() > size;
        List<UUID> pageIds = hasMore ? ids.subList(0, size) : ids;
        Map<UUID, FinancialTransaction> byId = transactions.findAllOwned(userId, pageIds).stream()
                .collect(java.util.stream.Collectors.toMap(FinancialTransaction::getId, Function.identity()));
        // Bản ghi bị xóa giữa hai câu truy vấn (hiếm) bị bỏ qua thay vì trả null.
        List<FinancialTransaction> rows = pageIds.stream().map(byId::get).filter(java.util.Objects::nonNull)
                .toList();
        String next = null;
        if (hasMore && !rows.isEmpty()) {
            FinancialTransaction last = rows.get(rows.size() - 1);
            next = Cursors.encode(new Cursors.Position(OCCURRED_SORT, last.getOccurredOn().toString(), last.getId()));
        }
        return new PageResponse<>(rows.stream().map(mapper::toResponse).toList(), next, hasMore);
    }

    @Transactional(readOnly = true)
    public TransactionResponse transaction(UUID userId, UUID transactionId) {
        return mapper.toResponse(transactions.findActive(transactionId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_TRANSACTION_NOT_FOUND)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinanceAiQueryApi.Snapshot> aiTransactions(UUID userId, LocalDate from, LocalDate to,
            String currency, int limit) {
        return transactions(userId, null, null, null, from.toString(), to.toString(), null, null,
                null, Math.min(limit, 200), null).items().stream()
                .filter(t -> currency == null || currency.equals(t.currency()))
                .map(t -> new FinanceAiQueryApi.Snapshot(t.id(), t.kind(), t.amount(), t.currency(),
                        t.occurredOn(), t.version()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TransferResponse> transfers(UUID userId, UUID accountId, String rawFrom, String rawTo,
            Integer limit, String cursor) {
        LocalDate from = rawFrom == null ? FinanceValues.MIN_DATE : FinanceValues.date(rawFrom, "from");
        LocalDate to = rawTo == null ? LocalDate.of(9999, 12, 31) : FinanceValues.date(rawTo, "to");
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, OCCURRED_SORT);
        LocalDate afterDate = after == null ? null : parseDate(after.key());
        List<AccountTransfer> rows = transfers.page(userId, accountId, from, to, after == null, afterDate,
                after == null ? null : after.id(), Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<AccountTransfer> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            AccountTransfer last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(OCCURRED_SORT, last.getOccurredOn().toString(), last.getId()));
        }
        return new PageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }

    @Transactional(readOnly = true)
    public TransferResponse transfer(UUID userId, UUID transferId) {
        return mapper.toResponse(transfers.findActive(transferId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_TRANSFER_NOT_FOUND)));
    }

    // ---------------------------------------------------------------- ngân sách

    @Transactional(readOnly = true)
    public PageResponse<BudgetResponse> budgets(UUID userId, String archived, Integer limit, String cursor) {
        String mode = archivedMode(archived);
        Page<Budget> page = createdPage(limit, cursor, (after, size) -> budgets.page(userId, mode, after == null,
                after == null ? null : after.at(), after == null ? null : after.id(), Limit.of(size)));
        return page.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public BudgetResponse budget(UUID userId, UUID budgetId) {
        return mapper.toResponse(findBudget(userId, budgetId));
    }

    /** GET /budgets/{id}/usage?month=YYYY-MM (mặc định tháng hiện tại theo múi giờ người dùng, D109). */
    @Transactional(readOnly = true)
    public BudgetUsageResponse usage(UUID userId, UUID budgetId, String rawMonth) {
        return usageOf(userId, findBudget(userId, budgetId), month(userId, rawMonth));
    }

    /** GET /budgets/usage?month=: mọi ngân sách đang hoạt động (chưa lưu trữ). */
    @Transactional(readOnly = true)
    public List<BudgetUsageResponse> usageAll(UUID userId, String rawMonth) {
        YearMonth month = month(userId, rawMonth);
        return budgets.findAllActive(userId).stream().map(b -> usageOf(userId, b, month)).toList();
    }

    private BudgetUsageResponse usageOf(UUID userId, Budget budget, YearMonth month) {
        BigDecimal spent = jdbc.spent(userId, budget.getCurrency(), budget.getCategoryId(), month.atDay(1),
                month.plusMonths(1).atDay(1));
        BigDecimal limit = budget.getAmount();
        return new BudgetUsageResponse(budget.getId(), budget.getCategoryId(), budget.getCurrency(), month.toString(),
                Money.format(spent), Money.format(limit), ratio(spent, limit), Money.format(limit.subtract(spent)));
    }

    private Budget findBudget(UUID userId, UUID budgetId) {
        return budgets.findActive(budgetId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_BUDGET_NOT_FOUND));
    }

    private YearMonth month(UUID userId, String raw) {
        YearMonth month = FinanceValues.month(raw, "month");
        return month != null ? month : YearMonth.from(calendar.today(userId));
    }

    // ---------------------------------------------------------------- định kỳ

    @Transactional(readOnly = true)
    public PageResponse<RecurringResponse> recurring(UUID userId, Integer limit, String cursor) {
        Page<RecurringTransaction> page = createdPage(limit, cursor, (after, size) -> recurring.page(userId,
                after == null, after == null ? null : after.at(), after == null ? null : after.id(), Limit.of(size)));
        return page.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RecurringResponse recurringOne(UUID userId, UUID recurringId) {
        return mapper.toResponse(recurring.findActive(recurringId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_RECURRING_NOT_FOUND)));
    }

    // ---------------------------------------------------------------- mục tiêu tiết kiệm

    @Transactional(readOnly = true)
    public PageResponse<GoalView> goals(UUID userId, String status, Integer limit, String cursor) {
        if (status != null && !GOAL_STATUSES.contains(status)) {
            throw FinanceValues.invalid("status", "INVALID_VALUE", "status là ACTIVE, ACHIEVED hoặc ARCHIVED.");
        }
        Page<SavingGoal> page = createdPage(limit, cursor, (after, size) -> goals.page(userId, status, after == null,
                after == null ? null : after.at(), after == null ? null : after.id(), Limit.of(size)));
        return new PageResponse<>(goalViews(userId, page.items()), page.next(), page.hasMore());
    }

    @Transactional(readOnly = true)
    public GoalView goal(UUID userId, UUID goalId) {
        return goalViews(userId, List.of(findGoal(userId, goalId))).get(0);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContributionResponse> contributions(UUID userId, UUID goalId, Integer limit, String cursor) {
        findGoal(userId, goalId);
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, OCCURRED_SORT);
        LocalDate afterDate = after == null ? null : parseDate(after.key());
        List<SavingContribution> rows = contributions.page(userId, goalId, after == null, afterDate,
                after == null ? null : after.id(), Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<SavingContribution> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            SavingContribution last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(OCCURRED_SORT, last.getOccurredOn().toString(), last.getId()));
        }
        return new PageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }

    private List<GoalView> goalViews(UUID userId, List<SavingGoal> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<UUID, BigDecimal> sums = jdbc.contributionSums(userId, list.stream().map(SavingGoal::getId).toList());
        return list.stream().map(g -> {
            BigDecimal saved = sums.getOrDefault(g.getId(), BigDecimal.ZERO);
            BigDecimal remaining = g.getTargetAmount().subtract(saved).max(BigDecimal.ZERO);
            return GoalView.of(mapper.toResponse(g), Money.format(saved), ratio(saved, g.getTargetAmount()),
                    Money.format(remaining));
        }).toList();
    }

    private SavingGoal findGoal(UUID userId, UUID goalId) {
        return goals.findActive(goalId, userId)
                .orElseThrow(() -> FinanceValues.notFound(ErrorCode.FINANCE_GOAL_NOT_FOUND));
    }

    // ---------------------------------------------------------------- snapshot sync (kể cả tombstone)

    @Transactional(readOnly = true)
    public List<FinanceSyncState> syncStates(String entityType, UUID userId, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return switch (entityType) {
            case FinancialAccount.ENTITY_TYPE -> states(accounts.findAllOwned(userId, ids), mapper::toResponse);
            case FinancialCategory.ENTITY_TYPE -> states(categories.findAllOwned(userId, ids), mapper::toResponse);
            case FinancialTransaction.ENTITY_TYPE -> states(transactions.findAllOwned(userId, ids),
                    mapper::toResponse);
            case AccountTransfer.ENTITY_TYPE -> states(transfers.findAllOwned(userId, ids), mapper::toResponse);
            case Budget.ENTITY_TYPE -> states(budgets.findAllOwned(userId, ids), mapper::toResponse);
            case RecurringTransaction.ENTITY_TYPE -> states(recurring.findAllOwned(userId, ids), mapper::toResponse);
            case SavingGoal.ENTITY_TYPE -> states(goals.findAllOwned(userId, ids), mapper::toResponse);
            case SavingContribution.ENTITY_TYPE -> states(contributions.findAllOwned(userId, ids),
                    mapper::toResponse);
            default -> throw new IllegalArgumentException("entityType");
        };
    }

    private static <E extends SyncedEntity> List<FinanceSyncState> states(List<E> rows, Function<E, ?> toResponse) {
        return rows.stream()
                .map(e -> new FinanceSyncState(toResponse.apply(e), e.getId(), e.getVersion(), e.isDeleted()))
                .toList();
    }

    // ---------------------------------------------------------------- tiện ích

    /** ratio = a / b, 4 chữ số thập phân, làm tròn HALF_UP (chỉ là tỷ lệ hiển thị, không phải số tiền) [PROPOSED]. */
    static BigDecimal ratio(BigDecimal a, BigDecimal b) {
        return a.divide(b, 4, RoundingMode.HALF_UP);
    }

    private static String archivedMode(String archived) {
        String mode = archived == null ? "false" : archived;
        if (!ARCHIVED_MODES.contains(mode)) {
            throw FinanceValues.invalid("archived", "INVALID_VALUE", "archived phải là false, true hoặc all.");
        }
        return mode;
    }

    private static LocalDate parseDate(String key) {
        try {
            return LocalDate.parse(key);
        } catch (DateTimeParseException | NullPointerException e) {
            throw badCursor();
        }
    }

    private static RuntimeException badCursor() {
        return FinanceValues.invalid("cursor", "INVALID_VALUE", "Con trỏ phân trang không hợp lệ.");
    }

    record After(Instant at, UUID id) {
    }

    interface PageQuery<E> {
        List<E> fetch(After after, int size);
    }

    record Page<E>(List<E> items, String next, boolean hasMore) {
        <R> PageResponse<R> map(Function<E, R> f) {
            return new PageResponse<>(items.stream().map(f).toList(), next, hasMore);
        }
    }

    /** Keyset createdAt giảm dần, id giảm dần cho mọi danh sách entity. */
    private static <E extends SyncedEntity> Page<E> createdPage(Integer limit, String cursor, PageQuery<E> query) {
        int size = Cursors.limit(limit);
        Cursors.Position position = Cursors.decode(cursor, CREATED_SORT);
        After after = null;
        if (position != null) {
            try {
                after = new After(Instant.parse(position.key()), position.id());
            } catch (DateTimeParseException | NullPointerException e) {
                throw badCursor();
            }
        }
        List<E> rows = query.fetch(after, size + 1);
        boolean hasMore = rows.size() > size;
        List<E> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            E last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(CREATED_SORT, last.getCreatedAt().toString(), last.getId()));
        }
        return new Page<>(page, next, hasMore);
    }
}
