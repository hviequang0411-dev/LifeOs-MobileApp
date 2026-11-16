package com.lifeos.sync.dto;

import tools.jackson.databind.JsonNode;

/**
 * Thân POST /sync/push. mutations giữ dạng cây JSON để SyncPushRequestParser kiểm cấu trúc và báo đúng vị trí lỗi
 * (SYNC_PROTOCOL §2.1, D93). toString KHÔNG in payload (SECURITY_RULES §11.1, SYN-47): Spring MVC ghi DEBUG thân
 * request qua toString.
 */
public record PushRequest(JsonNode mutations) {

    @Override
    public String toString() {
        return "PushRequest[mutations=" + (mutations == null ? "null" : mutations.size()) + "]";
    }
}
