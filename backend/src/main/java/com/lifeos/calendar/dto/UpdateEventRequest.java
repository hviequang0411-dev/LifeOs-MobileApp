package com.lifeos.calendar.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * PATCH /calendar/events/{id}: sửa CẢ CHUỖI ("tất cả"). JSON Merge Patch, version bắt buộc.
 */
public class UpdateEventRequest {

    @NotNull
    private Long version;
    private Patch<String> title = Patch.absent();
    private Patch<String> description = Patch.absent();
    private Patch<String> location = Patch.absent();
    private Patch<Boolean> allDay = Patch.absent();
    private Patch<String> timezone = Patch.absent();
    private Patch<String> startLocal = Patch.absent();
    private Patch<String> endLocal = Patch.absent();
    private Patch<String> startDate = Patch.absent();
    private Patch<String> endDate = Patch.absent();
    private Patch<Map<String, Object>> recurrence = Patch.absent();

    public UpdateEventRequest() {
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

    @JsonSetter("location")
    public void setLocation(String location) {
        this.location = Patch.of(location);
    }

    @JsonSetter("allDay")
    public void setAllDay(Boolean allDay) {
        this.allDay = Patch.of(allDay);
    }

    @JsonSetter("timezone")
    public void setTimezone(String timezone) {
        this.timezone = Patch.of(timezone);
    }

    @JsonSetter("startLocal")
    public void setStartLocal(String startLocal) {
        this.startLocal = Patch.of(startLocal);
    }

    @JsonSetter("endLocal")
    public void setEndLocal(String endLocal) {
        this.endLocal = Patch.of(endLocal);
    }

    @JsonSetter("startDate")
    public void setStartDate(String startDate) {
        this.startDate = Patch.of(startDate);
    }

    @JsonSetter("endDate")
    public void setEndDate(String endDate) {
        this.endDate = Patch.of(endDate);
    }

    @JsonSetter("recurrence")
    public void setRecurrence(Map<String, Object> recurrence) {
        this.recurrence = Patch.of(recurrence);
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

    public Patch<String> location() {
        return location;
    }

    public Patch<Boolean> allDay() {
        return allDay;
    }

    public Patch<String> timezone() {
        return timezone;
    }

    public Patch<String> startLocal() {
        return startLocal;
    }

    public Patch<String> endLocal() {
        return endLocal;
    }

    public Patch<String> startDate() {
        return startDate;
    }

    public Patch<String> endDate() {
        return endDate;
    }

    public Patch<Map<String, Object>> recurrence() {
        return recurrence;
    }

    @Override
    public String toString() {
        return "UpdateEventRequest[" + "version=" + version + "]";
    }
}
