package com.lifeos.habit.api;

import java.util.UUID;

/** Sự kiện miền: Habit vừa xóa mềm + flush, trước change_logs của Habit (D86); notification xóa reminder của nó. */
public record HabitDeletionStarted(UUID userId, UUID habitId) {
}
