package com.lifeos.analytics.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Phản tư (D202). Nguồn data snapshot review. toString không in nội dung. */
public record ReviewResponse(UUID id, String periodType, LocalDate periodKey, String reflection, Integer mood,
        long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "ReviewResponse[id=" + id + ", version=" + version + "]";
    }
}
