package com.lifeos.ai.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.Size;

public record AiConversationCreateRequest(@Size(max = 120) String title) {
    @JsonAnySetter public void rejectUnknown(String field, Object value) {
        throw new IllegalArgumentException("Unknown AI conversation field: " + field);
    }
}
