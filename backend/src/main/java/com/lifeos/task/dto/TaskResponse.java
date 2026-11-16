package com.lifeos.task.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tài nguyên Task (API_CONVENTIONS §8.4). dueTime dạng "HH:mm" (giờ địa phương, §2). toString không in
 * title/description (SECURITY_RULES §11.1).
 */
public record TaskResponse(UUID id, String title, String description, String status, String priority,
        LocalDate dueDate, String dueTime, Instant completedAt, UUID projectId, UUID categoryId, UUID goalId,
        UUID milestoneId, long version,
        Instant createdAt, Instant updatedAt) {

    @Override
    public String toString() {
        return "TaskResponse[id=" + id + ", version=" + version + "]";
    }
}
