package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.lifeos.support.ApiTestBase;
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
 * Các đường đồng thời của Category, theo cách XÁC ĐỊNH (kết nối JDBC khác giữ khóa chưa commit; Awaitility chờ request
 * đứng đợi khóa trong pg_stat_activity; timeout cứng):
 * <ul>
 *   <li>hai lần tạo cùng tên (khác hoa thường) → đúng một bản ghi, request sau nhận 409 CATEGORY_NAME_TAKEN — vi phạm
 *       ux_categories_user_name xảy ra SAU bước kiểm trước;</li>
 *   <li>hai lần tạo cùng id → 409 ENTITY_ALREADY_EXISTS;</li>
 *   <li>@Version của Hibernate khi PATCH → 409 kèm currentVersion.</li>
 * </ul>
 * Khóa FOR SHARE khi Task tham chiếu Category (và thứ tự Project trước Category) kiểm ở CascadeConcurrencyIT (2c/2e).
 */
class CategoryConcurrencyIT extends ApiTestBase {

    private static final String BASE = "/api/v1/categories";

    @Autowired
    DataSource dataSource;

    private void awaitWaitingOnLock(String sqlPrefix) {
        await().atMost(Duration.ofSeconds(15)).until(() -> jdbc.sql("""
                SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND query ILIKE :q
                """).param("q", sqlPrefix + "%").query(Integer.class).single() > 0);
    }

    /** Chạy request trong luồng khác trong khi một kết nối JDBC giữ câu lệnh chưa commit, rồi commit nó. */
    private MvcResult whileCompetitorHolds(String competitorSql, Object[] params, String waitingSql,
            java.util.concurrent.Callable<MvcResult> request) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection competitor = dataSource.getConnection()) {
            competitor.setAutoCommit(false);
            try (PreparedStatement ps = competitor.prepareStatement(competitorSql)) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
                ps.executeUpdate();
            }
            Future<MvcResult> future = pool.submit(request);
            awaitWaitingOnLock(waitingSql);
            competitor.commit();
            return future.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentCreateWithTheSameNameIgnoringCaseIsNameTaken() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();

        MvcResult result = whileCompetitorHolds(
                "INSERT INTO categories (id, user_id, name) VALUES (?, ?, 'Gia Đình')",
                new Object[] {UUID.randomUUID(), user.id()}, "insert into categories",
                () -> post(user, BASE, body("id", id.toString(), "name", "gia đình")));

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(readJson(result)).containsEntry("errorCode", "CATEGORY_NAME_TAKEN");
        assertThat(jdbc.sql("SELECT count(*) FROM categories WHERE user_id = :u AND deleted_at IS NULL")
                .param("u", user.id()).query(Integer.class).single()).isEqualTo(1);
        assertThat(changeLogs(user.id(), "category", id)).isZero();
    }

    @Test
    void concurrentCreateWithTheSameIdIsAlreadyExists() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID id = UUID.randomUUID();

        MvcResult result = whileCompetitorHolds(
                "INSERT INTO categories (id, user_id, name) VALUES (?, ?, 'Trước')",
                new Object[] {id, other.id()}, "insert into categories",
                () -> post(user, BASE, body("id", id.toString(), "name", "Sau")));

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(readJson(result)).containsEntry("errorCode", "ENTITY_ALREADY_EXISTS");
    }

    @Test
    void hibernateVersionConflictOnPatchIs409WithCurrentVersion() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        assertThat(post(user, BASE, body("id", id.toString(), "name", "Gốc")).getResponse().getStatus())
                .isEqualTo(201);

        MvcResult result = whileCompetitorHolds(
                "UPDATE categories SET version = version + 1, name = 'Thắng' WHERE id = ?",
                new Object[] {id}, "update categories",
                () -> patch(user, BASE + "/" + id, body("version", 1, "name", "Thua")));

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(readJson(result)).containsEntry("errorCode", "ENTITY_VERSION_CONFLICT")
                .containsEntry("currentVersion", 2);
        assertThat(changeLogs(user.id(), "category", id, "UPDATE")).isZero();
    }
}
