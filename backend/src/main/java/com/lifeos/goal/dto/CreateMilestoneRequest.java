package com.lifeos.goal.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /goals/{id}/milestones. position vắng = cuối danh sách (max + 1000, D187). */
public record CreateMilestoneRequest(@NotNull UUID id, @NotNull String title, String dueOn, Long position) {

    @Override
    public String toString() {
        return "CreateMilestoneRequest[id=" + id + "]";
    }
}
