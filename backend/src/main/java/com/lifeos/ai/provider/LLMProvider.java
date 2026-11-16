package com.lifeos.ai.provider;

import java.util.Map;

/** LifeOS-owned provider port. Provider SDK/type must never cross this boundary. */
public interface LLMProvider {
    record Request(String mode, String instruction, Map<String, Object> context, Map<String, Object> responseSchema) {
    }

    record Response(String content, Map<String, Object> structured, long inputTokens, long cachedInputTokens,
            long outputTokens, long reasoningTokens, long latencyMs, String resultCode) {
    }

    Response generate(Request request);
}
