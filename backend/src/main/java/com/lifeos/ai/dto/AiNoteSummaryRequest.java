package com.lifeos.ai.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AiNoteSummaryRequest(@NotNull Style style, @NotNull Set<AiContextDomain> contextDomains) {
    public enum Style { BRIEF, BULLETS, DETAILED }
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown AI note summary field: " + field);
    }
}
