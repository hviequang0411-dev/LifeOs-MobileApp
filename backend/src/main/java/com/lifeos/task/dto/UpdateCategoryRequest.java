package com.lifeos.task.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/** PATCH /categories/{id} { version, name? } theo JSON Merge Patch; version bắt buộc (D58). Trường lạ bị bỏ qua. */
public class UpdateCategoryRequest {

    @NotNull
    private Long version;
    private Patch<String> name = Patch.absent();

    public UpdateCategoryRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = Patch.of(name);
    }

    public Long version() {
        return version;
    }

    public Patch<String> name() {
        return name;
    }

    @Override
    public String toString() {
        return "UpdateCategoryRequest[version=" + version + "]";
    }
}
