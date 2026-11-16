package com.lifeos.goal.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Goal (FR-GOAL-01). Nguồn data snapshot goal. toString không in tiêu đề/mô tả. */
public record GoalResponse(UUID id, String title, String description, LocalDate startOn, LocalDate targetOn,
        String status, Instant statusChangedAt, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "GoalResponse[id=" + id + ", version=" + version + "]";
    }
}
