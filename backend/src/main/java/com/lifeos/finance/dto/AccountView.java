package com.lifeos.finance.dto;

import java.time.Instant;
import java.util.UUID;

/** GET /accounts, /accounts/{id}: tài khoản kèm số dư tính tại hôm nay theo múi giờ người dùng (D102, D108). */
public record AccountView(UUID id, String name, String accountType, String currency, String openingBalance,
        Instant archivedAt, long version, Instant createdAt, Instant updatedAt, String balance) {

    public static AccountView of(AccountResponse a, String balance) {
        return new AccountView(a.id(), a.name(), a.accountType(), a.currency(), a.openingBalance(), a.archivedAt(),
                a.version(), a.createdAt(), a.updatedAt(), balance);
    }

    @Override
    public String toString() {
        return "AccountView[id=" + id + ", version=" + version + "]";
    }
}
