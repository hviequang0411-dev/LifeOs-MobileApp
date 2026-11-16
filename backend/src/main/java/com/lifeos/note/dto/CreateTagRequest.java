package com.lifeos.note.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /tags. */
public record CreateTagRequest(@NotNull UUID id, @NotNull String name, String color) {

    @Override
    public String toString() {
        return "CreateTagRequest[id=" + id + "]";
    }
}
