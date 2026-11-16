package com.lifeos.sync.repository;

import com.lifeos.common.sync.ChangeOperation;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Ghi change_logs (DATABASE_DESIGN §4.2). JdbcClient theo D30. */
@Repository
public class ChangeLogRepository {

    private static final String INSERT = """
            INSERT INTO change_logs
                (user_id, seq, entity_type, entity_id, operation, entity_version, mutation_id, created_at)
            VALUES (:userId, :seq, :entityType, :entityId, :operation, :entityVersion, :mutationId, :now)
            """;

    private final JdbcClient jdbc;

    public ChangeLogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID userId, long seq, String entityType, UUID entityId, ChangeOperation operation,
            long entityVersion, UUID mutationIdOrNull, Instant now) {
        jdbc.sql(INSERT)
                .param("userId", userId)
                .param("seq", seq)
                .param("entityType", entityType)
                .param("entityId", entityId)
                .param("operation", operation.name())
                .param("entityVersion", entityVersion)
                .param("mutationId", mutationIdOrNull, java.sql.Types.OTHER)
                .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                .update();
    }
}
