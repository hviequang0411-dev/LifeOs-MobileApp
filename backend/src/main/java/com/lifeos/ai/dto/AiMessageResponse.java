package com.lifeos.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record AiMessageResponse(UUID id, AiMessageRole role, String content, AiGenerationMode mode,
        boolean contextAvailable, Instant createdAt) { }
