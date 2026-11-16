package com.lifeos.sync.repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * SQL của retention và kiểm tra nhất quán (PHASE_5 D153, D154, D156, D172). Mọi giá trị là tham số; tên bảng chỉ đến từ
 * hằng số của SyncEntityHandler.table() và được kiểm dạng {@code [a-z_]+}.
 */
@Repository
public class RetentionStore {

    private static final Pattern IDENT = Pattern.compile("^[a-z_]+$");

    private final JdbcClient jdbc;

    public RetentionStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    static String ident(String name) {
        if (name == null || !IDENT.matcher(name).matches()) {
            throw new IllegalArgumentException("Tên bảng/cột không hợp lệ");
        }
        return name;
    }

    /** D154: một lô sync_mutations quá hạn. @return số dòng đã xóa */
    public int deleteMutationsBefore(Instant cutoff, int batch) {
        return jdbc.sql("""
                DELETE FROM sync_mutations WHERE ctid IN (
                    SELECT ctid FROM sync_mutations WHERE created_at < :cutoff LIMIT :batch)
                """).param("cutoff", utc(cutoff)).param("batch", batch).update();
    }

    /** Người dùng có change_logs đủ điều kiện dọn (cũ hơn cutoff). */
    public List<UUID> usersWithOldChangeLogs(Instant cutoff, int limit) {
        return jdbc.sql("SELECT DISTINCT user_id FROM change_logs WHERE created_at < :cutoff LIMIT :limit")
                .param("cutoff", utc(cutoff)).param("limit", limit).query(UUID.class).list();
    }

    /** Khóa dòng trạng thái của user (FOR UPDATE) — tuần tự với ChangeRecorder; trả last_seq. */
    public Optional<Long> lockState(UUID userId) {
        return jdbc.sql("SELECT last_seq FROM user_sync_state WHERE user_id = :u FOR UPDATE").param("u", userId)
                .query(Long.class).optional();
    }

    /**
     * D153: xóa một lô change_logs của user có seq ≤ maxSeq (giữ ≥ N seq gần nhất) và cũ hơn cutoff.
     *
     * @return (số dòng đã xóa, seq lớn nhất đã xóa); count = 0 nếu không xóa gì
     */
    public Deleted deleteChangeLogs(UUID userId, long maxSeq, Instant cutoff, int batch) {
        return jdbc.sql("""
                WITH gone AS (
                    DELETE FROM change_logs WHERE user_id = :u AND seq IN (
                        SELECT seq FROM change_logs WHERE user_id = :u AND seq <= :maxSeq AND created_at < :cutoff
                        ORDER BY seq LIMIT :batch)
                    RETURNING seq)
                SELECT count(*) AS n, coalesce(max(seq), 0) AS max_seq FROM gone
                """).param("u", userId).param("maxSeq", maxSeq).param("cutoff", utc(cutoff)).param("batch", batch)
                .query((rs, i) -> new Deleted(rs.getInt("n"), rs.getLong("max_seq"))).single();
    }

    public record Deleted(int count, long maxSeq) {
    }

    public void raiseMinRetained(UUID userId, long seq) {
        jdbc.sql("UPDATE user_sync_state SET min_retained_seq = GREATEST(min_retained_seq, :seq) WHERE user_id = :u")
                .param("seq", seq).param("u", userId).update();
    }

    /**
     * D156: xóa cứng một lô tombstone của {@code table} — deleted_at cũ hơn cutoff, không còn change_logs nào của
     * entity (mọi lần thay đổi đã bị dọn ⇒ seq của lần xóa &lt; min_retained_seq), và không còn dòng con tham chiếu
     * (kể cả tombstone con) theo {@code childRefs} dạng "bảng_con.cột".
     */
    public int deleteTombstones(String table, String entityType, List<String> childRefs, Instant cutoff, int batch) {
        StringBuilder where = new StringBuilder("t.deleted_at < :cutoff AND NOT EXISTS (SELECT 1 FROM change_logs c"
                + " WHERE c.user_id = t.user_id AND c.entity_type = :type AND c.entity_id = t.id)");
        for (String ref : childRefs) {
            String[] parts = ref.split("\\.");
            where.append(" AND NOT EXISTS (SELECT 1 FROM ").append(ident(parts[0])).append(" x WHERE x.")
                    .append(ident(parts[1])).append(" = t.id)");
        }
        if ("financial_transactions".equals(table)) {
            // §4 kế hoạch (PHASE_5 D156, [OPEN] 3): giao dịch sinh từ định kỳ giữ tombstone khi dòng định kỳ còn tồn tại.
            where.append(" AND (t.recurring_transaction_id IS NULL OR NOT EXISTS (SELECT 1 FROM recurring_transactions r"
                    + " WHERE r.id = t.recurring_transaction_id))");
        }
        String t = ident(table);
        return jdbc.sql("DELETE FROM " + t + " WHERE id IN (SELECT t.id FROM " + t + " t WHERE " + where
                        + " LIMIT :batch)")
                .param("cutoff", utc(cutoff)).param("type", entityType).param("batch", batch).update();
    }

