package com.lifeos.project.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/**
 * PATCH /projects/{id} { version, name?, description? } theo JSON Merge Patch: setter ghi nhận trường có mặt để phân
 * biệt "vắng" với "null". version bắt buộc (D58). Trường lạ bị bỏ qua.
 */
public class UpdateProjectRequest {

    @NotNull
    private Long version;
    private Patch<String> name = Patch.absent();
    private Patch<String> description = Patch.absent();

    public UpdateProjectRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = Patch.of(name);
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = Patch.of(description);
    }

    public Long version() {
        return version;
    }

    public Patch<String> name() {
        return name;
    }

    public Patch<String> description() {
        return description;
    }

    @Override
    public String toString() {
        return "UpdateProjectRequest[version=" + version + "]";
    }
}
