package com.lifeos.goal.dto;

import jakarta.validation.constraints.NotNull;

/** POST /goals/{id}/status (D188). */
public record GoalStatusRequest(@NotNull Long version, @NotNull String status) {
}
