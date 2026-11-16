package com.lifeos.goal.api;

import java.util.UUID;

/**
 * Sự kiện miền: Goal vừa xóa mềm + flush (D186). Listener (note: note_links tới goal) xóa mềm dữ liệu con trong CÙNG
 * transaction và thêm change_logs vào {@code changes}; module goal ghi tất cả sau cùng, mutation_id NULL (D86).
 */
public record GoalDeletionStarted(UUID userId, UUID goalId, com.lifeos.common.sync.PendingChanges changes) {
}
