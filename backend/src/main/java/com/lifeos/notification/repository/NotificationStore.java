package com.lifeos.notification.repository;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Dữ liệu dẫn xuất của notification (PHASE_4 §5: notifications, notification_deliveries, notification_preferences,
 * device_push_tokens) — KHÔNG đồng bộ, không change_logs. SQL tham số hóa; mọi truy vấn theo user_id trừ hàng đợi gửi
 * (worker toàn hệ thống).
 */
@Repository
public class NotificationStore {

    private final JdbcClient jdbc;

    public NotificationStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    // ---------------------------------------------------------------- notifications

    public record NotificationRow(UUID id, UUID userId, String category, String templateKey, String title,
            String body, String data, Instant readAt, Instant createdAt, Instant expiresAt) {
        @Override
        public String toString() {
            return "NotificationRow[id=" + id + "]";
        }
    }

    /** D122: idempotent theo dedupe_key. @return true nếu đã chèn */
    public boolean insertNotification(NotificationRow n, String dedupeKey) {
        return jdbc.sql("""
                INSERT INTO notifications (id, user_id, category, template_key, title, body, data, dedupe_key,
                                           created_at, expires_at)
                VALUES (:id, :userId, :category, :templateKey, :title, :body, CAST(:data AS jsonb), :dedupe,
                        :createdAt, :expiresAt)
                ON CONFLICT (dedupe_key) DO NOTHING
                """).param("id", n.id()).param("userId", n.userId()).param("category", n.category())
                .param("templateKey", n.templateKey()).param("title", n.title()).param("body", n.body())
                .param("data", n.data()).param("dedupe", dedupeKey).param("createdAt", utc(n.createdAt()))
                .param("expiresAt", utc(n.expiresAt())).update() == 1;
    }

    private static final String NOTIFICATION_COLUMNS = "id, user_id, category, template_key, title, body,"
            + " data::text AS data, read_at, created_at, expires_at";

    private static NotificationRow row(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new NotificationRow(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getString("category"), rs.getString("template_key"), rs.getString("title"), rs.getString("body"),
                rs.getString("data"), instant(rs, "read_at"), instant(rs, "created_at"), instant(rs, "expires_at"));
    }

