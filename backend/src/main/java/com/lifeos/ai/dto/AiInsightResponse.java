package com.lifeos.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiInsightResponse(String summary, List<AiObservation> observations, UUID contextMessageId) { }
