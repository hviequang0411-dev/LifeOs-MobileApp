package com.lifeos.ai.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AiTaskBreakdownRequest(@Size(max = 1000) String instruction,
        @NotNull @Size(max = 3) Set<AiContextDomain> contextDomains) {
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown AI task breakdown field: " + field);
    }
}
