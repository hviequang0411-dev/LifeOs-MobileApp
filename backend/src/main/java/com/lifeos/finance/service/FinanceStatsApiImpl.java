package com.lifeos.finance.service;

import com.lifeos.finance.api.FinanceStatsApi;
import com.lifeos.finance.domain.Money;
import com.lifeos.finance.dto.BudgetUsageResponse;
import com.lifeos.finance.dto.CashFlowResponse;
import com.lifeos.finance.repository.FinanceJdbc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt {@link FinanceStatsApi} dùng lại FinanceReportService/FinanceQueryService và FinanceJdbc (D102, D119). */
@Service
class FinanceStatsApiImpl implements FinanceStatsApi {

    private final FinanceJdbc jdbc;
    private final FinanceReportService reports;
    private final FinanceQueryService queries;

    FinanceStatsApiImpl(FinanceJdbc jdbc, FinanceReportService reports, FinanceQueryService queries) {
        this.jdbc = jdbc;
        this.reports = reports;
        this.queries = queries;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Totals> totals(UUID userId, LocalDate from, LocalDate to) {
        Map<String, BigDecimal[]> byCurrency = new LinkedHashMap<>();
        for (FinanceJdbc.CurrencyKindTotal row : jdbc.currencyTotals(userId, from, to)) {
            BigDecimal[] t = byCurrency.computeIfAbsent(row.currency(),
                    k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            int i = FinanceValues.INCOME.equals(row.kind()) ? 0 : 1;
            t[i] = t[i].add(row.total());
        }
        List<Totals> out = new ArrayList<>();
        byCurrency.forEach((currency, t) -> out.add(new Totals(currency, Money.format(t[0]), Money.format(t[1]),
                Money.format(t[0].subtract(t[1])))));
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Flow flow(UUID userId, String currency, LocalDate from, LocalDate to, String granularity) {
        CashFlowResponse cash = reports.cashFlow(userId, from.toString(), to.toString(), granularity, currency);
        Map<UUID, BigDecimal> byCategory = new HashMap<>();
        for (FinanceJdbc.ReportRow row : jdbc.reportRows(userId, cash.currency(), from, to)) {
            if (!FinanceValues.INCOME.equals(row.kind())) {
                byCategory.merge(row.categoryId(), row.total(), BigDecimal::add);
            }
        }
        List<CategoryShare> top = byCategory.entrySet().stream()
                .sorted(Map.Entry.<UUID, BigDecimal>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(e -> Objects.toString(e.getKey(), "")))
                .limit(10).map(e -> new CategoryShare(e.getKey(), Money.format(e.getValue()))).toList();
        return new Flow(cash.currency(), cash.income(), cash.expense(), cash.net(), cash.buckets().stream()
                .map(b -> new Bucket(b.periodStart(), b.income(), b.expense(), b.net())).toList(), top);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetUse> budgets(UUID userId, YearMonth month) {
        List<BudgetUsageResponse> usage = queries.usageAll(userId, month.toString());
        return usage.stream().map(u -> new BudgetUse(u.budgetId(), u.categoryId(), u.currency(), u.spent(), u.limit(),
                u.ratio())).toList();
    }
}
