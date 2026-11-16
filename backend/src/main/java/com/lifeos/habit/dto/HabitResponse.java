package com.lifeos.habit.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tài nguyên Habit (PHASE_2 §6). weekdays là bitmask (Thứ Hai = 1 … Chủ Nhật = 64, D230); reminderTime "HH:mm".
 * Cũng là nguồn {@code data} của snapshot sync (bỏ id, version — SYNC_PROTOCOL §6.1). toString không in tên/mô tả.
 */
public record HabitResponse(UUID id, String name, String description, String color, String icon,
        String scheduleType, Integer weekdays, Integer timesPerWeek, Integer intervalDays, LocalDate startOn,
        LocalDate endOn, String reminderTime, Instant archivedAt, long version, Instant createdAt,
        Instant updatedAt) {

    @Override
    public String toString() {
        return "HabitResponse[id=" + id + ", version=" + version + "]";
    }
}
