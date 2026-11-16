package com.lifeos.finance.dto;

import java.time.Instant;
import java.util.UUID;

/** Tài khoản (PHASE_3 §6). openingBalance là chuỗi thập phân (D100). Nguồn data của snapshot sync. */
public record AccountResponse(UUID id, String name, String accountType, String currency, String openingBalance,
        Instant archivedAt, long version, Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "AccountResponse[id=" + id + ", version=" + version + "]";
    }
}