    /** Dòng phụ không đồng bộ của ngân sách sắp bị xóa cứng (budget_alerts). */
    public int deleteAlertsOfPurgeableBudgets(Instant cutoff) {
        return jdbc.sql("""
                DELETE FROM budget_alerts a USING budgets b
                WHERE a.budget_id = b.id AND b.deleted_at < :cutoff
                  AND NOT EXISTS (SELECT 1 FROM change_logs c WHERE c.user_id = b.user_id AND c.entity_type = 'budget'
                                  AND c.entity_id = b.id)
                """).param("cutoff", utc(cutoff)).update();
    }

    /** D170: báo cáo chẩn đoán quá 30 ngày. */
    public int deleteDiagnosticsBefore(Instant cutoff) {
        return jdbc.sql("DELETE FROM sync_diagnostics WHERE created_at < :cutoff").param("cutoff", utc(cutoff))
                .update();
    }

    // ---------------------------------------------------------------- D172 kiểm tra nhất quán (chỉ đọc)

    /** (a) last_seq nhỏ hơn seq lớn nhất của change_logs. */
    public long usersBehindLog() {
        return jdbc.sql("""
                SELECT count(*) FROM user_sync_state s
                WHERE s.last_seq < (SELECT coalesce(max(c.seq), 0) FROM change_logs c WHERE c.user_id = s.user_id)
                """).query(Long.class).single();
    }

    /** (b) entity còn sống có change_logs nhưng version khác version của dòng change_logs mới nhất. */
    public long versionMismatches(String table, String entityType) {
        String t = ident(table);
        return jdbc.sql("SELECT count(*) FROM " + t + " t WHERE t.deleted_at IS NULL AND EXISTS (SELECT 1 FROM"
                        + " change_logs c WHERE c.user_id = t.user_id AND c.entity_type = :type AND c.entity_id = t.id)"
                        + " AND t.version <> (SELECT c.entity_version FROM change_logs c WHERE c.user_id = t.user_id"
                        + " AND c.entity_type = :type AND c.entity_id = t.id ORDER BY c.seq DESC LIMIT 1)")
                .param("type", entityType).query(Long.class).single();
    }

    /** (c) change_logs gần đây (trong hạn giữ sync_mutations) có mutation_id không tồn tại. */
    public long orphanMutationIds(Instant since) {
        return jdbc.sql("""
                SELECT count(*) FROM change_logs c
                WHERE c.mutation_id IS NOT NULL AND c.created_at >= :since
                  AND NOT EXISTS (SELECT 1 FROM sync_mutations m WHERE m.user_id = c.user_id
                                  AND m.mutation_id = c.mutation_id)
                """).param("since", utc(since)).query(Long.class).single();
    }

    /** Snapshot (D152): id còn sống của một bảng, theo id tăng dần sau {@code afterId}. */
    public List<UUID> liveIds(String table, UUID userId, UUID afterId, int limit) {
        return jdbc.sql("SELECT id FROM " + ident(table) + " WHERE user_id = :u AND deleted_at IS NULL"
                        + " AND (CAST(:after AS uuid) IS NULL OR id > :after) ORDER BY id LIMIT :limit")
                .param("u", userId).param("after", afterId, java.sql.Types.OTHER).param("limit", limit)
                .query(UUID.class).list();
    }

    public long lastSeq(UUID userId) {
        return jdbc.sql("SELECT last_seq FROM user_sync_state WHERE user_id = :u").param("u", userId)
                .query(Long.class).optional().orElse(0L);
    }

    public void insertDiagnostics(UUID id, UUID userId, String payload, Instant now) {
        jdbc.sql("INSERT INTO sync_diagnostics (id, user_id, payload, created_at) VALUES (:id, :u, CAST(:p AS jsonb),"
                + " :now)").param("id", id).param("u", userId).param("p", payload).param("now", utc(now)).update();
    }
}
