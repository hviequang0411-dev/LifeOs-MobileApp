package com.lifeos.routine.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/** PATCH /routines/{id} theo JSON Merge Patch; version bắt buộc. kind có mặt thì weekdays vắng = null (thay cả bộ). */
public class UpdateRoutineRequest {

    @NotNull
    private Long version;
    private Patch<String> name = Patch.absent();
    private Patch<String> kind = Patch.absent();
    private Patch<Integer> weekdays = Patch.absent();
    private Patch<String> reminderTime = Patch.absent();

    public UpdateRoutineRequest() {
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

    @JsonSetter("weekdays")
    public void setWeekdays(Integer weekdays) {
        this.weekdays = Patch.of(weekdays);
    }

    @JsonSetter("reminderTime")
    public void setReminderTime(String reminderTime) {
        this.reminderTime = Patch.of(reminderTime);
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

    public Patch<Integer> weekdays() {
        return weekdays;
    }

    public Patch<String> reminderTime() {
        return reminderTime;
    }

    @Override
    public String toString() {
        return "UpdateRoutineRequest[version=" + version + "]";
    }
}
