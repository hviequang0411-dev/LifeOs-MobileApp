package com.lifeos.calendar.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /calendar/events/{id}/exceptions ("chỉ lần này", D127). */
public record CreateExceptionRequest(@NotNull UUID id, @NotNull String originalStartLocal, @NotNull String kind,
        String title, String description, String location, String startLocal, String endLocal) {

    @Override
    public String toString() {
        return "CreateExceptionRequest[id=" + id + "]";
    }
}
