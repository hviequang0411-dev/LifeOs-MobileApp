package com.lifeos.finance.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Giao dịch thu/chi (FR-FIN-04). amount là chuỗi; currency, recurringTransactionId, recurrenceDate do server. */
public record TransactionResponse(UUID id, UUID accountId, UUID categoryId, String kind, String amount,
        String currency, LocalDate occurredOn, String note, UUID recurringTransactionId, LocalDate recurrenceDate,
        long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "TransactionResponse[id=" + id + ", version=" + version + "]";
    }
}
