package com.lifeos.finance.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * PATCH /finance/accounts/{id} (JSON Merge Patch, version bắt buộc). currency đổi được khi chưa có giao dịch (FINANCE_CURRENCY_LOCKED).
 */
public class UpdateAccountRequest {

    @NotNull
    private Long version;
    private Patch<String> name = Patch.absent();
    private Patch<String> accountType = Patch.absent();
    private Patch<String> currency = Patch.absent();
    private Patch<String> openingBalance = Patch.absent();

    public UpdateAccountRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = Patch.of(name);
    }

    @JsonSetter("accountType")
    public void setAccountType(String accountType) {
        this.accountType = Patch.of(accountType);
    }

    @JsonSetter("currency")
    public void setCurrency(String currency) {
        this.currency = Patch.of(currency);
    }

    @JsonSetter("openingBalance")
    @JsonDeserialize(using = MoneyText.class)
    public void setOpeningBalance(String openingBalance) {
        this.openingBalance = Patch.of(openingBalance);
    }

    public Long version() {
        return version;
    }

    public Patch<String> name() {
        return name;
    }

    public Patch<String> accountType() {
        return accountType;
    }

    public Patch<String> currency() {
        return currency;
    }

    public Patch<String> openingBalance() {
        return openingBalance;
    }

    @Override
    public String toString() {
        return "UpdateAccountRequest[version=" + version + "]";
    }
}
