package com.lifeos.ai.dto;

import java.time.LocalDate;

public record AiUsageDay(LocalDate date, long requests, long inputTokens, long outputTokens,
        long estimatedCostMicrousd) {
}
