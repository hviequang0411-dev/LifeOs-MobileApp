package com.lifeos.note.dto;

import java.time.Instant;
import java.util.UUID;

/** Thẻ (D184). Nguồn data snapshot tag. toString không in tên. */
public record TagResponse(UUID id, String name, String color, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "TagResponse[id=" + id + ", version=" + version + "]";
    }
}
