package com.lifeos.task.api;

import java.util.UUID;

/**
 * Sự kiện miền: Task vừa được xóa mềm và flush, TRƯỚC change_logs DELETE của Task (D80, D86). Listener (calendar: time
 * block; notification: reminder) xóa mềm dữ liệu con trong CÙNG transaction và thêm change_logs vào {@code changes}
 * (KHÔNG tự ghi — nhiều listener); TaskCommandService ghi tất cả sau cùng, mutation_id NULL (D86).
 */
public record TaskDeletionStarted(UUID userId, UUID taskId, com.lifeos.common.sync.PendingChanges changes) {
}
