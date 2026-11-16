package com.lifeos.identity.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** SQL của xuất dữ liệu và xóa tài khoản (PHASE_7 D204, D205). Mọi câu lệnh theo user_id; tham số hóa. */
@Repository
public class AccountStore {

    public record Export(UUID id, UUID userId, String status, Instant requestedAt, Instant startedAt,
            Instant finishedAt, Instant expiresAt, String fileRef, Long sizeBytes, String errorCode,
            int downloadCount) {
    }

    public record Code(UUID id, String codeHash, Instant expiresAt) {
    }

    private static final String EXPORT_COLUMNS = "id, user_id, status, requested_at, started_at, finished_at,"
            + " expires_at, file_ref, size_bytes, error_code, download_count";

    private final JdbcClient jdbc;

    public AccountStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- deletion

    public Optional<Instant> deletionScheduledAt(UUID userId) {
        return jdbc.sql("SELECT deletion_scheduled_at FROM users WHERE id = :u").param("u", userId)
                .query((rs, n) -> instant(rs.getTimestamp(1))).optional().filter(java.util.Objects::nonNull);
    }

    public int schedule(UUID userId, Instant at) {
        return jdbc.sql("UPDATE users SET deletion_scheduled_at = :at WHERE id = :u AND deletion_scheduled_at IS NULL")
                .param("at", utc(at)).param("u", userId).update();
    }

    public int cancel(UUID userId) {
        return jdbc.sql("UPDATE users SET deletion_scheduled_at = NULL WHERE id = :u"
                + " AND deletion_scheduled_at IS NOT NULL").param("u", userId).update();
    }

    public List<UUID> dueForDeletion(Instant now, int limit) {
        return jdbc.sql("SELECT id FROM users WHERE deletion_scheduled_at <= :now ORDER BY deletion_scheduled_at"
                + " LIMIT :limit").param("now", utc(now)).param("limit", limit).query(UUID.class).list();
    }

    public void invalidateCodes(UUID userId, Instant now) {
        jdbc.sql("UPDATE account_deletion_codes SET used_at = :now WHERE user_id = :u AND used_at IS NULL")
                .param("now", utc(now)).param("u", userId).update();
    }

    public void insertCode(UUID userId, String codeHash, Instant expiresAt) {
        jdbc.sql("INSERT INTO account_deletion_codes (id, user_id, code_hash, expires_at) VALUES (:id, :u, :h, :e)")
                .param("id", UUID.randomUUID()).param("u", userId).param("h", codeHash).param("e", utc(expiresAt))
                .update();
    }

    public Optional<Code> activeCode(UUID userId, Instant now) {
        return jdbc.sql("SELECT id, code_hash, expires_at FROM account_deletion_codes WHERE user_id = :u"
                        + " AND used_at IS NULL AND expires_at > :now ORDER BY created_at DESC LIMIT 1")
                .param("u", userId).param("now", utc(now))
                .query((rs, n) -> new Code(rs.getObject(1, UUID.class), rs.getString(2), instant(rs.getTimestamp(3))))
                .optional();
    }

    public int consumeCode(UUID codeId, Instant now) {
        return jdbc.sql("UPDATE account_deletion_codes SET used_at = :now WHERE id = :id AND used_at IS NULL")
                .param("now", utc(now)).param("id", codeId).update();
    }

    public void audit(String userHash, Instant at, long rows) {
        jdbc.sql("INSERT INTO account_deletion_audit (user_hash, deleted_at, table_rows) VALUES (:h, :at, :r)")
                .param("h", userHash).param("at", utc(at)).param("r", (int) Math.min(rows, Integer.MAX_VALUE))
                .update();
    }

    public int deleteAuditBefore(Instant cutoff) {
        return jdbc.sql("DELETE FROM account_deletion_audit WHERE deleted_at < :c").param("c", utc(cutoff)).update();
    }

    // ---------------------------------------------------------------- exports

    /**
     * Tuần tự hóa các yêu cầu xuất của MỘT người dùng (đợt sửa F5): khóa dòng users tới khi commit, để "kiểm đang có
     * yêu cầu / hạn mức 2 lần/24 giờ rồi chèn" là nguyên tử. FOR NO KEY UPDATE: không chặn các INSERT có khóa ngoại
     * tới users của request khác (chúng chỉ cần FOR KEY SHARE).
     *
     * @return false nếu người dùng không còn tồn tại
     */
    public boolean lockUserForExport(UUID userId) {
        return jdbc.sql("SELECT id FROM users WHERE id = :u FOR NO KEY UPDATE").param("u", userId)
                .query(UUID.class).optional().isPresent();
    }

