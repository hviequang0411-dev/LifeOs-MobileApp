package com.lifeos.common.outbox;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Truy cập outbox_events cho worker và job dọn (DATABASE_DESIGN §4.4, D30: JdbcClient, SKIP LOCKED). Mỗi phương thức
 * là MỘT câu lệnh chạy autocommit — tức một transaction ngắn riêng (SYSTEM_ARCHITECTURE §3.11 bước 1 và 3); không có
 * lời gọi mạng nào nằm giữa chúng. Kết thúc (SENT/FAILED) luôn ghi đè payload thành {} (D37, LOG-02).
 */
@Component
public class OutboxStore {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<>() {
    };

    /** Bước 4 (§3.11): PROCESSING quá lease (worker chết giữa chừng) → tính là một lần thử thất bại. */
    private static final String RECLAIM = """
            UPDATE outbox_events SET
                attempts        = attempts + 1,
                status          = CASE WHEN attempts + 1 >= :max THEN 'FAILED' ELSE 'PENDING' END,
                payload         = CASE WHEN attempts + 1 >= :max THEN '{}'::jsonb ELSE payload END,
                processed_at    = CASE WHEN attempts + 1 >= :max THEN :now ELSE NULL END,
                next_attempt_at = :now,
                locked_until    = NULL,
                last_error      = 'LEASE_EXPIRED'
            WHERE status = 'PROCESSING' AND locked_until < :now
            """;

    /** Bước 1: nhận việc đến hạn; SKIP LOCKED để nhiều worker/instance không nhận trùng (JAVA_SPRING_RULES §10.4). */
    private static final String CLAIM = """
            UPDATE outbox_events SET status = 'PROCESSING', locked_until = :lockedUntil
            WHERE id IN (SELECT id FROM outbox_events
                         WHERE status = 'PENDING' AND next_attempt_at <= :now
                         ORDER BY created_at
                         LIMIT :limit
                         FOR UPDATE SKIP LOCKED)
            RETURNING id, event_type, aggregate_type, aggregate_id, payload::text AS payload, attempts
            """;

    /** Chỉ kết thúc khi worker này còn giữ lease (chống ghi đè sau khi lease đã bị thu hồi). */
    private static final String OWNED = " WHERE id = :id AND status = 'PROCESSING' AND locked_until = :lockedUntil";

    private static final String SENT = """
            UPDATE outbox_events SET status = 'SENT', processed_at = :now, payload = '{}'::jsonb,
                locked_until = NULL, last_error = NULL""" + OWNED;

    private static final String RETRY = """
            UPDATE outbox_events SET status = 'PENDING', attempts = attempts + 1, next_attempt_at = :next,
                locked_until = NULL, last_error = :error""" + OWNED;

    private static final String FAILED = """
            UPDATE outbox_events SET status = 'FAILED', attempts = attempts + 1, processed_at = :now,
                payload = '{}'::jsonb, locked_until = NULL, last_error = :error""" + OWNED;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    public OutboxStore(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /** Sự kiện đã nhận cùng thời điểm hết lease mà worker phải trình ra khi kết thúc. */
    public record Claimed(OutboxEvent event, OffsetDateTime lockedUntil) {
    }

    public int reclaimExpired(Instant now, int maxAttempts) {
        return jdbc.sql(RECLAIM).param("now", at(now)).param("max", maxAttempts).update();
    }

    public List<Claimed> claim(Instant now, Instant lockedUntil, int limit) {
        OffsetDateTime lease = at(lockedUntil);
        return jdbc.sql(CLAIM).param("now", at(now)).param("lockedUntil", lease).param("limit", limit)
                .query((rs, n) -> new Claimed(new OutboxEvent(rs.getObject("id", UUID.class),
                        rs.getString("event_type"), rs.getString("aggregate_type"),
                        rs.getObject("aggregate_id", UUID.class), json.readValue(rs.getString("payload"), MAP),
                        rs.getInt("attempts")), lease))
                .list();
    }

    /** @return true nếu worker còn giữ lease và đã ghi kết quả */
    public boolean markSent(Claimed claimed, Instant now) {
        return owned(jdbc.sql(SENT).param("now", at(now)), claimed).update() == 1;
    }

    public boolean markRetry(Claimed claimed, Instant nextAttemptAt, String error) {
        return owned(jdbc.sql(RETRY).param("next", at(nextAttemptAt)).param("error", error), claimed).update() == 1;
    }

    public boolean markFailed(Claimed claimed, Instant now, String error) {
        return owned(jdbc.sql(FAILED).param("now", at(now)).param("error", error), claimed).update() == 1;
    }

    /** Job dọn (§3.12, DATABASE_DESIGN §7): xóa SENT/FAILED đã hoàn tất trước {@code cutoff}. */
    public int deleteFinishedBefore(Instant cutoff) {
        return jdbc.sql("DELETE FROM outbox_events WHERE status IN ('SENT', 'FAILED') AND processed_at < :cutoff")
                .param("cutoff", at(cutoff)).update();
    }

    /** Kích thước hàng đợi (metric, SYSTEM_ARCHITECTURE §3.13). */
    public long countPending() {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE status = 'PENDING'").query(Long.class).single();
    }

    private static JdbcClient.StatementSpec owned(JdbcClient.StatementSpec spec, Claimed claimed) {
        return spec.param("id", claimed.event().id()).param("lockedUntil", claimed.lockedUntil());
    }

    /** timestamptz lưu tới micro giây: cắt trước khi ghi để so khớp locked_until chính xác. */
    private static OffsetDateTime at(Instant instant) {
        return OffsetDateTime.ofInstant(instant.truncatedTo(java.time.temporal.ChronoUnit.MICROS), ZoneOffset.UTC);
    }
}
