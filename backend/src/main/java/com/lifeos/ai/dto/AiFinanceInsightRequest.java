package com.lifeos.ai.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Set;

public record AiFinanceInsightRequest(@NotNull LocalDate from, @NotNull LocalDate to,
        @NotNull @Pattern(regexp = "^[A-Z]{3}$") String currency,
        @NotNull Set<AiContextDomain> contextDomains) {
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown AI finance insight field: " + field);
    }
}
