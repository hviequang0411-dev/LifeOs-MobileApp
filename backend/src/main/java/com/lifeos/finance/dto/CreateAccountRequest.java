package com.lifeos.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/** POST /finance/accounts. openingBalance là chuỗi (vắng = "0"); số JSON bị từ chối. */
public record CreateAccountRequest(@NotNull UUID id, @NotNull String name, @NotNull String accountType,
        @NotNull String currency, @JsonDeserialize(using = MoneyText.class) String openingBalance) {

    @Override
    public String toString() {
        return "CreateAccountRequest[id=" + id + "]";
    }
}
