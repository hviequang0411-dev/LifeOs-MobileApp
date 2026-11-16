package com.lifeos.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/** POST /finance/saving-goals. */
public record CreateGoalRequest(@NotNull UUID id, @NotNull String name, @NotNull String currency,
        @JsonDeserialize(using = MoneyText.class) @NotNull String targetAmount, String targetOn) {

    @Override
    public String toString() {
        return "CreateGoalRequest[id=" + id + "]";
    }
}
