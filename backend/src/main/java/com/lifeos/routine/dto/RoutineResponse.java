package com.lifeos.routine.dto;

import java.time.Instant;
import java.util.UUID;

/** Tài nguyên Routine (PHASE_2 §6, D240). weekdays bitmask (khi WEEKLY); reminderTime "HH:mm". Nguồn data snapshot. */
public record RoutineResponse(UUID id, String name, String kind, Integer weekdays, String reminderTime,
        Instant archivedAt, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "RoutineResponse[id=" + id + ", version=" + version + "]";
    }
}
