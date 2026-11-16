package com.lifeos.task.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * POST /tasks { id, title, description?, status?, priority?, dueDate?, dueTime?, projectId?, categoryId? }
 * (API_CONVENTIONS §8.4). Giá trị kiểm ở service (dùng chung với sync). completedAt, version, userId, createdAt,
 * updatedAt do server — client gửi sẽ bị bỏ qua (SECURITY_RULES §7.5).
 */
public record CreateTaskRequest(@NotNull UUID id, @NotNull String title, String description, String status,
        String priority, String dueDate, String dueTime, UUID projectId, UUID categoryId, UUID goalId,
        UUID milestoneId) {

    @Override
    public String toString() {
        return "CreateTaskRequest[id=" + id + "]";
    }
}
