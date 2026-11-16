package com.lifeos.sync.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bộ đếm seq theo user (SYNC_RULES §9.3, SYSTEM_ARCHITECTURE §3.6). JdbcClient theo D30. */
@Repository
public class UserSyncStateRepository {

    /**
     * Upsert duy nhất: tạo dòng khi chưa có (D21) hoặc tăng last_seq. Dòng bị khóa đến khi transaction commit,
     * nên ghi của một user được tuần tự và seq liên tục theo thứ tự commit; rollback hoàn tác luôn phần tăng.
     */
    private static final String NEXT_SEQ = """
            INSERT INTO user_sync_state (user_id, last_seq, updated_at)
            VALUES (:userId, 1, :now)
            ON CONFLICT (user_id)
            DO UPDATE SET last_seq = user_sync_state.last_seq + 1, updated_at = :now
            RETURNING last_seq
            """;

    private final JdbcClient jdbc;

    public UserSyncStateRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long nextSeq(UUID userId, Instant now) {
        return jdbc.sql(NEXT_SEQ)
                .param("userId", userId)
                .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .query(Long.class)
                .single();
    }
}
