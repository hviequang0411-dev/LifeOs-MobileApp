package com.lifeos.finance.service;

import com.lifeos.common.outbox.OutboxWriter;
import com.lifeos.finance.entity.RecurringTransaction;
import com.lifeos.finance.repository.RecurringTransactionRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Tạm dừng giao dịch định kỳ khi tài khoản của nó không còn ghi được (đã lưu trữ) — đợt sửa F6, thay cho việc bỏ lặng
 * lần xuất hiện. Dùng ở hai nơi, luôn BÊN TRONG transaction của nơi gọi và khi nơi gọi đang giữ khóa FOR UPDATE trên
 * bản ghi định kỳ: lúc lưu trữ tài khoản ({@link AccountCommandService}) và lưới an toàn trong job
 * ({@link RecurringRunService}). nextRunOn KHÔNG đổi; người dùng mở lại tài khoản rồi "tiếp tục" (quy tắc tiếp tục
 * hiện có: không bù lần bị lỡ khi đang tạm dừng).
 * <p>
 * Ghi outbox {@code RECURRING_PAUSED} theo schema tối thiểu D149 (không số tiền, không ghi chú). Nơi gọi phải gọi
 * ChangeRecorder cho bản ghi định kỳ SAU phương thức này (D86: ghi nghiệp vụ trước, ChangeRecorder cuối).
 */
@Component
class RecurringPauser {

    static final String EVENT_TYPE = "RECURRING_PAUSED";
    static final String TEMPLATE_KEY = "finance.recurring.paused";

    private final RecurringTransactionRepository recurring;
    private final OutboxWriter outbox;

    RecurringPauser(RecurringTransactionRepository recurring, OutboxWriter outbox) {
        this.recurring = recurring;
        this.outbox = outbox;
    }

    /** @return bản ghi đã lưu (version mới) — nơi gọi ghi change_logs UPDATE cho nó */
    RecurringTransaction pause(RecurringTransaction r) {
        r.setStatus("PAUSED");
        RecurringTransaction saved = recurring.saveAndFlush(r);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("recurringId", saved.getId().toString());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", saved.getUserId().toString());
        payload.put("category", "FINANCE");
        payload.put("templateKey", TEMPLATE_KEY);
        payload.put("variables", variables);
        outbox.write(EVENT_TYPE, RecurringTransaction.ENTITY_TYPE, saved.getId(), payload);
        return saved;
    }
}
