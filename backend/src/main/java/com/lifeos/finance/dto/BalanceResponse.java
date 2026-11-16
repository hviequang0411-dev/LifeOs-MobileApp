package com.lifeos.finance.dto;

import java.time.LocalDate;
import java.util.UUID;

/** GET /accounts/{id}/balance?asOf=: số dư cuối ngày asOf (D102). */
public record BalanceResponse(UUID accountId, String currency, LocalDate asOf, String balance) {

    @Override
    public String toString() {
        return "BalanceResponse[accountId=" + accountId + "]";
    }
}
