package com.lifeos.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/** POST /finance/budgets. categoryId vắng = ngân sách tổng chi. */
public record CreateBudgetRequest(@NotNull UUID id, UUID categoryId, @NotNull String currency,
        @JsonDeserialize(using = MoneyText.class) @NotNull String amount) {

    @Override
    public String toString() {
        return "CreateBudgetRequest[id=" + id + "]";
    }
}
