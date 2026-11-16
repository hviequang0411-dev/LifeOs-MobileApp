package com.lifeos.finance.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * PATCH /finance/transactions/{id}.
 */
public class UpdateTransactionRequest {

    @NotNull
    private Long version;
    private Patch<UUID> accountId = Patch.absent();
    private Patch<UUID> categoryId = Patch.absent();
    private Patch<String> kind = Patch.absent();
    private Patch<String> amount = Patch.absent();
    private Patch<String> occurredOn = Patch.absent();
    private Patch<String> note = Patch.absent();

    public UpdateTransactionRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("accountId")
    public void setAccountId(UUID accountId) {
        this.accountId = Patch.of(accountId);
    }

    @JsonSetter("categoryId")
    public void setCategoryId(UUID categoryId) {
        this.categoryId = Patch.of(categoryId);
    }

    @JsonSetter("kind")
    public void setKind(String kind) {
        this.kind = Patch.of(kind);
    }

    @JsonSetter("amount")
    @JsonDeserialize(using = MoneyText.class)
    public void setAmount(String amount) {
        this.amount = Patch.of(amount);
    }

    @JsonSetter("occurredOn")
    public void setOccurredOn(String occurredOn) {
        this.occurredOn = Patch.of(occurredOn);
    }

    @JsonSetter("note")
    public void setNote(String note) {
        this.note = Patch.of(note);
    }

    public Long version() {
        return version;
    }

    public Patch<UUID> accountId() {
        return accountId;
    }

    public Patch<UUID> categoryId() {
        return categoryId;
    }

    public Patch<String> kind() {
        return kind;
    }

    public Patch<String> amount() {
        return amount;
    }

    public Patch<String> occurredOn() {
        return occurredOn;
    }

    public Patch<String> note() {
        return note;
    }

    @Override
    public String toString() {
        return "UpdateTransactionRequest[version=" + version + "]";
    }
}
