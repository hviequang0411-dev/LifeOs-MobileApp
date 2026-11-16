package com.lifeos.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record AiConversationResponse(UUID id, String title, Instant createdAt, Instant updatedAt, Instant expiresAt) { }
