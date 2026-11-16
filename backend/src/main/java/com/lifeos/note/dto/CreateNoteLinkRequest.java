package com.lifeos.note.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /notes/{id}/links (D189): targetType ∈ {TASK, GOAL}. */
public record CreateNoteLinkRequest(@NotNull UUID id, @NotNull String targetType, @NotNull UUID targetId) {
}
