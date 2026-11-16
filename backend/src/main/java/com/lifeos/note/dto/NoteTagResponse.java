package com.lifeos.note.dto;

import java.time.Instant;
import java.util.UUID;

/** Liên kết ghi chú ↔ thẻ (D184). Nguồn data snapshot note_tag. */
public record NoteTagResponse(UUID id, UUID noteId, UUID tagId, long version, Instant createdAt, Instant updatedAt) {
}
