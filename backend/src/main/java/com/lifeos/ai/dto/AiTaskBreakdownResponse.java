package com.lifeos.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiTaskBreakdownResponse(UUID sourceTaskId, List<AiTaskDraft> items, UUID contextMessageId) { }
