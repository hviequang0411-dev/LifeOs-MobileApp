package com.lifeos.habit.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** GET /habits, /habits/{id}: Habit kèm chuỗi tính tại thời điểm đọc (D233; không lưu, không đồng bộ). */
public record HabitView(UUID id, String name, String description, String color, String icon, String scheduleType,
        Integer weekdays, Integer timesPerWeek, Integer intervalDays, LocalDate startOn, LocalDate endOn,
        String reminderTime, Instant archivedAt, long version, Instant createdAt, Instant updatedAt,
        int currentStreak, int longestStreak) {

    public static HabitView of(HabitResponse h, int currentStreak, int longestStreak) {
        return new HabitView(h.id(), h.name(), h.description(), h.color(), h.icon(), h.scheduleType(), h.weekdays(),
                h.timesPerWeek(), h.intervalDays(), h.startOn(), h.endOn(), h.reminderTime(), h.archivedAt(),
                h.version(), h.createdAt(), h.updatedAt(), currentStreak, longestStreak);
    }

    @Override
    public String toString() {
        return "HabitView[id=" + id + ", version=" + version + "]";
    }
}
