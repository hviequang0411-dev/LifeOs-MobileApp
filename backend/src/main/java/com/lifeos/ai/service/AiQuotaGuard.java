package com.lifeos.ai.service;

import com.lifeos.ai.config.AiProperties;
import com.lifeos.ai.repository.AiStore;
import com.lifeos.common.exception.BusinessException;
import com.lifeos.common.exception.RateLimitedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiQuotaGuard {
    private final AiStore store;
    private final AiProperties properties;
    private final Clock clock;

    public AiQuotaGuard(AiStore store, AiProperties properties, Clock clock) {
        this.store = store;
        this.properties = properties;
        this.clock = clock;
    }

    /** Check immediately before a provider call. No counter is held in a DB transaction across network I/O. */
    @Transactional(readOnly = true)
    public void check(UUID userId) {
        if (!properties.isEnabled()) {
            throw AiErrors.error("AI_DISABLED", HttpStatus.SERVICE_UNAVAILABLE, "AI đang tắt.");
        }
        if (!properties.isBudgetAvailable()) {
            throw AiErrors.error("AI_BUDGET_EXCEEDED", HttpStatus.SERVICE_UNAVAILABLE,
                    "Ngân sách AI tháng đã chạm giới hạn.");
        }
        ZonedDateTime now = clock.instant().atZone(ZoneOffset.UTC);
        Instant dayStart = now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plus(Duration.ofDays(1));
        Instant monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant monthEnd = now.plusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        AiStore.Totals minute = store.totals(userId, clock.instant().minusSeconds(60), clock.instant());
        if (minute.requests() >= properties.getRateLimitPerMinute()) {
            throw limited("AI_RATE_LIMITED", 60, null);
        }
        AiStore.Totals day = store.totals(userId, dayStart, dayEnd);
        if (exceeded(day, properties.getDailyRequestLimit(), properties.getDailyInputTokenLimit(),
                properties.getDailyOutputTokenLimit())) {
            throw limited("AI_QUOTA_EXCEEDED", Duration.between(clock.instant(), dayEnd).toSeconds(), dayEnd);
        }
        AiStore.Totals month = store.totals(userId, monthStart, monthEnd);
        if (exceeded(month, properties.getMonthlyRequestLimit(), properties.getMonthlyInputTokenLimit(),
                properties.getMonthlyOutputTokenLimit())) {
            throw limited("AI_QUOTA_EXCEEDED", Duration.between(clock.instant(), monthEnd).toSeconds(), monthEnd);
        }
    }

    private static boolean exceeded(AiStore.Totals used, long requests, long input, long output) {
        return used.requests() >= requests || used.inputTokens() >= input || used.outputTokens() >= output;
    }

    private static BusinessException limited(String code, long seconds, Instant resetAt) {
        BusinessException error = new RateLimitedException(seconds).with("errorCode", code);
        return resetAt == null ? error : error.with("resetsAt", resetAt);
    }
}
