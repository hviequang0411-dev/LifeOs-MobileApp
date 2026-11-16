package com.lifeos.analytics.dto;

import com.lifeos.analytics.domain.TaskMetrics;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** GET /analytics/tasks (FR-INT-04, D198) kèm so sánh kỳ trước cùng độ dài. */
public record TaskAnalytics(LocalDate from, LocalDate to, String granularity, Instant computedAt, long completed,
        long completedWithDue, long completedOnTime, BigDecimal onTimeRate, Long medianCycleSeconds,
        long overdueOpen, List<Bucket> buckets, List<TaskMetrics.Share> byProject,
        List<TaskMetrics.Share> byCategory, Previous previous) {

    public record Bucket(LocalDate periodStart, long completed) {
    }

    public record Previous(LocalDate from, LocalDate to, long completed, BigDecimal onTimeRate) {
    }
}
