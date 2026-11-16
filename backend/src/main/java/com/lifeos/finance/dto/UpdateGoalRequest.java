package com.lifeos.finance.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * PATCH /finance/saving-goals/{id}. status: ACTIVE | ARCHIVED (ACHIEVED do server, gửi lên bị bỏ qua).
 */
public class UpdateGoalRequest {

    @NotNull
    private Long version;
    private Patch<String> name = Patch.absent();
    private Patch<String> currency = Patch.absent();
    private Patch<String> targetAmount = Patch.absent();
    private Patch<String> targetOn = Patch.absent();
    private Patch<String> status = Patch.absent();

    public UpdateGoalRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = Patch.of(name);
    }

    @JsonSetter("currency")
    public void setCurrency(String currency) {
        this.currency = Patch.of(currency);
    }

    @JsonSetter("targetAmount")
    @JsonDeserialize(using = MoneyText.class)
    public void setTargetAmount(String targetAmount) {
        this.targetAmount = Patch.of(targetAmount);
    }

    @JsonSetter("targetOn")
    public void setTargetOn(String targetOn) {
        this.targetOn = Patch.of(targetOn);
    }

    @JsonSetter("status")
    public void setStatus(String status) {
        this.status = Patch.of(status);
    }

    public Long version() {
        return version;
    }

    public Patch<String> name() {
        return name;
    }

    public Patch<String> currency() {
        return currency;
    }

    public Patch<String> targetAmount() {
        return targetAmount;
    }

    public Patch<String> targetOn() {
        return targetOn;
    }

    public Patch<String> status() {
        return status;
    }

    @Override
    public String toString() {
        return "UpdateGoalRequest[version=" + version + "]";
    }
}