    private static Instant instant(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    public Optional<NotificationRow> notification(UUID id) {
        return jdbc.sql("SELECT " + NOTIFICATION_COLUMNS + " FROM notifications WHERE id = :id").param("id", id)
                .query((rs, n) -> row(rs)).optional();
    }

    /** Inbox (D142): còn hạn, mới nhất trước, keyset (createdAt, id). */
    public List<NotificationRow> inbox(UUID userId, boolean unreadOnly, Instant now, Instant afterCreatedAt,
            UUID afterId, int limit) {
        return jdbc.sql("SELECT " + NOTIFICATION_COLUMNS + """
                 FROM notifications
                WHERE user_id = :userId AND expires_at > :now
                  AND (:unread = false OR read_at IS NULL)
                  AND (CAST(:afterAt AS timestamptz) IS NULL OR created_at < :afterAt
                       OR (created_at = :afterAt AND id < :afterId))
                ORDER BY created_at DESC, id DESC LIMIT :limit
                """).param("userId", userId).param("now", utc(now)).param("unread", unreadOnly)
                .param("afterAt", utc(afterCreatedAt), Types.TIMESTAMP_WITH_TIMEZONE)
                .param("afterId", afterId, Types.OTHER).param("limit", limit)
                .query((rs, n) -> row(rs)).list();
    }

    public long unreadCount(UUID userId, Instant now) {
        return jdbc.sql("SELECT count(*) FROM notifications WHERE user_id = :u AND read_at IS NULL"
                + " AND expires_at > :now").param("u", userId).param("now", utc(now)).query(Long.class).single();
    }

    /** @return số dòng khớp (thông báo của user, còn hạn); read_at chỉ đặt nếu NULL (idempotent, D142) */
    public int markRead(UUID userId, UUID id, Instant now) {
        int found = jdbc.sql("SELECT count(*) FROM notifications WHERE id = :id AND user_id = :u AND expires_at > :now")
                .param("id", id).param("u", userId).param("now", utc(now)).query(Integer.class).single();
        jdbc.sql("UPDATE notifications SET read_at = :now WHERE id = :id AND user_id = :u AND read_at IS NULL")
                .param("id", id).param("u", userId).param("now", utc(now)).update();
        return found;
    }

    public int markAllRead(UUID userId, Instant now) {
        return jdbc.sql("UPDATE notifications SET read_at = :now WHERE user_id = :u AND read_at IS NULL")
                .param("u", userId).param("now", utc(now)).update();
    }

    public int deleteNotification(UUID userId, UUID id) {
        return jdbc.sql("DELETE FROM notifications WHERE id = :id AND user_id = :u").param("id", id)
                .param("u", userId).update();
    }

    /**
     * Dọn hết hạn theo lô (D142). Thông báo còn delivery chưa kết thúc (PENDING/IN_FLIGHT — ví dụ push đang chờ hết
     * giờ yên tĩnh khi IN_APP tắt nên expires_at = lúc tạo) được GIỮ tới khi mọi delivery xong: xóa cha sẽ cascade xóa
     * delivery và push/email không bao giờ được gửi (D121, D135; đợt sửa F2). Hộp thư vẫn ẩn nó theo expires_at.
     */
    public int deleteExpired(Instant now, int batch) {
        return jdbc.sql("""
                DELETE FROM notifications WHERE id IN (
                    SELECT n.id FROM notifications n
                    WHERE n.expires_at <= :now
                      AND NOT EXISTS (SELECT 1 FROM notification_deliveries d
                                      WHERE d.notification_id = n.id AND d.status IN ('PENDING', 'IN_FLIGHT'))
                    ORDER BY n.expires_at LIMIT :batch)
                """).param("now", utc(now)).param("batch", batch).update();
    }

    // ---------------------------------------------------------------- deliveries

    public void insertDelivery(UUID id, UUID userId, UUID notificationId, String channel, UUID tokenId,
            Instant scheduledFor, Instant now) {
        jdbc.sql("""
                INSERT INTO notification_deliveries (id, user_id, notification_id, channel, device_token_id, status,
                                                     scheduled_for, attempt_count, next_attempt_at, created_at)
                VALUES (:id, :u, :n, :channel, :token, 'PENDING', :at, 0, :at, :now)
                """).param("id", id).param("u", userId).param("n", notificationId).param("channel", channel)
                .param("token", tokenId, Types.OTHER).param("at", utc(scheduledFor)).param("now", utc(now))
                .update();
    }

    /** Delivery đã nhận, kèm {@code lease} — hạn khóa mà worker phải trình ra khi ghi kết quả (đợt sửa F3). */
    public record Claimed(UUID id, UUID userId, UUID notificationId, String channel, UUID tokenId, int attempts,
            Instant scheduledFor, Instant createdAt, OffsetDateTime lease) {
    }

    /**
     * D137: chọn việc đến hạn (PENDING, hoặc IN_FLIGHT quá hạn khóa 5 phút — tự khôi phục) bằng FOR UPDATE SKIP LOCKED,
     * đánh dấu IN_FLIGHT với hạn khóa mới. Gọi trong transaction NGẮN; gửi mạng ở ngoài. Hạn khóa (next_attempt_at
     * của dòng IN_FLIGHT, cắt tới micro giây) đồng thời là dấu sở hữu: lần lấy lại sau luôn đặt hạn khóa muộn hơn
     * (chỉ lấy lại được khi hạn cũ ≤ now), nên hai lần nhận cùng một dòng không bao giờ trùng dấu.
     */
    public List<Claimed> claim(Instant now, Instant lockUntil, int batch) {
        OffsetDateTime lease = OffsetDateTime.ofInstant(lockUntil.truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                ZoneOffset.UTC);
        return jdbc.sql("""
                UPDATE notification_deliveries d SET status = 'IN_FLIGHT', next_attempt_at = :lockUntil
                FROM (SELECT id FROM notification_deliveries
                      WHERE status IN ('PENDING', 'IN_FLIGHT') AND next_attempt_at <= :now
                      ORDER BY next_attempt_at LIMIT :batch FOR UPDATE SKIP LOCKED) due
                WHERE d.id = due.id
                RETURNING d.id, d.user_id, d.notification_id, d.channel, d.device_token_id, d.attempt_count,
                          d.scheduled_for, d.created_at
                """).param("now", utc(now)).param("lockUntil", lease).param("batch", batch)
                .query((rs, n) -> new Claimed(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                        rs.getObject("notification_id", UUID.class), rs.getString("channel"),
                        rs.getObject("device_token_id", UUID.class), rs.getInt("attempt_count"),
                        instant(rs, "scheduled_for"), instant(rs, "created_at"), lease))
                .list();
    }

    /**
     * Ghi kết quả (kết thúc hoặc hẹn thử lại) CHỈ khi worker này còn giữ lease: dòng vẫn IN_FLIGHT VÀ hạn khóa đúng
     * là hạn đã cấp lúc nhận — cùng mẫu {@code OutboxStore.OWNED}. Worker quá hạn bị worker khác lấy lại sẽ không ghi
     * đè được kết quả của chủ mới (đợt sửa F3).
     *
     * @return true nếu còn giữ lease và đã ghi
     */
    public boolean finish(Claimed claimed, String status, int attempts, Instant nextAttemptAt, String errorCode,
            Instant sentAt) {
        return jdbc.sql("""
                UPDATE notification_deliveries
                SET status = :status, attempt_count = :attempts, next_attempt_at = :next, last_error_code = :error,
                    sent_at = :sentAt
                WHERE id = :id AND status = 'IN_FLIGHT' AND next_attempt_at = :lease
                """).param("id", claimed.id()).param("lease", claimed.lease()).param("status", status)
                .param("attempts", attempts)
                .param("next", utc(nextAttemptAt)).param("error", errorCode)
                .param("sentAt", utc(sentAt), Types.TIMESTAMP_WITH_TIMEZONE).update() == 1;
    }

    public List<Map<String, Object>> deliveriesOf(UUID notificationId) {
        return jdbc.sql("SELECT * FROM notification_deliveries WHERE notification_id = :n ORDER BY created_at, id")
                .param("n", notificationId).query().listOfRows();
    }

    // ---------------------------------------------------------------- preferences

    public Map<String, Boolean> preferences(UUID userId) {
        Map<String, Boolean> out = new java.util.HashMap<>();
        jdbc.sql("SELECT category, channel, enabled FROM notification_preferences WHERE user_id = :u")
                .param("u", userId).query((rs, n) -> {
                    out.put(rs.getString("category") + ":" + rs.getString("channel"), rs.getBoolean("enabled"));
                    return null;
                }).list();
        return out;
    }

    public void upsertPreference(UUID userId, String category, String channel, boolean enabled) {
        jdbc.sql("""
                INSERT INTO notification_preferences (user_id, category, channel, enabled)
                VALUES (:u, :c, :ch, :e)
                ON CONFLICT (user_id, category, channel) DO UPDATE SET enabled = EXCLUDED.enabled
                """).param("u", userId).param("c", category).param("ch", channel).param("e", enabled).update();
    }

    // ---------------------------------------------------------------- push tokens

    public record PushToken(UUID id, UUID userId, UUID deviceId, String platform, String token) {
        @Override
        public String toString() {
            return "PushToken[id=" + id + "]";
        }
    }

    private static final String TOKEN_COLUMNS = "id, user_id, device_id, platform, token";

    private static PushToken token(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PushToken(rs.getObject("id", UUID.class), rs.getObject("user_id", UUID.class),
                rs.getObject("device_id", UUID.class), rs.getString("platform"), rs.getString("token"));
    }

    public List<PushToken> tokensOf(UUID userId) {
        return jdbc.sql("SELECT " + TOKEN_COLUMNS + " FROM device_push_tokens WHERE user_id = :u ORDER BY id")
                .param("u", userId).query((rs, n) -> token(rs)).list();
    }

    public Optional<PushToken> token(UUID id) {
        return jdbc.sql("SELECT " + TOKEN_COLUMNS + " FROM device_push_tokens WHERE id = :id").param("id", id)
                .query((rs, n) -> token(rs)).optional();
    }

    /** Khóa dòng người dùng-cấp (theo token) trước upsert: token đổi chủ được chuyển sang thiết bị mới (D139). */
    public Optional<PushToken> tokenByValueForUpdate(String token) {
        return jdbc.sql("SELECT " + TOKEN_COLUMNS + " FROM device_push_tokens WHERE token = :t FOR UPDATE")
                .param("t", token).query((rs, n) -> token(rs)).optional();
    }

    public int deleteTokenById(UUID id) {
        return jdbc.sql("DELETE FROM device_push_tokens WHERE id = :id").param("id", id).update();
    }

    public int deleteTokenOfDevice(UUID userId, UUID deviceId) {
        return jdbc.sql("DELETE FROM device_push_tokens WHERE user_id = :u AND device_id = :d").param("u", userId)
                .param("d", deviceId).update();
    }

    public void upsertDeviceToken(UUID userId, UUID deviceId, String platform, String token, Instant now) {
        jdbc.sql("""
                INSERT INTO device_push_tokens (id, user_id, device_id, platform, token, created_at, last_seen_at)
                VALUES (:id, :u, :d, :p, :t, :now, :now)
                ON CONFLICT (device_id) DO UPDATE SET token = EXCLUDED.token, platform = EXCLUDED.platform,
                    user_id = EXCLUDED.user_id, last_seen_at = EXCLUDED.last_seen_at
                """).param("id", UUID.randomUUID()).param("u", userId).param("d", deviceId).param("p", platform)
                .param("t", token).param("now", utc(now)).update();
    }

    /** D139: tối đa {@code max} token mỗi user — gỡ token cũ nhất theo last_seen_at. @return số đã gỡ */
    public int trimTokens(UUID userId, int max) {
        return jdbc.sql("""
                DELETE FROM device_push_tokens WHERE id IN (
                    SELECT id FROM device_push_tokens WHERE user_id = :u
                    ORDER BY last_seen_at DESC, id DESC OFFSET :max)
                """).param("u", userId).param("max", max).update();
    }

    /** D141: gỡ token không thấy quá hạn. */
    public int deleteStaleTokens(Instant before) {
        return jdbc.sql("DELETE FROM device_push_tokens WHERE last_seen_at < :before").param("before", utc(before))
                .update();
    }

    public Map<UUID, String> tokenSuffixes(UUID userId) {
        Map<UUID, String> out = new java.util.HashMap<>();
        jdbc.sql("SELECT device_id, right(token, 6) AS suffix FROM device_push_tokens WHERE user_id = :u")
                .param("u", userId).query((rs, n) -> {
                    out.put(rs.getObject("device_id", UUID.class), rs.getString("suffix"));
                    return null;
                }).list();
        return out;
    }
}
