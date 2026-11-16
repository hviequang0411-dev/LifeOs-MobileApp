package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Chống Task "mồ côi" khi xóa Project/Category đồng thời với ghi Task (D80; SYN-13/14/21 phần server). Mọi kịch bản
 * XÁC ĐỊNH: một kết nối JDBC đóng vai giao dịch đối thủ và giữ khóa dòng chưa commit; Awaitility chờ đến khi request
 * thật đứng đợi khóa trong pg_stat_activity; timeout cứng.
 * <ul>
 *   <li>Xóa giữ FOR UPDATE trước → Task tạo sau chờ FOR SHARE, rồi thấy bản ghi đã xóa → 422, không có Task mồ côi.</li>
 *   <li>Ghi Task giữ FOR SHARE trước → xóa chờ FOR UPDATE, rồi cascade gỡ luôn Task vừa ghi.</li>
 *   <li>Thứ tự khóa Project trước Category: Task đang chờ khóa Category thì đã giữ khóa Project, nên xóa Project phải
 *       chờ — cùng một thứ tự ở mọi đường nên không có vòng chờ (deadlock).</li>
 *   <li>Xóa Project đụng một Task đang bị sửa đồng thời: @Version chặn cascade, cả việc xóa rollback (409).</li>
 * </ul>
 */
class CascadeConcurrencyIT extends TaskApiTestBase {

    @Autowired
    DataSource dataSource;

    private ExecutorService pool;
    private Connection competitor;

    @BeforeEach
    void open() throws Exception {
        pool = Executors.newFixedThreadPool(2);
        competitor = dataSource.getConnection();
        competitor.setAutoCommit(false);
    }

    @AfterEach
    void close() throws Exception {
        pool.shutdownNow();
        if (!competitor.isClosed()) {
            competitor.rollback();
            competitor.close();
        }
    }

    private void competitorExecutes(String sql, Object... params) throws Exception {
        try (PreparedStatement ps = competitor.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.execute();
        }
    }

    private Future<MvcResult> async(Callable<MvcResult> request) {
        return pool.submit(request);
    }

    private int waitingOn(String sqlPattern) {
        return jdbc.sql("SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND query ILIKE :q")
                .param("q", sqlPattern).query(Integer.class).single();
    }

    private void awaitWaitingOn(String sqlPattern) {
        await().atMost(Duration.ofSeconds(15)).until(() -> waitingOn(sqlPattern) > 0);
    }

    private int liveTasksReferencing(String column, UUID id) {
        return jdbc.sql("SELECT count(*) FROM tasks WHERE " + column + " = :id AND deleted_at IS NULL")
                .param("id", id).query(Integer.class).single();
    }

    // ---------- Project

    @Test
    void projectDeletionFirstMakesAConcurrentTaskCreateFailWith422() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        competitorExecutes("SELECT 1 FROM projects WHERE id = ? FOR UPDATE", project);
        competitorExecutes("UPDATE projects SET deleted_at = now(), version = version + 1 WHERE id = ?", project);

        UUID taskId = UUID.randomUUID();
        Future<MvcResult> create = async(() -> createTask(user, taskId, "projectId", project.toString()));
        awaitWaitingOn("select%from projects%");
        competitor.commit();

