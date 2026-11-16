package com.lifeos.project.dto;

import java.time.Instant;
import java.util.UUID;

/** Tài nguyên Project (API_CONVENTIONS §8.5). toString không in name/description (SECURITY_RULES §11.1). */
public record ProjectResponse(UUID id, String name, String description, Instant archivedAt, long version,
        Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "ProjectResponse[id=" + id + ", version=" + version + "]";
    }
}
