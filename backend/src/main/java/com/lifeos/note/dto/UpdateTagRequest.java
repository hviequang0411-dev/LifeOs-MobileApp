package com.lifeos.note.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/** PATCH /tags/{id} theo JSON Merge Patch. */
public class UpdateTagRequest {

    @NotNull
    private Long version;
    private Patch<String> name = Patch.absent();
    private Patch<String> color = Patch.absent();

    public UpdateTagRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = Patch.of(name);
    }

    @JsonSetter("color")
    public void setColor(String color) {
        this.color = Patch.of(color);
    }

    public Long version() {
        return version;
    }

    public Patch<String> name() {
        return name;
    }

    public Patch<String> color() {
        return color;
    }

    @Override
    public String toString() {
        return "UpdateTagRequest[version=" + version + "]";
    }
}
