package com.lifeos.finance.service;

import com.lifeos.common.time.UserCalendar;
import com.lifeos.finance.domain.Money;
import com.lifeos.finance.dto.CashFlowResponse;
import com.lifeos.finance.dto.MonthlyReportResponse;
import com.lifeos.finance.repository.FinanceJdbc;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo cáo tháng và dòng tiền (FR-FIN-10, UC-FIN-08). Luôn theo MỘT tiền tệ (D103, không quy đổi); chỉ giao dịch thu/chi
 * chưa xóa; loại trừ chuyển khoản (FIN-04) và danh mục ADJUSTMENT (D119). Chuỗi thời gian đủ mọi kỳ kể cả kỳ trống
 * (FIN-16). Tiền là chuỗi.
 */
@Service
public class FinanceReportService {

    /** PHASE_3 §6: tối đa 366 ngày (granularity day) hoặc 24 tháng (week/month) mỗi lần. */
    static final int MAX_DAYS = 366;
    static final int MAX_MONTHS = 24;
    private static final Set<String> GRANULARITIES = Set.of("day", "week", "month");

    private final FinanceJdbc jdbc;
    private final UserCalendar calendar;

    public FinanceReportService(FinanceJdbc jdbc, UserCalendar calendar) {
        this.jdbc = jdbc;
        this.calendar = calendar;
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse monthly(UUID userId, String rawMonth, String rawCurrency) {
        YearMonth month = FinanceValues.month(rawMonth, "month");
        if (month == null) {
            month = YearMonth.from(calendar.today(userId));
        }
        String currency = FinanceValues.currency(rawCurrency, "currency");
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        List<FinanceJdbc.ReportRow> rows = jdbc.reportRows(userId, currency, from, to);

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        Map<UUID, BigDecimal[]> byAccount = new LinkedHashMap<>();
        Map<LocalDate, BigDecimal[]> daily = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            daily.put(d, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (FinanceJdbc.ReportRow row : rows) {
            boolean isIncome = FinanceValues.INCOME.equals(row.kind());
            if (isIncome) {
                income = income.add(row.total());
            } else {
                expense = expense.add(row.total());
            }
            byCategory.merge(row.kind() + "|" + row.categoryId(), row.total(), BigDecimal::add);
            BigDecimal[] acc = byAccount.computeIfAbsent(row.accountId(),
                    k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
            acc[isIncome ? 0 : 1] = acc[isIncome ? 0 : 1].add(row.total());
            BigDecimal[] day = daily.get(row.date());
            day[isIncome ? 0 : 1] = day[isIncome ? 0 : 1].add(row.total());
        }
        List<MonthlyReportResponse.CategoryTotal> categories = new ArrayList<>();
        byCategory.forEach((key, total) -> {
            String[] parts = key.split("\\|", 2);
            UUID categoryId = "null".equals(parts[1]) ? null : UUID.fromString(parts[1]);
            categories.add(new MonthlyReportResponse.CategoryTotal(categoryId, parts[0], Money.format(total)));
        });
        categories.sort(Comparator.comparing(MonthlyReportResponse.CategoryTotal::kind)
                .thenComparing(c -> new BigDecimal(c.total()), Comparator.reverseOrder())
                .thenComparing(c -> Objects.toString(c.categoryId(), "")));
        List<MonthlyReportResponse.AccountTotal> accounts = new ArrayList<>();
        byAccount.forEach((accountId, totals) -> accounts.add(new MonthlyReportResponse.AccountTotal(accountId,
                Money.format(totals[0]), Money.format(totals[1]))));
        accounts.sort(Comparator.comparing(a -> a.accountId().toString()));
        List<MonthlyReportResponse.DayTotal> days = new ArrayList<>();
        daily.forEach((date, totals) -> days.add(new MonthlyReportResponse.DayTotal(date, Money.format(totals[0]),
                Money.format(totals[1]))));
        return new MonthlyReportResponse(month.toString(), currency, Money.format(income), Money.format(expense),
                Money.format(income.subtract(expense)), categories, accounts, days);
    }

    /** GET /reports/cash-flow?from=&to=&granularity=month|week|day&currency= (tuần bắt đầu theo weekStartDay). */
    @Transactional(readOnly = true)
    public CashFlowResponse cashFlow(UUID userId, String rawFrom, String rawTo, String rawGranularity,
            String rawCurrency) {
        LocalDate from = FinanceValues.requiredDate(rawFrom, "from");
        LocalDate to = FinanceValues.requiredDate(rawTo, "to");
        String granularity = rawGranularity == null ? "month"
                : FinanceValues.oneOf(rawGranularity, GRANULARITIES, "granularity");
        String currency = FinanceValues.currency(rawCurrency, "currency");
        if (to.isBefore(from)) {
            throw FinanceValues.invalid("to", "INVALID_VALUE", "to không được trước from.");
        }
        boolean tooLong = "day".equals(granularity)
                ? ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS
                : !to.isBefore(from.plusMonths(MAX_MONTHS));
        if (tooLong) {
            throw FinanceValues.invalid("to", "OUT_OF_RANGE", "Khoảng thời gian vượt giới hạn cho phép.");
        }
        DayOfWeek weekStart = calendar.weekStart(userId);
        Map<LocalDate, BigDecimal[]> buckets = new TreeMap<>();
        for (LocalDate d = bucketOf(from, granularity, weekStart); !d.isAfter(to); d = nextBucket(d, granularity)) {
            buckets.put(d, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
        }
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (FinanceJdbc.ReportRow row : jdbc.reportRows(userId, currency, from, to)) {
            boolean isIncome = FinanceValues.INCOME.equals(row.kind());
            BigDecimal[] bucket = buckets.get(bucketOf(row.date(), granularity, weekStart));
            bucket[isIncome ? 0 : 1] = bucket[isIncome ? 0 : 1].add(row.total());
            if (isIncome) {
                income = income.add(row.total());
            } else {
                expense = expense.add(row.total());
            }
        }
        List<CashFlowResponse.Bucket> out = new ArrayList<>();
        buckets.forEach((start, t) -> out.add(new CashFlowResponse.Bucket(start, Money.format(t[0]),
                Money.format(t[1]), Money.format(t[0].subtract(t[1])))));
        return new CashFlowResponse(currency, granularity, from, to, Money.format(income), Money.format(expense),
                Money.format(income.subtract(expense)), out);
    }

    private static LocalDate bucketOf(LocalDate date, String granularity, DayOfWeek weekStart) {
        return switch (granularity) {
            case "day" -> date;
            case "week" -> date.with(TemporalAdjusters.previousOrSame(weekStart));
            default -> date.withDayOfMonth(1);
        };
    }

    private static LocalDate nextBucket(LocalDate start, String granularity) {
        return switch (granularity) {
            case "day" -> start.plusDays(1);
            case "week" -> start.plusWeeks(1);
            default -> start.plusMonths(1);
        };
    }
}
