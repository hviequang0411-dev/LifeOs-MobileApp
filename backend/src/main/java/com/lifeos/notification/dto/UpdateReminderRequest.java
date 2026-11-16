package com.lifeos.notification.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;

/**
 * PATCH /reminders/{id}. targetType/targetId bất biến.
 */
public class UpdateReminderRequest {

    @NotNull
    private Long version;
    private Patch<String> mode = Patch.absent();
    private Patch<Integer> offsetMinutes = Patch.absent();
    private Patch<String> localTime = Patch.absent();
    private Patch<String> localDate = Patch.absent();
    private Patch<Map<String, Object>> repeatRule = Patch.absent();
    private Patch<Boolean> urgent = Patch.absent();

    public UpdateReminderRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("mode")
    public void setMode(String mode) {
        this.mode = Patch.of(mode);
    }

    @JsonSetter("offsetMinutes")
    public void setOffsetMinutes(Integer offsetMinutes) {
        this.offsetMinutes = Patch.of(offsetMinutes);
    }

    @JsonSetter("localTime")
    public void setLocalTime(String localTime) {
        this.localTime = Patch.of(localTime);
    }

    @JsonSetter("localDate")
    public void setLocalDate(String localDate) {
        this.localDate = Patch.of(localDate);
    }

    @JsonSetter("repeatRule")
    public void setRepeatRule(Map<String, Object> repeatRule) {
        this.repeatRule = Patch.of(repeatRule);
    }

    @JsonSetter("urgent")
    public void setUrgent(Boolean urgent) {
        this.urgent = Patch.of(urgent);
    }

    public Long version() {
        return version;
    }

    public Patch<String> mode() {
        return mode;
    }

    public Patch<Integer> offsetMinutes() {
        return offsetMinutes;
    }

    public Patch<String> localTime() {
        return localTime;
    }

    public Patch<String> localDate() {
        return localDate;
    }

    public Patch<Map<String, Object>> repeatRule() {
        return repeatRule;
    }

    public Patch<Boolean> urgent() {
        return urgent;
    }

    @Override
    public String toString() {
        return "UpdateReminderRequest[" + "version=" + version + "]";
    }
}
