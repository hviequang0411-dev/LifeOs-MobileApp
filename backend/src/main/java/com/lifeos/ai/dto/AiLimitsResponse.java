package com.lifeos.ai.dto;

public record AiLimitsResponse(boolean enabled, boolean budgetAvailable, AiQuotaWindow daily,
        AiQuotaWindow monthly, int rateLimitPerMinute, int maxConcurrentPerUser) {
}
