package com.lifeos.ai.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AiContextResponse(UUID messageId, Instant capturedAt, Set<AiContextDomain> domains,
        List<AiContextEntity> entities, boolean truncated) { }
