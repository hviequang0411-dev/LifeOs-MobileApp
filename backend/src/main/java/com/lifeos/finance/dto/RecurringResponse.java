package com.lifeos.finance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Giao dịch định kỳ (D110). nextRunOn do server. */
public record RecurringResponse(UUID id, UUID accountId, UUID categoryId, String kind, String amount,
        String currency, String note, String frequency, int intervalCount, LocalDate startOn, LocalDate endOn,
        LocalDate nextRunOn, String status, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "RecurringResponse[id=" + id + ", version=" + version + "]";
    }
}
