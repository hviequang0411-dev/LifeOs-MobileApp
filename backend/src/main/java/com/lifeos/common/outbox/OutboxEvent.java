package com.lifeos.common.outbox;

import java.util.Map;
import java.util.UUID;

/**
 * Một sự kiện outbox đã được worker nhận (SYSTEM_ARCHITECTURE §3.11). payload có thể chứa token thô và email (D37):
 * toString KHÔNG in payload (SECURITY_RULES §11.1).
 *
 * @param attempts số lần đã thử TRƯỚC lần này
 */
public record OutboxEvent(UUID id, String eventType, String aggregateType, UUID aggregateId,
        Map<String, Object> payload, int attempts) {

    @Override
    public String toString() {
        return "OutboxEvent[id=" + id + ", eventType=" + eventType + ", attempts=" + attempts + "]";
    }
}
