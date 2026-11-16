package com.lifeos.finance.dto;

import java.time.LocalDate;
import java.util.UUID;

/** POST /accounts/{id}/reconcile (D112): chỉ đọc; difference = statementBalance − computedBalance. */
public record ReconcileResponse(UUID accountId, String currency, LocalDate asOfDate, String statementBalance,
        String computedBalance, String difference) {

    @Override
    public String toString() {
        return "ReconcileResponse[accountId=" + accountId + "]";
    }
}
