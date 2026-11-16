package com.lifeos.ai.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;

public record AiInsightRequest(@NotNull LocalDate from, @NotNull LocalDate to,
        @NotNull Set<AiContextDomain> contextDomains) {
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown AI insight field: " + field);
    }
}
