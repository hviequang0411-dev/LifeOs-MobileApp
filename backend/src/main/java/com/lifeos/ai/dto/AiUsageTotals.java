package com.lifeos.ai.dto;

public record AiUsageTotals(long requests, long inputTokens, long outputTokens, long estimatedCostMicrousd) {
}
