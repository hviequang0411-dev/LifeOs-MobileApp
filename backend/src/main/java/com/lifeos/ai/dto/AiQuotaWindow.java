package com.lifeos.ai.dto;

import java.time.Instant;

public record AiQuotaWindow(long requestLimit, long requestsUsed, long inputTokenLimit, long inputTokensUsed,
        long outputTokenLimit, long outputTokensUsed, Instant resetsAt) {
}
