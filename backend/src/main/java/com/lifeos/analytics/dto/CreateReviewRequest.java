package com.lifeos.analytics.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /reviews. periodKey = ngày bắt đầu kỳ (D192). */
public record CreateReviewRequest(@NotNull UUID id, @NotNull String periodType, @NotNull String periodKey,
        String reflection, Integer mood) {

    @Override
    public String toString() {
        return "CreateReviewRequest[id=" + id + "]";
    }
}
