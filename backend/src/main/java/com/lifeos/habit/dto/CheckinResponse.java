package com.lifeos.habit.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Check-in (D232). Cũng là nguồn {@code data} của snapshot sync habit_checkin. toString không in ghi chú. */
public record CheckinResponse(UUID id, UUID habitId, LocalDate localDate, String status, String note, long version,
        Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "CheckinResponse[id=" + id + ", version=" + version + "]";
    }
}
