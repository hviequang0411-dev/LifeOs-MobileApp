package com.lifeos.ai.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AiPlanResponse(String periodType, LocalDate periodStart, String summary,
        List<AiPlanItem> items, UUID contextMessageId) { }
