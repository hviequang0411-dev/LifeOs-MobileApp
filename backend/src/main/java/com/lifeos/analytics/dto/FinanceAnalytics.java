package com.lifeos.analytics.dto;

import com.lifeos.finance.api.FinanceStatsApi;
import java.math.BigDecimal;
import java.time.Instant;

/** GET /analytics/finance (FR-INT-06, D199): một tiền tệ. */
public record FinanceAnalytics(Instant computedAt, String granularity, FinanceStatsApi.Flow flow,
        BigDecimal savingsRate) {
}
