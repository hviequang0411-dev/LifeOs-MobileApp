package com.lifeos.finance.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * PATCH /finance/budgets/{id}.
 */
public class UpdateBudgetRequest {

    @NotNull
    private Long version;
    private Patch<UUID> categoryId = Patch.absent();
    private Patch<String> currency = Patch.absent();
    private Patch<String> amount = Patch.absent();

    public UpdateBudgetRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("categoryId")
    public void setCategoryId(UUID categoryId) {
        this.categoryId = Patch.of(categoryId);
    }

    @JsonSetter("currency")
    public void setCurrency(String currency) {
        this.currency = Patch.of(currency);
    }

    @JsonSetter("amount")
    @JsonDeserialize(using = MoneyText.class)
    public void setAmount(String amount) {
        this.amount = Patch.of(amount);
    }

    public Long version() {
        return version;
    }

    public Patch<UUID> categoryId() {
        return categoryId;
    }

    public Patch<String> currency() {
        return currency;
    }

    public Patch<String> amount() {
        return amount;
    }

    @Override
    public String toString() {
        return "UpdateBudgetRequest[version=" + version + "]";
    }
}
