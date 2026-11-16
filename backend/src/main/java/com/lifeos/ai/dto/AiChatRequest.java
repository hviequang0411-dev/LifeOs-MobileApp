package com.lifeos.ai.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AiChatRequest(@NotBlank @Size(max = 8000) String message, @NotNull AiGenerationMode mode,
        @NotNull @Size(max = 5) Set<AiContextDomain> contextDomains) {
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown AI chat field: " + field);
    }
}
