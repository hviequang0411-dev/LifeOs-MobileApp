package com.lifeos.project.api;

import java.util.UUID;

/**
 * Sự kiện miền đồng bộ (D80, JAVA_SPRING_RULES §4.5): phát trong transaction xóa Project, SAU khi đã khóa dòng
 * Project (FOR UPDATE), đánh dấu xóa mềm và flush (D86: mọi khóa/ghi nghiệp vụ trước ChangeRecorder.record), và TRƯỚC
 * khi ghi change_logs DELETE của Project. Listener của module task gỡ project_id khỏi các Task còn sống trong CÙNG
 * transaction (SYSTEM_ARCHITECTURE §3.8), nên change_logs của Task đứng trước change_logs xóa Project. Dòng
 * change_logs của Task bị cascade luôn có mutation_id = NULL (SYNC_PROTOCOL §3.2), nên sự kiện không mang mutationId.
 */
public record ProjectDeletionStarted(UUID userId, UUID projectId) {
}
