package com.lifeos.routine.api;

import java.util.UUID;

/** Sự kiện miền: Routine vừa xóa mềm + flush, trước change_logs của Routine (D86); notification xóa reminder. */
public record RoutineDeletionStarted(UUID userId, UUID routineId) {
}
