package com.lifeos.calendar.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /time-blocks. */
public record CreateTimeBlockRequest(@NotNull UUID id, @NotNull UUID taskId, @NotNull String startAt,
        @NotNull String endAt, String note) {

    @Override
    public String toString() {
        return "CreateTimeBlockRequest[id=" + id + "]";
    }
}
