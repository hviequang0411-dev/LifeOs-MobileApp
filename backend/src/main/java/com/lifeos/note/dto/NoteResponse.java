package com.lifeos.note.dto;

import java.time.Instant;
import java.util.UUID;

/** Ghi chú đầy đủ (FR-NOTE-01). Nguồn data snapshot note. toString không in tiêu đề/nội dung (D138). */
public record NoteResponse(UUID id, String title, String content, boolean pinned, Instant archivedAt, long version,
        Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "NoteResponse[id=" + id + ", version=" + version + "]";
    }
}
