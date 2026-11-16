package com.lifeos.sync.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Đọc change_logs và last_seq cho pull (SYSTEM_ARCHITECTURE §3.7, SYNC_PROTOCOL §6). Mọi truy vấn có user_id (SYN-20);
 * chỉ đọc, KHÔNG khóa user_sync_state (D89, SYN-43). Đọc theo khóa chính (user_id, seq).
 */
@Repository
public class ChangeLogReader {

    private static final String PAGE = """
            SELECT seq, entity_type, entity_id, operation, entity_version FROM change_logs
            WHERE user_id = :userId AND seq > :after
            ORDER BY seq
            LIMIT :limit
            """;

    private final JdbcClient jdbc;

    public ChangeLogReader(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Một dòng change_logs. */
    public record Row(long seq, String entityType, UUID entityId, String operation, long entityVersion) {
    }

    public List<Row> page(UUID userId, long afterSeq, int limit) {
        return jdbc.sql(PAGE).param("userId", userId).param("after", afterSeq).param("limit", limit)
                .query((rs, n) -> new Row(rs.getLong("seq"), rs.getString("entity_type"),
                        rs.getObject("entity_id", UUID.class), rs.getString("operation"),
                        rs.getLong("entity_version")))
                .list();
    }

    /** last_seq hiện tại (0 nếu user chưa có thay đổi nào). Đọc thường, không FOR UPDATE. */
    public long lastSeq(UUID userId) {
        return jdbc.sql("SELECT last_seq FROM user_sync_state WHERE user_id = :userId").param("userId", userId)
                .query(Long.class).optional().orElse(0L);
    }

    /** D150: cursor nhỏ hơn giá trị này đã bị retention dọn (0 nếu chưa có dòng trạng thái). */
    public long minRetainedSeq(UUID userId) {
        return jdbc.sql("SELECT min_retained_seq FROM user_sync_state WHERE user_id = :userId").param("userId", userId)
                .query(Long.class).optional().orElse(0L);
    }
}
