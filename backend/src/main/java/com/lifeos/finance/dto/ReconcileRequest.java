package com.lifeos.finance.dto;

import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

/** POST /finance/accounts/{id}/reconcile (D112). */
public record ReconcileRequest(@JsonDeserialize(using = MoneyText.class) @NotNull String statementBalance,
        @NotNull String asOfDate) {

    @Override
    public String toString() {
        return "ReconcileRequest[]";
    }
}
