package com.lifeos.calendar.dto;

import java.time.Instant;
import java.util.UUID;

/** Khối thời gian của Task (D128). */
public record TimeBlockResponse(UUID id, UUID taskId, Instant startAt, Instant endAt, String note, long version,
        Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "TimeBlockResponse[id=" + id + ", version=" + version + "]";
    }
}
