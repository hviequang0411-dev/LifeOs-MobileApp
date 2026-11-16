package com.lifeos.common.outbox;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ghi outbox_events trong CÙNG transaction với thay đổi nghiệp vụ (JAVA_SPRING_RULES §3.3, SYSTEM_ARCHITECTURE §3.11).
 * Worker xử lý ở Giai đoạn 4. Không dùng @Transactional ở đây (luật A10: chỉ ở ..service..), nên kiểm tra tường minh
 * rằng đang có transaction: gọi ngoài transaction là lỗi lập trình.
 * Payload có thể tạm chứa token thô (D37); worker ghi đè thành {} sau khi gửi.
 */
@Component
public class OutboxWriter {

    private static final String INSERT = """
            INSERT INTO outbox_events
                (id, event_type, aggregate_type, aggregate_id, user_id, payload, status, attempts, next_attempt_at,
                 created_at)
            VALUES (:id, :eventType, :aggregateType, :aggregateId, :userId, CAST(:payload AS jsonb), 'PENDING', 0, :now,
                    :now)
            """;
    private static final String AGGREGATE_USER = "user";

    private final JdbcClient jdbc;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    public OutboxWriter(JdbcClient jdbc, JsonMapper jsonMapper, Clock clock) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    /** @return id của sự kiện vừa ghi */
    public UUID write(String eventType, String aggregateType, UUID aggregateId, Map<String, Object> payload) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(payload, "payload");
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("OutboxWriter phải được gọi bên trong transaction nghiệp vụ");
        }
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        jdbc.sql(INSERT)
                .param("id", id)
                .param("eventType", eventType)
                .param("aggregateType", aggregateType)
                .param("aggregateId", aggregateId, java.sql.Types.OTHER)
                .param("userId", owner(aggregateType, aggregateId, payload), java.sql.Types.OTHER)
                .param("payload", jsonMapper.writeValueAsString(payload))
                .param("now", now)
                .update();
        return id;
    }

    /**
     * Người dùng sở hữu sự kiện, lưu ở cột {@code user_id} để xóa tài khoản xóa được mọi sự kiện của người đó kể cả
     * sau khi payload bị ghi đè (D205, đợt sửa F8): aggregate {@code user} → aggregateId; ngược lại {@code userId}
     * trong payload (schema D149) nếu là UUID hợp lệ; không suy ra được → NULL (sự kiện không thuộc người dùng nào).
     */
    private static UUID owner(String aggregateType, UUID aggregateId, Map<String, Object> payload) {
        if (AGGREGATE_USER.equals(aggregateType) && aggregateId != null) {
            return aggregateId;
        }
        if (payload.get("userId") instanceof String raw) {
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return payload.get("userId") instanceof UUID id ? id : null;
    }
}
