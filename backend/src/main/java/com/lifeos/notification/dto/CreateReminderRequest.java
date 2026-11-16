package com.lifeos.notification.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

/** POST /reminders. mode OFFSET (offsetMinutes trước hạn Task/giờ bắt đầu Event) hoặc LOCAL_AT (localTime [+ localDate], giờ địa phương người dùng). */
public record CreateReminderRequest(@NotNull UUID id, @NotNull String targetType, UUID targetId,
        @NotNull String mode, Integer offsetMinutes, String localTime, String localDate,
        Map<String, Object> repeatRule, Boolean urgent) {

    @Override
    public String toString() {
        return "CreateReminderRequest[id=" + id + "]";
    }
}
