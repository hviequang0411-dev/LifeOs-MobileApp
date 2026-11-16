package com.lifeos.goal.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Milestone (FR-GOAL-02, D187). Nguồn data snapshot milestone. toString không in tiêu đề. */
public record MilestoneResponse(UUID id, UUID goalId, String title, LocalDate dueOn, long position,
        Instant completedAt, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "MilestoneResponse[id=" + id + ", version=" + version + "]";
    }
}
