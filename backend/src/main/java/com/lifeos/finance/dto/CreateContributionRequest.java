package com.lifeos.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/** POST /finance/saving-goals/{id}/contributions. */
public record CreateContributionRequest(@NotNull UUID id,
        @JsonDeserialize(using = MoneyText.class) @NotNull String amount, @NotNull String occurredOn, String note) {

    @Override
    public String toString() {
        return "CreateContributionRequest[id=" + id + "]";
    }
}
