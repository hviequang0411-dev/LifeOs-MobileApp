package com.lifeos.routine.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Lượt chạy (D242): completedAt do server. Nguồn data snapshot routine_log. */
public record LogResponse(UUID id, UUID routineId, LocalDate localDate, int totalSteps, Instant startedAt,
        Instant completedAt, long version, Instant createdAt, Instant updatedAt) {
}
