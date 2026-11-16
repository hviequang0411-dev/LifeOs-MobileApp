package com.lifeos.outbox;

import com.lifeos.common.outbox.OutboxWorker;
import com.lifeos.common.outbox.OutboxWriter;
import com.lifeos.identity.IdentityIntegrationTest;
import com.lifeos.support.RecordingEmailSender;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Hỗ trợ test outbox: worker gọi trực tiếp (scheduler tắt trong context test), stub email ghi lại lời gọi, ghi sự kiện
 * bằng OutboxWriter thật trong transaction. DB dùng chung: worker xử lý cả sự kiện tồn đọng của test khác, nên mọi
 * khẳng định chỉ nhắm vào sự kiện/địa chỉ của chính test.
 */
abstract class OutboxTestSupport extends IdentityIntegrationTest {

    @Autowired
    OutboxWorker worker;

    @Autowired
    OutboxWriter writer;

    @Autowired
    RecordingEmailSender mail;

    @Autowired
    PlatformTransactionManager transactionManager;

    /** Chạy worker cho đến khi không còn việc đến hạn (có giới hạn để không lặp vô hạn). */
    void drain() {
        for (int i = 0; i < 1_000 && worker.runOnce() > 0; i++) {
            // tiếp tục
        }
    }

    UUID write(String eventType, Map<String, Object> payload) {
        return new TransactionTemplate(transactionManager).execute(status ->
                writer.write(eventType, "user", UUID.randomUUID(), payload));
    }

    Map<String, Object> row(UUID eventId) {
        return jdbc.sql("SELECT status, attempts, next_attempt_at, locked_until, last_error, processed_at,"
                        + " payload::text AS payload FROM outbox_events WHERE id = :id")
                .param("id", eventId).query().singleRow();
    }

    UUID eventOf(UUID userId, String eventType) {
        return jdbc.sql("SELECT id FROM outbox_events WHERE aggregate_id = :u AND event_type = :t"
                        + " ORDER BY created_at DESC LIMIT 1")
                .param("u", userId).param("t", eventType).query(UUID.class).single();
    }
}
