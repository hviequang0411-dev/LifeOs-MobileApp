package com.lifeos.finance.dto;

import java.time.Instant;
import java.util.UUID;

/** Ngân sách tháng (D109). categoryId null = tổng chi. */
public record BudgetResponse(UUID id, UUID categoryId, String currency, String amount, Instant archivedAt,
        long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "BudgetResponse[id=" + id + ", version=" + version + "]";
    }
}
