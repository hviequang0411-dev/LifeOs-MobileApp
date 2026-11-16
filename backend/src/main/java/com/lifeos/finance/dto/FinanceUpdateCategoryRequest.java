package com.lifeos.finance.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/**
 * PATCH /finance/categories/{id}. kind bất biến.
 */
public class FinanceUpdateCategoryRequest {

    @NotNull
    private Long version;
    private Patch<String> name = Patch.absent();
    private Patch<String> kind = Patch.absent();

    public FinanceUpdateCategoryRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = Patch.of(name);
    }

    @JsonSetter("kind")
    public void setKind(String kind) {
        this.kind = Patch.of(kind);
    }

    public Long version() {
        return version;
    }

    public Patch<String> name() {
        return name;
    }

    public Patch<String> kind() {
        return kind;
    }

    @Override
    public String toString() {
        return "FinanceUpdateCategoryRequest[version=" + version + "]";
    }
}
