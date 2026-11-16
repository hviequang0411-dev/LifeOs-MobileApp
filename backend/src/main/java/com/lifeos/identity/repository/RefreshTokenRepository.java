package com.lifeos.identity.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * refresh_tokens bằng JdbcClient (ngoại lệ của D30 đã duyệt ở Q2 Giai đoạn 1): xoay vòng nguyên tử D72 cần
 * {@code UPDATE … WHERE used_at IS NULL … RETURNING}. Chỉ lưu SHA-256 hex của token (SECURITY_RULES §4.2). Mọi thời
 * điểm lấy từ Clock của ứng dụng (tham số), không dùng now() của DB.
 */
@Repository
public class RefreshTokenRepository {

    /**
     * Đánh dấu đã dùng MỘT cách nguyên tử, chỉ khi token còn hiệu lực: chưa dùng, chưa thu hồi, chưa hết hạn, thiết bị
     * chưa thu hồi, user còn ACTIVE (SECURITY_RULES §4.2). Hai request đồng thời cùng token: request sau chờ khóa dòng,
     * đánh giá lại điều kiện sau khi request đầu commit và không cập nhật được dòng nào.
     */
    private static final String CONSUME = """
            UPDATE refresh_tokens rt
               SET used_at = :now, replaced_by_id = :replacedBy
             WHERE rt.token_hash = :hash
               AND rt.used_at IS NULL
               AND rt.revoked_at IS NULL
               AND rt.expires_at > :now
               AND EXISTS (SELECT 1 FROM devices d WHERE d.id = rt.device_id AND d.revoked_at IS NULL)
               AND EXISTS (SELECT 1 FROM users u WHERE u.id = rt.user_id AND u.status = 'ACTIVE')
            RETURNING rt.id, rt.user_id, rt.device_id, rt.family_id, rt.family_started_at
            """;

    private static final String INSERT = """
            INSERT INTO refresh_tokens
                (id, user_id, device_id, family_id, token_hash, family_started_at, issued_at, expires_at)
            VALUES (:id, :userId, :deviceId, :familyId, :hash, :familyStartedAt, :issuedAt, :expiresAt)
            """;

    private final JdbcClient jdbc;

    public RefreshTokenRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(UUID id, UUID userId, UUID deviceId, UUID familyId, String tokenHash, Instant familyStartedAt,
            Instant issuedAt, Instant expiresAt) {
        jdbc.sql(INSERT)
                .param("id", id)
                .param("userId", userId)
                .param("deviceId", deviceId)
                .param("familyId", familyId)
                .param("hash", tokenHash)
                .param("familyStartedAt", ts(familyStartedAt))
                .param("issuedAt", ts(issuedAt))
                .param("expiresAt", ts(expiresAt))
                .update();
    }

    /** @return token vừa được dùng nếu còn hiệu lực; rỗng nếu không (không phân biệt lý do ở bước này) */
    public Optional<ConsumedToken> consume(String tokenHash, UUID replacedById, Instant now) {
        return jdbc.sql(CONSUME)
                .param("hash", tokenHash)
                .param("replacedBy", replacedById)
                .param("now", ts(now))
                .query(RefreshTokenRepository::consumed)
                .optional();
    }

    /** Trạng thái hiện tại của token theo hash, bất kể còn hiệu lực hay không. */
    public Optional<TokenState> findByHash(String tokenHash) {
        return jdbc.sql("""
                        SELECT id, user_id, device_id, family_id, used_at, revoked_at, expires_at
                          FROM refresh_tokens WHERE token_hash = :hash
                        """)
                .param("hash", tokenHash)
                .query((rs, n) -> new TokenState(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getObject("device_id", UUID.class), rs.getObject("family_id", UUID.class),
                        instant(rs, "used_at"), instant(rs, "revoked_at"), instant(rs, "expires_at")))
                .optional();
    }

    /** Thu hồi mọi token chưa thu hồi của một family. */
    public int revokeFamily(UUID familyId, Instant now) {
        return jdbc.sql("UPDATE refresh_tokens SET revoked_at = :now WHERE family_id = :f AND revoked_at IS NULL")
                .param("f", familyId).param("now", ts(now)).update();
    }

    /** Thu hồi mọi token chưa thu hồi của một thiết bị (thu hồi thiết bị; đăng nhập lại cùng thiết bị, D75). */
    public int revokeByDevice(UUID userId, UUID deviceId, Instant now) {
        return jdbc.sql("UPDATE refresh_tokens SET revoked_at = :now"
                        + " WHERE user_id = :u AND device_id = :d AND revoked_at IS NULL")
                .param("u", userId).param("d", deviceId).param("now", ts(now)).update();
    }

    /** Đặt lại mật khẩu: thu hồi MỌI refresh token của user (SECURITY_RULES §3.4). */
    public int revokeAllForUser(UUID userId, Instant now) {
        return jdbc.sql("UPDATE refresh_tokens SET revoked_at = :now WHERE user_id = :u AND revoked_at IS NULL")
                .param("u", userId).param("now", ts(now)).update();
    }

    /** Đổi mật khẩu: thu hồi refresh token của các thiết bị KHÁC thiết bị hiện tại (SECURITY_RULES §3.4). */
    public int revokeAllForUserExceptDevice(UUID userId, UUID keepDeviceId, Instant now) {
        return jdbc.sql("UPDATE refresh_tokens SET revoked_at = :now"
                        + " WHERE user_id = :u AND device_id <> :d AND revoked_at IS NULL")
                .param("u", userId).param("d", keepDeviceId).param("now", ts(now)).update();
    }

    private static ConsumedToken consumed(ResultSet rs, int rowNum) throws SQLException {
        return new ConsumedToken(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getObject("device_id", UUID.class), rs.getObject("family_id", UUID.class),
                instant(rs, "family_started_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static OffsetDateTime ts(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    /** Token vừa được đánh dấu dùng trong lần xoay vòng này. */
    /** Job dọn (SYSTEM_ARCHITECTURE §3.12): xóa refresh token hết hạn trước {@code cutoff}. */
    public int deleteExpiredBefore(Instant cutoff) {
        return jdbc.sql("DELETE FROM refresh_tokens WHERE expires_at < :cutoff")
                .param("cutoff", java.time.OffsetDateTime.ofInstant(cutoff, java.time.ZoneOffset.UTC)).update();
    }

    public record ConsumedToken(UUID id, UUID userId, UUID deviceId, UUID familyId, Instant familyStartedAt) {
    }

    /** Trạng thái lưu trữ của một token. */
    public record TokenState(UUID id, UUID userId, UUID deviceId, UUID familyId, Instant usedAt, Instant revokedAt,
            Instant expiresAt) {
    }
}
