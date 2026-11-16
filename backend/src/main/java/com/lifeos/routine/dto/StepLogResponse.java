package com.lifeos.routine.dto;

import java.time.Instant;
import java.util.UUID;

/** Kết quả một bước trong lượt (D243). Nguồn data snapshot routine_step_log. */
public record StepLogResponse(UUID id, UUID routineLogId, UUID stepId, String status, long version,
        Instant createdAt, Instant updatedAt) {
}
