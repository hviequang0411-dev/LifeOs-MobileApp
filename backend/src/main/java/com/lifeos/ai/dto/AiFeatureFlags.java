package com.lifeos.ai.dto;

public record AiFeatureFlags(boolean chat, boolean insights, boolean notes, boolean finance, boolean rag,
        boolean actions) {
}
