package com.lifeos.ai.dto;

import java.util.UUID;

public record AiSourceReference(AiContextDomain domain, String entityType, UUID entityId) { }