    public void insertExport(UUID id, UUID userId, Instant now) {
        jdbc.sql("INSERT INTO data_exports (id, user_id, status, requested_at) VALUES (:id, :u, 'PENDING', :now)")
                .param("id", id).param("u", userId).param("now", utc(now)).update();
    }

    public int countRequestedSince(UUID userId, Instant since) {
        return jdbc.sql("SELECT count(*) FROM data_exports WHERE user_id = :u AND requested_at > :s")
                .param("u", userId).param("s", utc(since)).query(Integer.class).single();
    }

    public Optional<Export> activeExport(UUID userId) {
        return jdbc.sql("SELECT " + EXPORT_COLUMNS + " FROM data_exports WHERE user_id = :u"
                + " AND status IN ('PENDING', 'RUNNING') ORDER BY requested_at LIMIT 1").param("u", userId)
                .query((rs, n) -> export(rs)).optional();
    }

    public Optional<Export> findExport(UUID userId, UUID id) {
        return jdbc.sql("SELECT " + EXPORT_COLUMNS + " FROM data_exports WHERE id = :id AND user_id = :u")
                .param("id", id).param("u", userId).query((rs, n) -> export(rs)).optional();
    }

    /** Giành một yêu cầu PENDING (SKIP LOCKED: nhiều instance không trùng việc). */
    public Optional<Export> claimPending(Instant now) {
        return jdbc.sql("UPDATE data_exports SET status = 'RUNNING', started_at = :now WHERE id = ("
                        + "SELECT id FROM data_exports WHERE status = 'PENDING' ORDER BY requested_at LIMIT 1"
                        + " FOR UPDATE SKIP LOCKED) RETURNING " + EXPORT_COLUMNS)
                .param("now", utc(now)).query((rs, n) -> export(rs)).optional();
    }

    public void markReady(UUID id, Instant now, Instant expiresAt, String fileRef, long size) {
        jdbc.sql("UPDATE data_exports SET status = 'READY', finished_at = :now, expires_at = :e, file_ref = :f,"
                        + " size_bytes = :s WHERE id = :id").param("now", utc(now)).param("e", utc(expiresAt))
                .param("f", fileRef).param("s", size).param("id", id).update();
    }

    public void markFailed(UUID id, Instant now, String errorCode) {
        jdbc.sql("UPDATE data_exports SET status = 'FAILED', finished_at = :now, error_code = :c WHERE id = :id")
                .param("now", utc(now)).param("c", errorCode).param("id", id).update();
    }

    /** RUNNING quá hạn (instance chết giữa chừng) → FAILED STALE. */
    public int failStale(Instant cutoff, Instant now) {
        return jdbc.sql("UPDATE data_exports SET status = 'FAILED', finished_at = :now, error_code = 'STALE'"
                + " WHERE status = 'RUNNING' AND started_at < :c").param("now", utc(now)).param("c", utc(cutoff))
                .update();
    }

    public int countDownload(UUID id, int max) {
        return jdbc.sql("UPDATE data_exports SET download_count = download_count + 1 WHERE id = :id"
                + " AND download_count < :max").param("id", id).param("max", max).update();
    }

    public List<Export> expiredReady(Instant now, int limit) {
        return jdbc.sql("SELECT " + EXPORT_COLUMNS + " FROM data_exports WHERE status = 'READY' AND expires_at <= :now"
                + " LIMIT :l").param("now", utc(now)).param("l", limit).query((rs, n) -> export(rs)).list();
    }

    public List<String> fileRefsOf(UUID userId) {
        return jdbc.sql("SELECT file_ref FROM data_exports WHERE user_id = :u AND file_ref IS NOT NULL")
                .param("u", userId).query(String.class).list();
    }

    public void markExpired(UUID id) {
        jdbc.sql("UPDATE data_exports SET status = 'EXPIRED', file_ref = NULL WHERE id = :id").param("id", id)
                .update();
    }

    private static Export export(java.sql.ResultSet rs) throws java.sql.SQLException {
        long size = rs.getLong("size_bytes");
        return new Export(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class), rs.getString("status"),
                instant(rs.getTimestamp("requested_at")), instant(rs.getTimestamp("started_at")),
                instant(rs.getTimestamp("finished_at")), instant(rs.getTimestamp("expires_at")),
                rs.getString("file_ref"), rs.wasNull() ? null : size, rs.getString("error_code"),
                rs.getInt("download_count"));
    }

    private static Instant instant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
