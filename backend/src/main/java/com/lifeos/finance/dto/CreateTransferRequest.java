package com.lifeos.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/** POST /finance/transfers. */
public record CreateTransferRequest(@NotNull UUID id, @NotNull UUID fromAccountId, @NotNull UUID toAccountId,
        @JsonDeserialize(using = MoneyText.class) @NotNull String amount, @NotNull String occurredOn, String note) {

    @Override
    public String toString() {
        return "CreateTransferRequest[id=" + id + "]";
    }
}
