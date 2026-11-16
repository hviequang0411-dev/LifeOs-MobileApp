package com.lifeos.routine.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /routines/{id}/steps. position vắng = cuối danh sách (max + 1000, D241). */
public record CreateStepRequest(@NotNull UUID id, @NotNull String title, Long position, Integer durationMinutes) {

    @Override
    public String toString() {
        return "CreateStepRequest[id=" + id + "]";
    }
}
