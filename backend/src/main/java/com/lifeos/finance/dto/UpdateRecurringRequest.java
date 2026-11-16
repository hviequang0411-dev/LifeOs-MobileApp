package com.lifeos.finance.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * PATCH /finance/recurring/{id}: đổi amount/category/note chỉ ảnh hưởng lần xuất hiện sau.
 */
public class UpdateRecurringRequest {

    @NotNull
    private Long version;
    private Patch<UUID> accountId = Patch.absent();
    private Patch<UUID> categoryId = Patch.absent();
    private Patch<String> kind = Patch.absent();
    private Patch<String> amount = Patch.absent();
    private Patch<String> note = Patch.absent();
    private Patch<String> frequency = Patch.absent();
    private Patch<Integer> intervalCount = Patch.absent();
    private Patch<String> startOn = Patch.absent();
    private Patch<String> endOn = Patch.absent();

    public UpdateRecurringRequest() {
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

    @JsonSetter("note")
    public void setNote(String note) {
        this.note = Patch.of(note);
    }

    @JsonSetter("frequency")
    public void setFrequency(String frequency) {
        this.frequency = Patch.of(frequency);
    }

    @JsonSetter("intervalCount")
    public void setIntervalCount(Integer intervalCount) {
        this.intervalCount = Patch.of(intervalCount);
    }

    @JsonSetter("startOn")
    public void setStartOn(String startOn) {
        this.startOn = Patch.of(startOn);
    }

    @JsonSetter("endOn")
    public void setEndOn(String endOn) {
        this.endOn = Patch.of(endOn);
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

    public Patch<String> note() {
        return note;
    }

    public Patch<String> frequency() {
        return frequency;
    }

    public Patch<Integer> intervalCount() {
        return intervalCount;
    }

    public Patch<String> startOn() {
        return startOn;
    }

    public Patch<String> endOn() {
        return endOn;
    }

    @Override
    public String toString() {
        return "UpdateRecurringRequest[version=" + version + "]";
    }
}