        MvcResult result = create.get(15, TimeUnit.SECONDS);
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(result)).containsEntry("errorCode", "PROJECT_NOT_FOUND");
        assertThat(liveTasksReferencing("project_id", project)).as("không có Task mồ côi").isZero();
    }

    @Test
    void taskWriteFirstIsCascadedByTheConcurrentProjectDeletion() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        UUID inFlightTask = UUID.randomUUID();
        // Đối thủ đóng vai một lần tạo Task đang dở: đã khóa Project FOR SHARE và chèn Task trỏ tới nó.
        competitorExecutes("SELECT 1 FROM projects WHERE id = ? FOR SHARE", project);
        competitorExecutes("INSERT INTO tasks (id, user_id, title, project_id) VALUES (?, ?, 'Đang ghi', ?)",
                inFlightTask, user.id(), project);

        Future<MvcResult> deletion = async(() -> delete(user, PROJECTS + "/" + project + "?version=1"));
        awaitWaitingOn("select%from projects%");
        assertThat(deletion.isDone()).as("xóa phải chờ khóa FOR SHARE").isFalse();
        competitor.commit();

        assertThat(deletion.get(15, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(204);
        Map<String, Object> row = taskRow(inFlightTask);
        assertThat(row.get("project_id")).as("cascade thấy Task vừa commit và gỡ nó").isNull();
        assertThat(row.get("version")).isEqualTo(2L);
        assertThat(changeLogs(user.id(), "task", inFlightTask, "UPDATE")).isEqualTo(1);
        assertThat(liveTasksReferencing("project_id", project)).isZero();
    }

    // ---------- Category

    @Test
    void categoryDeletionFirstMakesAConcurrentTaskCreateFailWith422() throws Exception {
        TestUser user = newUser();
        UUID category = category(user);
        competitorExecutes("SELECT 1 FROM categories WHERE id = ? FOR UPDATE", category);
        competitorExecutes("UPDATE categories SET deleted_at = now(), version = version + 1 WHERE id = ?", category);

        UUID taskId = UUID.randomUUID();
        Future<MvcResult> create = async(() -> createTask(user, taskId, "categoryId", category.toString()));
        awaitWaitingOn("select%from categories%");
        competitor.commit();

        MvcResult result = create.get(15, TimeUnit.SECONDS);
        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(result)).containsEntry("errorCode", "CATEGORY_NOT_FOUND");
        assertThat(liveTasksReferencing("category_id", category)).isZero();
    }

    @Test
    void taskWriteFirstIsCascadedByTheConcurrentCategoryDeletion() throws Exception {
        TestUser user = newUser();
        UUID category = category(user);
        UUID inFlightTask = UUID.randomUUID();
        competitorExecutes("SELECT 1 FROM categories WHERE id = ? FOR SHARE", category);
        competitorExecutes("INSERT INTO tasks (id, user_id, title, category_id) VALUES (?, ?, 'Đang ghi', ?)",
                inFlightTask, user.id(), category);

        Future<MvcResult> deletion = async(() -> delete(user, CATEGORIES + "/" + category + "?version=1"));
        awaitWaitingOn("select%from categories%");
        competitor.commit();

        assertThat(deletion.get(15, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(204);
        assertThat(taskRow(inFlightTask).get("category_id")).isNull();
        assertThat(changeLogs(user.id(), "task", inFlightTask, "UPDATE")).isEqualTo(1);
        assertThat(liveTasksReferencing("category_id", category)).isZero();
    }

    // ---------- Thứ tự khóa

    @Test
    void taskReferencesLockProjectBeforeCategory() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        UUID category = category(user);
        // Đối thủ đang xóa Category (giữ FOR UPDATE).
        competitorExecutes("SELECT 1 FROM categories WHERE id = ? FOR UPDATE", category);
        competitorExecutes("UPDATE categories SET deleted_at = now(), version = version + 1 WHERE id = ?", category);

        Future<MvcResult> create = async(() -> createTask(user, UUID.randomUUID(), "projectId", project.toString(),
                "categoryId", category.toString()));
        awaitWaitingOn("select%from categories%");
        // Request tạo Task đang chờ Category ⇒ nó ĐÃ giữ khóa FOR SHARE trên Project: xóa Project phải chờ.
        Future<MvcResult> projectDeletion = async(() -> delete(user, PROJECTS + "/" + project + "?version=1"));
        awaitWaitingOn("select%from projects%");
        assertThat(projectDeletion.isDone()).isFalse();

        competitor.commit();

        MvcResult created = create.get(15, TimeUnit.SECONDS);
        assertThat(created.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(created)).containsEntry("errorCode", "CATEGORY_NOT_FOUND");
        assertThat(projectDeletion.get(15, TimeUnit.SECONDS).getResponse().getStatus())
                .as("Task rollback nhả khóa Project; xóa Project chạy tiếp").isEqualTo(204);
    }

    // ---------- Cascade đụng Task đang bị sửa

    @Test
    void projectDeletionThatHitsAConcurrentlyEditedTaskRollsBackWith409() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        UUID task = task(user, "projectId", project.toString());
        // Đối thủ đóng vai một PATCH Task đang dở (đã tăng version, chưa commit).
        competitorExecutes("UPDATE tasks SET version = version + 1, title = 'Sửa đồng thời' WHERE id = ?", task);

        Future<MvcResult> deletion = async(() -> delete(user, PROJECTS + "/" + project + "?version=1"));
        awaitWaitingOn("update tasks%");
        competitor.commit();

        MvcResult result = deletion.get(15, TimeUnit.SECONDS);
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        // currentVersion là version của PROJECT (đường dẫn của request), dù xung đột thật nằm ở Task bị cascade —
        // hành vi đã biết, nêu trong báo cáo Giai đoạn 2. Client gửi lại với version hiện tại sẽ thành công.
        assertThat(readJson(result)).containsEntry("errorCode", "ENTITY_VERSION_CONFLICT")
                .containsEntry("currentVersion", 1);
        assertThat(jdbc.sql("SELECT deleted_at IS NULL FROM projects WHERE id = :p").param("p", project)
                .query(Boolean.class).single()).as("xóa Project rollback").isTrue();
        Map<String, Object> row = taskRow(task);
        assertThat(row.get("project_id")).isEqualTo(project);
        assertThat(row.get("title")).isEqualTo("Sửa đồng thời");
        assertThat(changeLogs(user.id(), "project", project, "DELETE")).isZero();

        assertThat(delete(user, PROJECTS + "/" + project + "?version=1").getResponse().getStatus())
                .as("gửi lại thì xóa và cascade được").isEqualTo(204);
        assertThat(taskRow(task).get("project_id")).isNull();
    }
}
