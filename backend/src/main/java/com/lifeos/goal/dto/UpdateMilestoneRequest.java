package com.lifeos.goal.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/** PATCH /milestones/{id} theo JSON Merge Patch; hoàn thành qua /complete, /reopen. */
public class UpdateMilestoneRequest {

    @NotNull
    private Long version;
    private Patch<String> title = Patch.absent();
    private Patch<String> dueOn = Patch.absent();
    private Patch<Long> position = Patch.absent();

    public UpdateMilestoneRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("title")
    public void setTitle(String title) {
        this.title = Patch.of(title);
    }

    @JsonSetter("dueOn")
    public void setDueOn(String dueOn) {
        this.dueOn = Patch.of(dueOn);
    }

    @JsonSetter("position")
    public void setPosition(Long position) {
        this.position = Patch.of(position);
    }

    public Long version() {
        return version;
    }

    public Patch<String> title() {
        return title;
    }

    public Patch<String> dueOn() {
        return dueOn;
    }

    public Patch<Long> position() {
        return position;
    }

    @Override
    public String toString() {
        return "UpdateMilestoneRequest[version=" + version + "]";
    }
}
