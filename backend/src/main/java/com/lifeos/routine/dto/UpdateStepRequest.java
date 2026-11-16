package com.lifeos.routine.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/** PATCH /routine-steps/{id} theo JSON Merge Patch; version bắt buộc. */
public class UpdateStepRequest {

    @NotNull
    private Long version;
    private Patch<String> title = Patch.absent();
    private Patch<Long> position = Patch.absent();
    private Patch<Integer> durationMinutes = Patch.absent();

    public UpdateStepRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = Patch.of(title);
    }

    @JsonSetter("position")
    public void setPosition(Long position) {
        this.position = Patch.of(position);
    }

    @JsonSetter("durationMinutes")
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = Patch.of(durationMinutes);
    }

    public Long version() {
        return version;
    }

    public Patch<String> title() {
        return title;
    }

    public Patch<Long> position() {
        return position;
    }

    public Patch<Integer> durationMinutes() {
        return durationMinutes;
    }

    @Override
    public String toString() {
        return "UpdateStepRequest[version=" + version + "]";
    }
}
