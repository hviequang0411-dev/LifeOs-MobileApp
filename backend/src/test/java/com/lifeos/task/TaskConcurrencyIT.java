package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Đường @Version của Hibernate cho Task (I4, D58), theo cách XÁC ĐỊNH: kết nối JDBC khác tăng version và giữ khóa dòng;
 * PATCH vượt qua kiểm tra tường minh rồi đứng chờ ở UPDATE; khi kết nối kia commit → 409 kèm currentVersion, không ghi
 * change_logs.
 */
class TaskConcurrencyIT extends TaskApiTestBase {

    @Autowired
    DataSource dataSource;

    @Test
    void hibernateVersionConflictOnPatchIs409WithCurrentVersion() throws Exception {
        TestUser user = newUser();
        UUID id = task(user);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection competitor = dataSource.getConnection()) {
            competitor.setAutoCommit(false);
            try (PreparedStatement ps = competitor.prepareStatement(
                    "UPDATE tasks SET version = version + 1, title = 'Thắng' WHERE id = ?")) {
                ps.setObject(1, id);
                ps.executeUpdate();
            }
            Future<MvcResult> patch = pool.submit(() -> patchTask(user, id, "version", 1, "title", "Thua"));
            await().atMost(Duration.ofSeconds(15)).until(() -> jdbc.sql("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE wait_event_type = 'Lock' AND query ILIKE 'update tasks%'
                    """).query(Integer.class).single() > 0);
            competitor.commit();

            MvcResult result = patch.get(15, TimeUnit.SECONDS);
            assertThat(result.getResponse().getStatus()).isEqualTo(409);
            assertThat(readJson(result)).containsEntry("errorCode", "ENTITY_VERSION_CONFLICT")
                    .containsEntry("currentVersion", 2);
        } finally {
            pool.shutdownNow();
        }
        assertThat(taskRow(id).get("title")).isEqualTo("Thắng");
        assertThat(changeLogs(user.id(), "task", id, "UPDATE")).isZero();
    }
}
