package com.lifeos.finance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Khoản góp tiết kiệm (D111). */
public record ContributionResponse(UUID id, UUID goalId, String amount, LocalDate occurredOn, String note,
        long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "ContributionResponse[id=" + id + ", version=" + version + "]";
    }
}
