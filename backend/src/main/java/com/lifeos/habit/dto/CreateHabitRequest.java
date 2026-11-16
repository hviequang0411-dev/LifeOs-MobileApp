package com.lifeos.habit.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * POST /habits. id do client tạo. Giá trị (tên, lịch D230, ngày) kiểm ở service; trường do server quản (version,
 * archivedAt, createdAt…) bị bỏ qua. startOn vắng = hôm nay theo múi giờ người dùng.
 */
public record CreateHabitRequest(@NotNull UUID id, @NotNull String name, String description, String color,
        String icon, @NotNull String scheduleType, Integer weekdays, Integer timesPerWeek, Integer intervalDays,
        String startOn, String endOn, String reminderTime) {

    @Override
    public String toString() {
        return "CreateHabitRequest[id=" + id + "]";
    }
}
