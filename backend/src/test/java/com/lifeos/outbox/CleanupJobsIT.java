package com.lifeos.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.outbox.OutboxCleanupJob;
import com.lifeos.identity.service.TokenCleanupService;
import com.lifeos.support.IntegrationTestBase;
import com.lifeos.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Job dọn định kỳ (SYSTEM_ARCHITECTURE §3.12, DATABASE_DESIGN §7): token hết hạn quá 7 ngày và outbox SENT/FAILED
 * hoàn tất quá 7 ngày bị xóa; phần còn lại giữ nguyên. Chạy lặp không đổi kết quả (idempotent).
 */
class CleanupJobsIT extends IntegrationTestBase {

    @Autowired
    JdbcClient jdbc;

    @Autowired
    TokenCleanupService tokenCleanup;

    @Autowired
    OutboxCleanupJob outboxCleanup;

    private static OffsetDateTime at(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private boolean exists(String table, UUID id) {
        return jdbc.sql("SELECT count(*) FROM " + table + " WHERE id = :id").param("id", id)
                .query(Integer.class).single() == 1;
    }

    private UUID token(String table, UUID userId, Instant expiresAt) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO " + table + " (id, user_id, token_hash, expires_at) VALUES (:id, :u, :h, :e)")
                .param("id", id).param("u", userId).param("h", "%064d".formatted(Math.abs(id.getMostSignificantBits())))
                .param("e", at(expiresAt)).update();
        return id;
    }

    private UUID refreshToken(UUID userId, Instant expiresAt) {
        UUID device = UUID.randomUUID();
        jdbc.sql("INSERT INTO devices (id, user_id, name, platform) VALUES (:id, :u, 'Pixel', 'ANDROID')")
                .param("id", device).param("u", userId).update();
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO refresh_tokens (id, user_id, device_id, family_id, token_hash, family_started_at, expires_at)
                VALUES (:id, :u, :d, :f, :h, :e, :e)""")
                .param("id", id).param("u", userId).param("d", device).param("f", UUID.randomUUID())
                .param("h", "%064d".formatted(Math.abs(id.getLeastSignificantBits()))).param("e", at(expiresAt))
                .update();
        return id;
    }

    private UUID outbox(String status, Instant processedAt) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                INSERT INTO outbox_events (id, event_type, payload, status, processed_at)
                VALUES (:id, 'PASSWORD_CHANGED', '{}'::jsonb, :s, :p)""")
                .param("id", id).param("s", status).param("p", processedAt == null ? null : at(processedAt),
                        java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                .update();
        return id;
    }

    @Test
    void tokensExpiredMoreThan7DaysAgoAreDeletedOthersKept() {
        UUID user = TestUsers.insert(jdbc);
        Instant now = clock.instant();
        Instant old = now.minus(Duration.ofDays(8));
        Instant recent = now.minus(Duration.ofDays(6));
        UUID oldVerification = token("email_verification_tokens", user, old);
        UUID recentVerification = token("email_verification_tokens", user, recent);
        UUID oldReset = token("password_reset_tokens", user, old);
        UUID liveReset = token("password_reset_tokens", user, now.plus(Duration.ofHours(1)));
        UUID oldRefresh = refreshToken(user, old);
        UUID recentRefresh = refreshToken(user, recent);

        assertThat(tokenCleanup.cleanup()).isGreaterThanOrEqualTo(3);

        assertThat(exists("email_verification_tokens", oldVerification)).isFalse();
        assertThat(exists("password_reset_tokens", oldReset)).isFalse();
        assertThat(exists("refresh_tokens", oldRefresh)).isFalse();
        assertThat(exists("email_verification_tokens", recentVerification)).isTrue();
        assertThat(exists("password_reset_tokens", liveReset)).isTrue();
        assertThat(exists("refresh_tokens", recentRefresh)).isTrue();
        tokenCleanup.cleanup();
        assertThat(exists("email_verification_tokens", recentVerification)).as("chạy lặp an toàn").isTrue();
    }

    @Test
    void finishedOutboxEventsOlderThan7DaysAreDeletedPendingAndRecentKept() {
        Instant now = clock.instant();
        UUID oldSent = outbox("SENT", now.minus(Duration.ofDays(8)));
        UUID oldFailed = outbox("FAILED", now.minus(Duration.ofDays(8)));
        UUID recentSent = outbox("SENT", now.minus(Duration.ofDays(6)));
        UUID pending = outbox("PENDING", null);

        assertThat(outboxCleanup.cleanup()).isGreaterThanOrEqualTo(2);

        assertThat(exists("outbox_events", oldSent)).isFalse();
        assertThat(exists("outbox_events", oldFailed)).isFalse();
        assertThat(exists("outbox_events", recentSent)).isTrue();
        assertThat(exists("outbox_events", pending)).isTrue();
        jdbc.sql("DELETE FROM outbox_events WHERE id = :id").param("id", pending).update();
    }
}
