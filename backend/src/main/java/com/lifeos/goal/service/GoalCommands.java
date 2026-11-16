package com.lifeos.goal.service;

import com.lifeos.common.util.Patch;
import java.util.UUID;

/** Lệnh của use case Goal/Milestone (REST và sync gọi cùng service). Patch vắng = không đổi / mặc định. */
public final class GoalCommands {

    private GoalCommands() {
    }

    /** status vắng = ACTIVE; statusChangedAt luôn do server. */
    public record Create(UUID id, Patch<String> title, Patch<String> description, Patch<String> startOn,
            Patch<String> targetOn, Patch<String> status) {
    }

    /** status có mặt phải là chuyển hợp lệ D188 (REST đổi status qua POST /goals/{id}/status). */
    public record Update(long expectedVersion, Patch<String> title, Patch<String> description, Patch<String> startOn,
            Patch<String> targetOn, Patch<String> status) {
        @Override
        public String toString() {
            return "GoalCommands.Update[expectedVersion=" + expectedVersion + "]";
        }
    }

    /** completed (chỉ sync): true ⇒ server đặt completedAt, false ⇒ mở lại [PROPOSED]. */
    public record CreateMilestone(UUID id, Patch<UUID> goalId, Patch<String> title, Patch<String> dueOn,
            Patch<Long> position, Patch<Boolean> completed) {
    }

    /** goalId bất biến (đổi → VALIDATION_FAILED). */
    public record UpdateMilestone(long expectedVersion, Patch<UUID> goalId, Patch<String> title, Patch<String> dueOn,
            Patch<Long> position, Patch<Boolean> completed) {
        @Override
        public String toString() {
            return "GoalCommands.UpdateMilestone[expectedVersion=" + expectedVersion + "]";
        }
    }
}
