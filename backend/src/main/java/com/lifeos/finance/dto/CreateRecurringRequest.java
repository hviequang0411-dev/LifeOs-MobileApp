package com.lifeos.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/** POST /finance/recurring. nextRunOn do server. */
public record CreateRecurringRequest(@NotNull UUID id, @NotNull UUID accountId, UUID categoryId,
        @NotNull String kind, @JsonDeserialize(using = MoneyText.class) @NotNull String amount, String note,
        @NotNull String frequency, Integer intervalCount, @NotNull String startOn, String endOn) {

    @Override
    public String toString() {
        return "CreateRecurringRequest[id=" + id + "]";
    }
}
