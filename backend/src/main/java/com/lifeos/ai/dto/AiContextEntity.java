package com.lifeos.ai.dto;

import java.util.Map;
import java.util.UUID;

public record AiContextEntity(AiContextDomain domain, String entityType, UUID entityId, Long entityVersion,
        Map<String, Object> fields) { }
