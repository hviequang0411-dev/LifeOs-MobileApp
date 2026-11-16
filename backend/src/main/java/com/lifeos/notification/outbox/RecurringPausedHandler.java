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
 * {@code RECURRING_PAUSED} do finance ghi khi một giao dịch định kỳ bị tạm dừng vì tài khoản đã lưu trữ (đợt sửa F6),
 * theo schema tối thiểu D149 ({@code userId, category, templateKey, variables}). Nội dung từ mẫu FINANCE, không số
 * tiền/ghi chú (D138); data chỉ có deep link {@code type=recurring, id}. Payload hỏng → FAILED ngay. Idempotent nhờ
 * dedupe {@code outbox:{eventId}} (D122). Chạy NGOÀI transaction; Orchestrator mở transaction riêng.
 */
@Component
class RecurringPausedHandler implements OutboxEventHandler {

    static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";
    private static final String CATEGORY = "FINANCE";

    private final NotificationOrchestrator orchestrator;

    RecurringPausedHandler(NotificationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public String eventType() {
        return "RECURRING_PAUSED";
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
        if (!CATEGORY.equals(payload.get("category")) || !(payload.get("templateKey") instanceof String key)
                || !Templates.exists(CATEGORY, key)) {
            throw new OutboxPermanentFailure(INVALID_PAYLOAD);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "recurring");
        if (payload.get("variables") instanceof Map<?, ?> variables
                && variables.get("recurringId") instanceof String recurringId) {
            data.put("id", recurringId);
        }
        orchestrator.create(new NotificationOrchestrator.Request(userId, CATEGORY, key, Map.of(),
                "outbox:" + event.id(), data, false, null));
    }
}
