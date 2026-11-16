package com.lifeos.task.dto;

import java.time.Instant;
import java.util.UUID;

/** Tài nguyên Category (API_CONVENTIONS §8.6). toString không in name (SECURITY_RULES §11.1). */
public record CategoryResponse(UUID id, String name, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "CategoryResponse[id=" + id + ", version=" + version + "]";
    }
}
