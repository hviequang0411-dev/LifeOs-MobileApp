package com.lifeos.routine.service;

import com.lifeos.common.util.Patch;
import java.util.UUID;

/** Lệnh của use case Routine (REST và sync gọi cùng service). Trường Patch: vắng = không đổi / mặc định. */
public final class RoutineCommands {

    private RoutineCommands() {
    }

    public record Create(UUID id, Patch<String> name, Patch<String> kind, Patch<Integer> weekdays,
            Patch<String> reminderTime) {
    }

    /** kind có mặt thì weekdays vắng mặt = null (lịch thay cả bộ) [PROPOSED]. */
    public record Update(long expectedVersion, Patch<String> name, Patch<String> kind, Patch<Integer> weekdays,
            Patch<String> reminderTime) {
    }

    public record CreateStep(UUID id, Patch<UUID> routineId, Patch<String> title, Patch<Long> position,
            Patch<Integer> durationMinutes) {
    }

    /** routineId bất biến (đổi → VALIDATION_FAILED). */
    public record UpdateStep(long expectedVersion, Patch<UUID> routineId, Patch<String> title, Patch<Long> position,
            Patch<Integer> durationMinutes) {
    }

    public record StartLog(UUID id, Patch<UUID> routineId, Patch<String> localDate, Patch<Integer> totalSteps) {
    }

    /** Lượt chạy: routineId, localDate, totalSteps bất biến; completedAt do server (không nhận từ client). */
    public record UpdateLog(long expectedVersion, Patch<UUID> routineId, Patch<String> localDate,
            Patch<Integer> totalSteps) {
    }

    public record CreateStepLog(UUID id, Patch<UUID> routineLogId, Patch<UUID> stepId, Patch<String> status) {
    }

    /** routineLogId, stepId bất biến; chỉ đổi status. */
    public record UpdateStepLog(long expectedVersion, Patch<UUID> routineLogId, Patch<UUID> stepId,
            Patch<String> status) {
    }
}
