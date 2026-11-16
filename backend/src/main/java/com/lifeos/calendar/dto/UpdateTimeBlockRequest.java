package com.lifeos.calendar.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * PATCH /time-blocks/{id}. taskId bất biến.
 */
public class UpdateTimeBlockRequest {

    @NotNull
    private Long version;
    private Patch<UUID> taskId = Patch.absent();
    private Patch<String> startAt = Patch.absent();
    private Patch<String> endAt = Patch.absent();
    private Patch<String> note = Patch.absent();

    public UpdateTimeBlockRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("taskId")
    public void setTaskId(UUID taskId) {
        this.taskId = Patch.of(taskId);
    }

    @JsonSetter("startAt")
    public void setStartAt(String startAt) {
        this.startAt = Patch.of(startAt);
    }

    @JsonSetter("endAt")
    public void setEndAt(String endAt) {
        this.endAt = Patch.of(endAt);
    }

    @JsonSetter("note")
    public void setNote(String note) {
        this.note = Patch.of(note);
    }

    public Long version() {
        return version;
    }

    public Patch<UUID> taskId() {
        return taskId;
    }

    public Patch<String> startAt() {
        return startAt;
    }

    public Patch<String> endAt() {
        return endAt;
    }

    public Patch<String> note() {
        return note;
    }

    @Override
    public String toString() {
        return "UpdateTimeBlockRequest[" + "version=" + version + "]";
    }
}
