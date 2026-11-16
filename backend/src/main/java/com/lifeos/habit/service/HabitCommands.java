package com.lifeos.habit.service;

import com.lifeos.common.util.Patch;
import java.util.UUID;

/**
 * Lệnh của use case Habit/check-in (JAVA_SPRING_RULES §2.2): không phụ thuộc HTTP, REST và sync gọi cùng service.
 * Trường dạng {@link Patch}: vắng = không đổi (update) / mặc định (create); payload sync sai kiểu nổ ở bước kiểm giá
 * trị, sau bước tồn tại/version (D96). toString không in dữ liệu người dùng.
 */
public final class HabitCommands {

    private HabitCommands() {
    }

    public record Create(UUID id, Patch<String> name, Patch<String> description, Patch<String> color,
            Patch<String> icon, Patch<String> scheduleType, Patch<Integer> weekdays, Patch<Integer> timesPerWeek,
            Patch<Integer> intervalDays, Patch<String> startOn, Patch<String> endOn, Patch<String> reminderTime) {
        @Override
        public String toString() {
            return "HabitCommands.Create[id=" + id + "]";
        }
    }

    /**
     * JSON Merge Patch. Lịch là một bộ (D254): khi scheduleType có mặt, weekdays/timesPerWeek/intervalDays vắng mặt
     * được hiểu là null (thay cả bộ) [PROPOSED].
     */
    public record Update(long expectedVersion, Patch<String> name, Patch<String> description, Patch<String> color,
            Patch<String> icon, Patch<String> scheduleType, Patch<Integer> weekdays, Patch<Integer> timesPerWeek,
            Patch<Integer> intervalDays, Patch<String> startOn, Patch<String> endOn, Patch<String> reminderTime) {
        @Override
        public String toString() {
            return "HabitCommands.Update[expectedVersion=" + expectedVersion + "]";
        }
    }

    /** CREATE check-in qua sync (khóa tự nhiên habit + ngày, D235). */
    public record CreateCheckin(UUID id, Patch<UUID> habitId, Patch<String> localDate, Patch<String> status,
            Patch<String> note) {
        @Override
        public String toString() {
            return "HabitCommands.CreateCheckin[id=" + id + "]";
        }
    }

    /** UPDATE check-in qua sync: habitId, localDate bất biến (đổi → VALIDATION_FAILED). */
    public record UpdateCheckin(long expectedVersion, Patch<UUID> habitId, Patch<String> localDate,
            Patch<String> status, Patch<String> note) {
        @Override
        public String toString() {
            return "HabitCommands.UpdateCheckin[expectedVersion=" + expectedVersion + "]";
        }
    }
}
