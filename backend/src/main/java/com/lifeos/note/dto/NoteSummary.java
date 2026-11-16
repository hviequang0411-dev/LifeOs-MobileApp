package com.lifeos.note.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Hàng danh sách/tìm kiếm: excerpt ≤ 200 ký tự và contentLength (code point), kèm id thẻ. */
public record NoteSummary(UUID id, String title, String excerpt, int contentLength, boolean pinned,
        Instant archivedAt, List<UUID> tagIds, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "NoteSummary[id=" + id + "]";
    }
}
