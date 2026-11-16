package com.lifeos.common.sync;

import java.util.UUID;

/**
 * Hợp đồng ghi nhật ký thay đổi (SYSTEM_ARCHITECTURE.md §3.3). Mọi *CommandService của entity đồng bộ
 * phải gọi record(...) ở cuối mỗi thao tác ghi, trong CÙNG transaction với thay đổi nghiệp vụ (luật ArchUnit A9).
 * Cài đặt nằm ở module sync; module nghiệp vụ chỉ biết interface này.
 */
public interface ChangeRecorder {

    /**
     * @param mutationIdOrNull mutationId nếu thay đổi đến từ sync; null nếu từ REST hoặc cascade
     * @return seq vừa được cấp cho user
     */
    long record(UUID userId, String entityType, UUID entityId, ChangeOperation operation, long entityVersion,
            UUID mutationIdOrNull);
}
