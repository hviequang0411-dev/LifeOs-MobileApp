package com.lifeos.common.sync;

import java.util.Map;
import java.util.UUID;

/**
 * Một thay đổi do client gửi lên (SYSTEM_ARCHITECTURE.md §4.1). payload là state đầy đủ của entity (D19).
 * Dạng khởi đầu của skeleton; hoàn thiện khi viết module sync.
 */
public record Mutation(UUID mutationId, String entityType, UUID entityId, ChangeOperation operation,
        long baseVersion, Map<String, Object> payload) {
}
