package com.lifeos.note.dto;

import java.time.Instant;
import java.util.UUID;

/** Liên kết ghi chú → TASK/GOAL (D189). Nguồn data snapshot note_link. */
public record NoteLinkResponse(UUID id, UUID noteId, String targetType, UUID targetId, long version,
        Instant createdAt, Instant updatedAt) {
}
