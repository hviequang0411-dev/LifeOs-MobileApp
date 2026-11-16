package com.lifeos.analytics.dto;

import com.lifeos.finance.api.FinanceStatsApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * GET /reviews/daily|weekly|monthly (FR-INT-01/02): số liệu tính khi đọc (D190) + phản tư hiện có. overdueOpen là số
 * Task quá hạn tính đến hôm nay. previous chỉ có ở WEEK/MONTH.
 */
public record ReviewSummary(String periodType, String periodKey, LocalDate from, LocalDate to, Instant computedAt,
        Tasks tasks, Habits habits, List<FinanceStatsApi.Totals> finance, Time time, ReviewResponse reflection,
        Comparison previous) {

    public record Tasks(long completed, long completedWithDue, long completedOnTime, BigDecimal onTimeRate,
            long overdueOpen, Long medianCycleSeconds) {
    }

    public record HabitItem(UUID habitId, String name, int scheduled, int done, BigDecimal completionRate) {
        @Override
        public String toString() {
            return "HabitItem[habitId=" + habitId + "]";
        }
    }

    public record Habits(int scheduled, int done, int skipped, int missed, BigDecimal completionRate,
            List<HabitItem> top) {
    }

    public record Time(int events, long eventMinutes, long blockMinutes) {
    }

    public record Comparison(String periodKey, long tasksCompleted, int habitsDone,
            List<FinanceStatsApi.Totals> finance) {
    }
}
