package com.lifeos.task.service;

import com.lifeos.common.util.Patch;
import java.util.UUID;

/**
 * Lệnh của use case Task (JAVA_SPRING_RULES §2.2). Giá trị giữ ở dạng chuỗi như client gửi (status, priority, dueDate
 * "yyyy-MM-dd", dueTime "HH:mm") để REST và sync (Giai đoạn 3) đi qua CÙNG bước kiểm tra ở service
 * (SECURITY_RULES §8.3). toString không in title/description (SECURITY_RULES §11.1).
 */
public final class TaskCommands {

    private TaskCommands() {
    }

    /**
     * Trường dạng {@link Patch} để payload sync sai kiểu được báo ở bước validation của service, sau bước kiểm id đã
     * dùng (D96). Vắng mặt = null (giá trị mặc định của REST).
     */
    public record Create(UUID id, Patch<String> title, Patch<String> description, Patch<String> status,
            Patch<String> priority, Patch<String> dueDate, Patch<String> dueTime, Patch<UUID> projectId,
            Patch<UUID> categoryId, Patch<UUID> goalId, Patch<UUID> milestoneId) {

        /** REST: giá trị đã qua Jackson (goalId/milestoneId thêm ở WP6, BACKEND_MASTER_PLAN §5 #2). */
        public Create(UUID id, String title, String description, String status, String priority, String dueDate,
                String dueTime, UUID projectId, UUID categoryId, UUID goalId, UUID milestoneId) {
            this(id, Patch.of(title), Patch.of(description), Patch.of(status), Patch.of(priority), Patch.of(dueDate),
                    Patch.of(dueTime), Patch.of(projectId), Patch.of(categoryId), Patch.of(goalId),
                    Patch.of(milestoneId));
        }

        @Override
        public String toString() {
            return "TaskCommands.Create[id=" + id + "]";
        }
    }

    /**
     * JSON Merge Patch: trường vắng = không đổi; null = xóa giá trị với trường cho phép null (description, dueDate,
     * dueTime, projectId, categoryId); title/status/priority không cho phép null.
     */
    public record Update(long expectedVersion, Patch<String> title, Patch<String> description, Patch<String> status,
            Patch<String> priority, Patch<String> dueDate, Patch<String> dueTime, Patch<UUID> projectId,
            Patch<UUID> categoryId, Patch<UUID> goalId, Patch<UUID> milestoneId) {
        @Override
        public String toString() {
            return "TaskCommands.Update[expectedVersion=" + expectedVersion + "]";
        }
    }
}
