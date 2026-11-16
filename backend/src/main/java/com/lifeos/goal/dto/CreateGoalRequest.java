package com.lifeos.goal.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /goals. status do server (ACTIVE); statusChangedAt không nhận từ client. */
public record CreateGoalRequest(@NotNull UUID id, @NotNull String title, String description, String startOn,
        String targetOn) {

    @Override
    public String toString() {
        return "CreateGoalRequest[id=" + id + "]";
    }
}
