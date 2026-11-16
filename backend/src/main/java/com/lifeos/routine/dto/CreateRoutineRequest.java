package com.lifeos.routine.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /routines. id do client tạo; giá trị kiểm ở service. */
public record CreateRoutineRequest(@NotNull UUID id, @NotNull String name, @NotNull String kind, Integer weekdays,
        String reminderTime) {

    @Override
    public String toString() {
        return "CreateRoutineRequest[id=" + id + "]";
    }
}
