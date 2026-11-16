package com.lifeos.finance.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * PATCH /finance/transfers/{id}. Hai tài khoản bất biến (đổi → VALIDATION_FAILED; xóa và tạo lại).
 */
public class UpdateTransferRequest {

    @NotNull
    private Long version;
    private Patch<UUID> fromAccountId = Patch.absent();
    private Patch<UUID> toAccountId = Patch.absent();
    private Patch<String> amount = Patch.absent();
    private Patch<String> occurredOn = Patch.absent();
    private Patch<String> note = Patch.absent();

    public UpdateTransferRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("fromAccountId")
    public void setFromAccountId(UUID fromAccountId) {
        this.fromAccountId = Patch.of(fromAccountId);
    }

    @JsonSetter("toAccountId")
    public void setToAccountId(UUID toAccountId) {
        this.toAccountId = Patch.of(toAccountId);
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

    public Patch<UUID> fromAccountId() {
        return fromAccountId;
    }

    public Patch<UUID> toAccountId() {
        return toAccountId;
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
        return "UpdateTransferRequest[version=" + version + "]";
    }
}
