package com.lifeos.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/** POST /finance/transactions. currency do server đặt theo tài khoản (D103): gửi lên bị bỏ qua. */
public record CreateTransactionRequest(@NotNull UUID id, @NotNull UUID accountId, UUID categoryId,
        @NotNull String kind, @JsonDeserialize(using = MoneyText.class) @NotNull String amount,
        @NotNull String occurredOn, String note) {

    @Override
    public String toString() {
        return "CreateTransactionRequest[id=" + id + "]";
    }
}
