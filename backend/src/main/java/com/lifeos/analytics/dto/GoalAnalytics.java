package com.lifeos.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** GET /analytics/goals (FR-INT-08, D200). risk ∈ {AT_RISK, OVERDUE} hoặc null. */
public record GoalAnalytics(Instant computedAt, BigDecimal riskThreshold, List<Item> goals) {

    public record Item(UUID goalId, String title, String status, LocalDate startOn, LocalDate targetOn,
            BigDecimal progress, BigDecimal elapsedRatio, String risk) {
        @Override
        public String toString() {
            return "Item[goalId=" + goalId + "]";
        }
    }
}
