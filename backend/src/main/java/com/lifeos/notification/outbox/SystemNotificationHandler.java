package com.lifeos.notification.outbox;

import com.lifeos.common.outbox.OutboxEvent;
import com.lifeos.common.outbox.OutboxEventHandler;
import com.lifeos.common.outbox.OutboxPermanentFailure;
import com.lifeos.notification.domain.Templates;
import com.lifeos.notification.service.NotificationOrchestrator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Thông báo in-app loại SYSTEM theo schema D149 (PHASE_7 UC-INT-05: tệp xuất dữ liệu đã sẵn sàng). Chỉ nhận mẫu có trong
 * {@code SYSTEM.*.properties}; data chỉ giữ {@code type}, {@code id}. Dedupe {@code outbox:{eventId}}.
 */
@Component
class SystemNotificationHandler implements OutboxEventHandler {

    static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";

    private final NotificationOrchestrator orchestrator;

    SystemNotificationHandler(NotificationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public String eventType() {
        return "SYSTEM_NOTIFICATION";
    }

    @Override
    public void handle(OutboxEvent event) {
        Map<String, Object> payload = event.payload();
        UUID userId;
        try {
            userId = UUID.fromString(String.valueOf(payload.get("userId")));
        } catch (IllegalArgumentException e) {
            throw new OutboxPermanentFailure(INVALID_PAYLOAD);
        }
        if (!"SYSTEM".equals(payload.get("category")) || !(payload.get("templateKey") instanceof String key)
                || !Templates.exists("SYSTEM", key)) {
            throw new OutboxPermanentFailure(INVALID_PAYLOAD);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        if (payload.get("data") instanceof Map<?, ?> raw) {
            if (raw.get("type") instanceof String type) {
                data.put("type", type);
            }
            if (raw.get("id") instanceof String id) {
                data.put("id", id);
            }
        }
        orchestrator.create(new NotificationOrchestrator.Request(userId, "SYSTEM", key, Map.of(),
                "outbox:" + event.id(), data, false, null));
    }
}
