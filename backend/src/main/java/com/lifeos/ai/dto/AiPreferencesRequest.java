package com.lifeos.ai.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;

public record AiPreferencesRequest(@NotNull Boolean taskCalendar, @NotNull Boolean habit, @NotNull Boolean goal,
        @NotNull Boolean notes, @NotNull Boolean finance, @NotNull Boolean providerTermsAccepted) {

    @JsonAnySetter
    public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown AI preference field: " + field);
    }
}
