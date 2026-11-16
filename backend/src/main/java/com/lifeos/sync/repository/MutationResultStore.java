package com.lifeos.sync.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Bảng idempotency sync_mutations (DATABASE_DESIGN §4.2, SYNC_RULES §9.2, D22). JdbcClient theo D30. Không bao giờ lưu
 * DUPLICATE: cột status và result là kết quả GỐC (SYNC_PROTOCOL §2.2).
 */
@Repository
public class MutationResultStore {

    /** Tên ràng buộc khóa chính: vi phạm = request khác cùng mutationId đã ghi trước (đọc lại → DUPLICATE). */
    public static final String PK_CONSTRAINT = "pk_sync_mutations";

    private static final String FIND = """
            SELECT entity_type, entity_id, operation, base_version, status, result::text AS result
            FROM sync_mutations WHERE user_id = :userId AND mutation_id = :mutationId
            """;

    private static final String INSERT = """
            INSERT INTO sync_mutations (user_id, mutation_id, entity_type, entity_id, operation, base_version, status,
                                        result_version, error_code, result, created_at, device_id)
            VALUES (:userId, :mutationId, :entityType, :entityId, :operation, :baseVersion, :status,
                    :resultVersion, :errorCode, CAST(:result AS jsonb), :now, :deviceId)
            """;

    private final JdbcClient jdbc;

    public MutationResultStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Kết quả đã lưu của một mutation. */
    public record Stored(String entityType, UUID entityId, String operation, long baseVersion, String status,
            String resultJson) {

        @Override
        public String toString() {
            return "Stored[entityType=" + entityType + ", entityId=" + entityId + ", status=" + status + "]";
        }
    }

    /** Dòng cần ghi. */
    public record Row(UUID userId, UUID mutationId, String entityType, UUID entityId, String operation,
            long baseVersion, String status, Long resultVersion, String errorCode, String resultJson, UUID deviceId) {
    }

    public Optional<Stored> find(UUID userId, UUID mutationId) {
        return jdbc.sql(FIND).param("userId", userId).param("mutationId", mutationId)
                .query((rs, n) -> new Stored(rs.getString("entity_type"), rs.getObject("entity_id", UUID.class),
                        rs.getString("operation"), rs.getLong("base_version"), rs.getString("status"),
                        rs.getString("result")))
                .optional();
    }

    /** INSERT thường (APPLIED trong T1): trùng khóa chính ném DuplicateKeyException. */
    public void insert(Row row, Instant now) {
        bind(jdbc.sql(INSERT), row, now).update();
    }

    /** INSERT … ON CONFLICT DO NOTHING (CONFLICT/REJECTED trong T2). @return số dòng đã chèn (0 = đã có bản khác) */
    public int insertIfAbsent(Row row, Instant now) {
        return bind(jdbc.sql(INSERT + " ON CONFLICT (user_id, mutation_id) DO NOTHING"), row, now).update();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, Row row, Instant now) {
        return spec.param("userId", row.userId())
                .param("mutationId", row.mutationId())
                .param("entityType", row.entityType())
                .param("entityId", row.entityId())
                .param("operation", row.operation())
                .param("baseVersion", row.baseVersion())
                .param("status", row.status())
                .param("resultVersion", row.resultVersion(), java.sql.Types.BIGINT)
                .param("deviceId", row.deviceId(), java.sql.Types.OTHER)
                .param("errorCode", row.errorCode(), java.sql.Types.VARCHAR)
                .param("result", row.resultJson())
                .param("now", OffsetDateTime.ofInstant(now, ZoneOffset.UTC));
    }
}
