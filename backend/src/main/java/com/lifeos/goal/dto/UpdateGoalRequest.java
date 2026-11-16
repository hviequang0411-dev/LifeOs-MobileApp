package com.lifeos.goal.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/** PATCH /goals/{id} theo JSON Merge Patch; version bắt buộc. Trạng thái đổi qua POST /goals/{id}/status. */
public class UpdateGoalRequest {

    @NotNull
    private Long version;
    private Patch<String> title = Patch.absent();
    private Patch<String> description = Patch.absent();
    private Patch<String> startOn = Patch.absent();
    private Patch<String> targetOn = Patch.absent();

    public UpdateGoalRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = Patch.of(title);
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = Patch.of(description);
    }

    @JsonSetter("startOn")
    public void setStartOn(String startOn) {
        this.startOn = Patch.of(startOn);
    }

    @JsonSetter("targetOn")
    public void setTargetOn(String targetOn) {
        this.targetOn = Patch.of(targetOn);
    }

    public Long version() {
        return version;
    }

    public Patch<String> title() {
        return title;
    }

    public Patch<String> description() {
        return description;
    }

    public Patch<String> startOn() {
        return startOn;
    }

    public Patch<String> targetOn() {
        return targetOn;
    }

    @Override
    public String toString() {
        return "UpdateGoalRequest[version=" + version + "]";
    }
}
