package com.lifeos.habit.dto;

/** Trạng thái Habit cho snapshot sync (kể cả tombstone). */
public record HabitSyncState(HabitResponse habit, boolean deleted) {
}
