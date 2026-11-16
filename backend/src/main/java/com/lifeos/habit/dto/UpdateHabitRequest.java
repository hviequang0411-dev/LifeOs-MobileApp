package com.lifeos.habit.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/**
 * PATCH /habits/{id} { version, …trường đổi } theo JSON Merge Patch (D62): setter ghi nhận trường có mặt để phân biệt
 * "vắng" với "null". version bắt buộc (D58). Trường lạ bị bỏ qua.
 */
public class UpdateHabitRequest {

    @NotNull
    private Long version;
    private Patch<String> name = Patch.absent();
    private Patch<String> description = Patch.absent();
    private Patch<String> color = Patch.absent();
    private Patch<String> icon = Patch.absent();
    private Patch<String> scheduleType = Patch.absent();
    private Patch<Integer> weekdays = Patch.absent();
    private Patch<Integer> timesPerWeek = Patch.absent();
    private Patch<Integer> intervalDays = Patch.absent();
    private Patch<String> startOn = Patch.absent();
    private Patch<String> endOn = Patch.absent();
    private Patch<String> reminderTime = Patch.absent();

    public UpdateHabitRequest() {
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

    @JsonSetter("color")
    public void setColor(String color) {
        this.color = Patch.of(color);
    }

    @JsonSetter("icon")
    public void setIcon(String icon) {
        this.icon = Patch.of(icon);
    }

    @JsonSetter("scheduleType")
    public void setScheduleType(String scheduleType) {
        this.scheduleType = Patch.of(scheduleType);
    }

    @JsonSetter("weekdays")
    public void setWeekdays(Integer weekdays) {
        this.weekdays = Patch.of(weekdays);
    }

    @JsonSetter("timesPerWeek")
    public void setTimesPerWeek(Integer timesPerWeek) {
        this.timesPerWeek = Patch.of(timesPerWeek);
    }

    @JsonSetter("intervalDays")
    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = Patch.of(intervalDays);
    }

    @JsonSetter("startOn")
    public void setStartOn(String startOn) {
        this.startOn = Patch.of(startOn);
    }

    @JsonSetter("endOn")
    public void setEndOn(String endOn) {
        this.endOn = Patch.of(endOn);
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

    public Patch<String> description() {
        return description;
    }

    public Patch<String> color() {
        return color;
    }

    public Patch<String> icon() {
        return icon;
    }

    public Patch<String> scheduleType() {
        return scheduleType;
    }

    public Patch<Integer> weekdays() {
        return weekdays;
    }

    public Patch<Integer> timesPerWeek() {
        return timesPerWeek;
    }

    public Patch<Integer> intervalDays() {
        return intervalDays;
    }

    public Patch<String> startOn() {
        return startOn;
    }

    public Patch<String> endOn() {
        return endOn;
    }

    public Patch<String> reminderTime() {
        return reminderTime;
    }

    @Override
    public String toString() {
        return "UpdateHabitRequest[version=" + version + "]";
    }
}
