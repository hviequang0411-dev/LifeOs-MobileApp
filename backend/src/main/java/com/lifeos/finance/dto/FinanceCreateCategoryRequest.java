package com.lifeos.finance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** POST /finance/categories. */
public record FinanceCreateCategoryRequest(@NotNull UUID id, @NotNull String name, @NotNull String kind) {

    @Override
    public String toString() {
        return "FinanceCreateCategoryRequest[id=" + id + "]";
    }
}
