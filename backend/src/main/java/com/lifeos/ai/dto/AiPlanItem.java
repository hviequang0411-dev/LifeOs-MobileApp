package com.lifeos.ai.dto;

import java.time.Instant;
import java.util.List;

public record AiPlanItem(String title, String reason, String priority, Instant suggestedStartAt,
        Instant suggestedEndAt, List<AiSourceReference> sourceRefs) { }
