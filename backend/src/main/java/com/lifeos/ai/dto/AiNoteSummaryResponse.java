package com.lifeos.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiNoteSummaryResponse(UUID noteId, String summary, List<String> keyPoints,
        UUID contextMessageId) { }
