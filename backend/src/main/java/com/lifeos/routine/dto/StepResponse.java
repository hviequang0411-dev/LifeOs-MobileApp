package com.lifeos.routine.dto;

import java.time.Instant;
import java.util.UUID;

/** Bước routine (D241). Nguồn data snapshot routine_step. toString không in tiêu đề. */
public record StepResponse(UUID id, UUID routineId, String title, long position, Integer durationMinutes,
        long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "StepResponse[id=" + id + ", version=" + version + "]";
    }
}
