package com.lifeos.finance.dto;

import java.time.Instant;
import java.util.UUID;

/** Danh mục thu/chi (D106, D113). systemKey khác null với danh mục hệ thống. */
public record FinanceCategoryResponse(UUID id, String name, String kind, String systemKey, Instant archivedAt,
        long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "FinanceCategoryResponse[id=" + id + ", version=" + version + "]";
    }
}
