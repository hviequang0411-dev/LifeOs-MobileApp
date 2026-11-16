package com.lifeos.ai.service;

import org.springframework.stereotype.Component;

/** D262/D283 reference price expressed only as integer micro-USD; no floating point. */
@Component
public class AiCostCalculator {
    static final long INPUT_MICROUSD_PER_TOKEN = 2;
    static final long OUTPUT_MICROUSD_PER_TOKEN = 10;

    public long estimate(long inputTokens, long outputTokens) {
        if (inputTokens < 0 || outputTokens < 0) {
            throw new IllegalArgumentException("Token count must be non-negative");
        }
        return Math.addExact(Math.multiplyExact(inputTokens, INPUT_MICROUSD_PER_TOKEN),
                Math.multiplyExact(outputTokens, OUTPUT_MICROUSD_PER_TOKEN));
    }
}
