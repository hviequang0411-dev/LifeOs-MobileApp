package com.lifeos.finance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Chuyển khoản (FR-FIN-05, D104). */
public record TransferResponse(UUID id, UUID fromAccountId, UUID toAccountId, String amount, String currency,
        LocalDate occurredOn, String note, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "TransferResponse[id=" + id + ", version=" + version + "]";
    }
}
