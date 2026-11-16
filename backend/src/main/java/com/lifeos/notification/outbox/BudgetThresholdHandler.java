package com.lifeos.notification.outbox;

import com.lifeos.common.outbox.OutboxEvent;
import com.lifeos.common.outbox.OutboxEventHandler;
import com.lifeos.common.outbox.OutboxPermanentFailure;
import com.lifeos.notification.domain.NotificationRules;
import com.lifeos.notification.domain.Templates;
import com.lifeos.notification.service.NotificationOrchestrator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Sự kiện từ module khác theo schema tối thiểu D149 ({@code userId, category, templateKey, variables}) — ở WP4:
 * {@code BUDGET_THRESHOLD} do finance ghi (D116). Biến chỉ lấy theo whitelist của {@link Templates}; payload hỏng →
 * FAILED ngay (INVALID_PAYLOAD), không chặn hàng đợi. Idempotent nhờ dedupe {@code outbox:{eventId}} (D122). Chạy NGOÀI
 * transaction; Orchestrator mở transaction riêng.
 */
@Component
class BudgetThresholdHandler implements OutboxEventHandler {

    static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";

    private final NotificationOrchestrator orchestrator;

    BudgetThresholdHandler(NotificationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public String eventType() {
        return "BUDGET_THRESHOLD";
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
        Object category = payload.get("category");
        Object templateKey = payload.get("templateKey");
        if (!(category instanceof String c) || !NotificationRules.CATEGORIES.contains(c)
                || !(templateKey instanceof String key) || !Templates.exists(c, key)
                || !(payload.get("variables") instanceof Map<?, ?> rawVariables)) {
            throw new OutboxPermanentFailure(INVALID_PAYLOAD);
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        rawVariables.forEach((k, v) -> {
            if (Templates.WHITELIST.contains(String.valueOf(k))) {
                variables.put(String.valueOf(k), v);
            }
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "budget");
        if (rawVariables.get("budgetId") instanceof String budgetId) {
            data.put("id", budgetId);
        }
        orchestrator.create(new NotificationOrchestrator.Request(userId, c, key, variables, "outbox:" + event.id(),
                data, false, null));
    }
}
