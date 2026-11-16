package com.lifeos.calendar.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Ngoại lệ một lần xuất hiện (D127). */
public record ExceptionResponse(UUID id, UUID eventId, LocalDateTime originalStartLocal, String kind, String title,
        String description, String location, LocalDateTime startLocal, LocalDateTime endLocal, long version,
        Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "ExceptionResponse[id=" + id + ", version=" + version + "]";
    }
}
