package com.lifeos.ai.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public record AiPlanRequest(@NotNull LocalDate periodStart,
        @NotNull @Size(max = 3) Set<AiContextDomain> contextDomains) {
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown AI plan field: " + field);
    }
}
