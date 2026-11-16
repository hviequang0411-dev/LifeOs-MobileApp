package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.util.Patch;
import com.lifeos.identity.service.UpdateSettingsCommand;
import com.lifeos.identity.service.UserSettingsCommandService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Hai PATCH /me/settings đồng thời cùng version (D58, I4): đúng một request thành công; request còn lại nhận 409
 * ENTITY_VERSION_CONFLICT kèm currentVersion — kể cả khi xung đột chỉ bị phát hiện bởi @Version của Hibernate lúc
 * flush (cả hai đã vượt qua bước kiểm version tường minh). Có timeout cứng.
 */
class SettingsConcurrencyIT extends IdentityIntegrationTest {

    @Autowired
    UserSettingsCommandService settingsCommand;

    @Autowired
    DataSource dataSource;

    private long settingsVersion(UUID userId) {
        return jdbc.sql("SELECT version FROM user_settings WHERE user_id = :u").param("u", userId)
                .query(Long.class).single();
    }

    private int settingsUpdates(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u AND entity_type = 'user_settings'"
                + " AND operation = 'UPDATE'").param("u", userId).query(Integer.class).single();
    }

    /**
     * Đường @Version một cách xác định: một transaction khác tăng version và giữ khóa dòng (chưa commit). Service đọc
     * được version 1 (bản đã commit), vượt qua kiểm tra tường minh, rồi bị chặn ở UPDATE khi flush. Khi transaction kia
     * commit, UPDATE … WHERE version = 1 không khớp dòng nào → Hibernate ném lỗi optimistic lock.
     */
    @Test
    void hibernateVersionCheckIsReportedAs409WithCurrentVersion() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection competitor = dataSource.getConnection()) {
            competitor.setAutoCommit(false);
            try (PreparedStatement ps = competitor.prepareStatement(
                    "UPDATE user_settings SET version = version + 1, week_start_day = 5 WHERE user_id = ?")) {
                ps.setObject(1, userId);
                assertThat(ps.executeUpdate()).isEqualTo(1);
            }

            Future<?> update = pool.submit(() -> settingsCommand.update(userId, new UpdateSettingsCommand(1,
                    Patch.of("Asia/Tokyo"), Patch.absent(), Patch.absent(), Patch.absent())));

            // Chờ đến khi UPDATE của service thật sự đứng chờ khóa dòng (đã qua kiểm tra tường minh).
            await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.sql("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE wait_event_type = 'Lock' AND query ILIKE 'update user_settings%'
                    """).query(Integer.class).single() > 0);
            competitor.commit();

            Throwable thrown = null;
            try {
                update.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                thrown = e.getCause();
            }
            assertThat(thrown).isInstanceOf(ConflictException.class);
            ConflictException conflict = (ConflictException) thrown;
            assertThat(conflict.getErrorCode()).isEqualTo(ErrorCode.ENTITY_VERSION_CONFLICT);
            assertThat(conflict.getProperties()).containsEntry("currentVersion", 2L);
        } finally {
            pool.shutdownNow();
        }
        Map<String, Object> row = jdbc.sql("SELECT timezone, week_start_day, version FROM user_settings"
                + " WHERE user_id = :u").param("u", userId).query().singleRow();
        assertThat(row.get("timezone")).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(((Number) row.get("week_start_day")).intValue()).isEqualTo(5);
        assertThat(row.get("version")).isEqualTo(2L);
        assertThat(settingsUpdates(userId)).as("request thua không ghi change_logs").isZero();
    }

    /** Hai PATCH qua HTTP đồng thời cùng version, lặp 20 lần (đường nào phát hiện xung đột cũng phải ra 409 đúng). */
    @Test
    void concurrentPatchesWithTheSameVersionHaveExactlyOneWinner() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        String access = str(loginOk(email, null), "accessToken");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 20; i++) {
                long version = settingsVersion(userId);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<MvcResult>> futures = new ArrayList<>();
                for (int t = 0; t < 2; t++) {
                    // Giá trị khác lần trước (lệch 1–3 theo mod 7) để request thắng luôn là một thay đổi thật.
                    int weekStartDay = 1 + ((i * 2 + t) % 7);
                    String timezone = t == 0 ? "Europe/Paris" : "Asia/Tokyo";
                    futures.add(pool.submit(() -> {
                        start.await();
                        return sendAuthed("PATCH", "/api/v1/me/settings", body("version", version,
                                "timezone", timezone, "weekStartDay", weekStartDay),
                                access);
                    }));
                }
                start.countDown();

                int ok = 0;
                int conflicts = 0;
                for (Future<MvcResult> f : futures) {
                    MvcResult r = f.get(30, TimeUnit.SECONDS);
                    if (r.getResponse().getStatus() == 200) {
                        ok++;
                        assertThat(((Number) readJson(r).get("version")).longValue()).isEqualTo(version + 1);
                    } else {
                        conflicts++;
                        assertThat(r.getResponse().getStatus()).as("lần %d", i).isEqualTo(409);
                        Map<String, Object> problem = readJson(r);
                        assertThat(problem).containsEntry("errorCode", "ENTITY_VERSION_CONFLICT");
                        assertThat(((Number) problem.get("currentVersion")).longValue()).isEqualTo(version + 1);
                    }
                }
                assertThat(ok).as("lần %d: số request thành công", i).isEqualTo(1);
                assertThat(conflicts).as("lần %d: số request xung đột", i).isEqualTo(1);
                assertThat(settingsVersion(userId)).isEqualTo(version + 1);
            }
            assertThat(settingsUpdates(userId)).as("mỗi lần đúng một change_logs UPDATE").isEqualTo(20);
        } finally {
            pool.shutdownNow();
        }
    }
}
