package com.lifeos.ai.service;

import com.lifeos.ai.config.AiProperties;
import com.lifeos.ai.dto.AiLimitsResponse;
import com.lifeos.ai.dto.AiQuotaWindow;
import com.lifeos.ai.dto.AiUsageDay;
import com.lifeos.ai.dto.AiUsageResponse;
import com.lifeos.ai.dto.AiUsageTotals;
import com.lifeos.ai.repository.AiStore;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiUsageService {
    private final AiStore store;
    private final AiProperties properties;
    private final Clock clock;

    public AiUsageService(AiStore store, AiProperties properties, Clock clock) {
        this.store = store;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AiUsageResponse usage(UUID userId, LocalDate from, LocalDate to) {
        if (from.isAfter(to) || from.plusDays(365).isBefore(to)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "from", "INVALID_VALUE",
                    "Khoảng usage phải từ 1 đến 366 ngày.");
        }
        List<AiUsageDay> days = store.usage(userId, from, to).stream()
                .map(v -> new AiUsageDay(v.date(), v.requests(), v.inputTokens(), v.outputTokens(), v.costMicrousd()))
                .toList();
        AiUsageTotals totals = new AiUsageTotals(days.stream().mapToLong(AiUsageDay::requests).sum(),
                days.stream().mapToLong(AiUsageDay::inputTokens).sum(),
                days.stream().mapToLong(AiUsageDay::outputTokens).sum(),
                days.stream().mapToLong(AiUsageDay::estimatedCostMicrousd).sum());
        return new AiUsageResponse(from, to, days, totals);
    }

    @Transactional(readOnly = true)
    public AiLimitsResponse limits(UUID userId) {
        ZonedDateTime now = clock.instant().atZone(ZoneOffset.UTC);
        Instant dayStart = now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plusSeconds(86_400);
        Instant monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant monthEnd = now.plusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        AiStore.Totals day = store.totals(userId, dayStart, dayEnd);
        AiStore.Totals month = store.totals(userId, monthStart, monthEnd);
        return new AiLimitsResponse(properties.isEnabled(), properties.isBudgetAvailable(),
                window(properties.getDailyRequestLimit(), properties.getDailyInputTokenLimit(),
                        properties.getDailyOutputTokenLimit(), day, dayEnd),
                window(properties.getMonthlyRequestLimit(), properties.getMonthlyInputTokenLimit(),
                        properties.getMonthlyOutputTokenLimit(), month, monthEnd),
                properties.getRateLimitPerMinute(), properties.getMaxConcurrentPerUser());
    }

    private static AiQuotaWindow window(long requests, long input, long output, AiStore.Totals used, Instant reset) {
        return new AiQuotaWindow(requests, used.requests(), input, used.inputTokens(), output, used.outputTokens(), reset);
    }
}
