package com.lifeos.calendar.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** Sự kiện lịch (PHASE_4 §6). startLocal/endLocal: giờ địa phương theo timezone của sự kiện; startAt/endAt/recurrenceUntil dẫn xuất UTC. recurrence: JSON D126 hoặc null. Nguồn data của snapshot sync. */
public record EventResponse(UUID id, String title, String description, String location, boolean allDay,
        String timezone, LocalDateTime startLocal, LocalDateTime endLocal, LocalDate startDate, LocalDate endDate,
        Instant startAt, Instant endAt, Map<String, Object> recurrence, Instant recurrenceUntil, long version,
        Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "EventResponse[id=" + id + ", version=" + version + "]";
    }
}
