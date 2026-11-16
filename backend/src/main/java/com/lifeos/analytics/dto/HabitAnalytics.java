package com.lifeos.analytics.dto;

import com.lifeos.habit.api.HabitStatsApi;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** GET /analytics/habits (FR-INT-05, D201). */
public record HabitAnalytics(LocalDate from, LocalDate to, Instant computedAt, int scheduled, int done,
        BigDecimal completionRate, List<HabitStatsApi.HabitPeriod> habits) {
}
