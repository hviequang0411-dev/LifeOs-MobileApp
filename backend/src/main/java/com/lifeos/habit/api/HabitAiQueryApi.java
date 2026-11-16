package com.lifeos.habit.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Minimal, ownership-scoped Habit snapshot exposed to AI. */
public interface HabitAiQueryApi {
    record Snapshot(UUID id, String name, String scheduleType, LocalDate startOn, LocalDate endOn,
            int currentStreak, int longestStreak, long version) { }

    List<Snapshot> aiHabits(UUID userId, int limit);
}
