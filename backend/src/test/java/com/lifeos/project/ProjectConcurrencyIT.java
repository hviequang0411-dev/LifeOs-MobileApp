package com.lifeos.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.lifeos.project.api.ProjectApi;
import com.lifeos.project.api.ProjectRef;
import com.lifeos.support.ApiTestBase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Các đường đồng thời của Project, kiểm theo cách XÁC ĐỊNH (một kết nối JDBC khác giữ khóa, Awaitility chờ đến khi
 * request thật sự đứng đợi khóa trong pg_stat_activity, có timeout cứng):
 * <ul>
 *   <li>@Version của Hibernate (qua VersionedWrites) → 409 ENTITY_VERSION_CONFLICT kèm currentVersion;</li>
 *   <li>hai lần tạo cùng id → 409 ENTITY_ALREADY_EXISTS (vi phạm khóa chính, D62);</li>
 *   <li>ProjectApi.lockActiveOwned (FOR SHARE) chặn xóa Project (FOR UPDATE) đến khi transaction kia kết thúc (D80).</li>
 * </ul>
 */
class ProjectConcurrencyIT extends ApiTestBase {

    private static final String BASE = "/api/v1/projects";

    @Autowired
    DataSource dataSource;

    @Autowired
    ProjectApi projectApi;

    @Autowired
    TransactionTemplate tx;

    private UUID create(TestUser user) throws Exception {
        UUID id = UUID.randomUUID();
        assertThat(post(user, BASE, body("id", id.toString(), "name", "Đồng thời")).getResponse().getStatus())
                .isEqualTo(201);
        return id;
    }

    private void awaitWaitingOnLock(String sqlPrefix) {
        await().atMost(Duration.ofSeconds(15)).until(() -> jdbc.sql("""
                SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND query ILIKE :q
                """).param("q", sqlPrefix + "%").query(Integer.class).single() > 0);
    }

    @Test
    void hibernateVersionConflictOnPatchIs409WithCurrentVersion() throws Exception {
        TestUser user = newUser();
        UUID id = create(user);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection competitor = dataSource.getConnection()) {
            competitor.setAutoCommit(false);
            try (PreparedStatement ps = competitor.prepareStatement(
                    "UPDATE projects SET version = version + 1, name = 'Thắng' WHERE id = ?")) {
                ps.setObject(1, id);
                ps.executeUpdate();
            }
            Future<MvcResult> patch = pool.submit(() -> patch(user, BASE + "/" + id,
                    body("version", 1, "name", "Thua")));
            awaitWaitingOnLock("update projects");
            competitor.commit();

            MvcResult result = patch.get(15, TimeUnit.SECONDS);
            assertThat(result.getResponse().getStatus()).isEqualTo(409);
            assertThat(readJson(result)).containsEntry("errorCode", "ENTITY_VERSION_CONFLICT")
                    .containsEntry("currentVersion", 2);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.sql("SELECT name FROM projects WHERE id = :id").param("id", id).query(String.class)
                .single()).isEqualTo("Thắng");
        assertThat(changeLogs(user.id(), "project", id, "UPDATE")).isZero();
    }

    @Test
    void concurrentCreateWithTheSameIdIsAlreadyExists() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID id = UUID.randomUUID();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection competitor = dataSource.getConnection()) {
            competitor.setAutoCommit(false);
            try (PreparedStatement ps = competitor.prepareStatement(
                    "INSERT INTO projects (id, user_id, name) VALUES (?, ?, 'Đến trước')")) {
                ps.setObject(1, id);
                ps.setObject(2, other.id());
                ps.executeUpdate();
            }
            Future<MvcResult> create = pool.submit(() -> post(user, BASE, body("id", id.toString(), "name", "Sau")));
            awaitWaitingOnLock("insert into projects");
            competitor.commit();

            MvcResult result = create.get(15, TimeUnit.SECONDS);
            assertThat(result.getResponse().getStatus()).isEqualTo(409);
            assertThat(readJson(result)).containsEntry("errorCode", "ENTITY_ALREADY_EXISTS");
        } finally {
            pool.shutdownNow();
        }
        assertThat(changeLogs(user.id(), "project", id)).isZero();
    }

    @Test
    void sharedLockFromProjectApiBlocksDeletionUntilItsTransactionEnds() throws Exception {
        TestUser user = newUser();
        UUID id = create(user);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.CountDownLatch locked = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
            Future<Optional<ProjectRef>> holder = pool.submit(() -> tx.execute(status -> {
                Optional<ProjectRef> ref = projectApi.lockActiveOwned(user.id(), id);
                locked.countDown();
                try {
                    release.await(15, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return ref;
            }));
            assertThat(locked.await(15, TimeUnit.SECONDS)).isTrue();

            Future<MvcResult> deletion = pool.submit(() -> delete(user, BASE + "/" + id + "?version=1"));
            awaitWaitingOnLock("select%from projects");
            assertThat(deletion.isDone()).as("xóa phải chờ khóa FOR SHARE").isFalse();

            release.countDown();
            assertThat(holder.get(15, TimeUnit.SECONDS)).contains(new ProjectRef(id, false));
            assertThat(deletion.get(15, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(204);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void projectApiHidesForeignAndDeletedProjectsAndRequiresATransactionToLock() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        UUID active = create(alice);
        UUID archived = create(alice);
        UUID deleted = create(alice);
        post(alice, BASE + "/" + archived + "/archive", body("version", 1));
        delete(alice, BASE + "/" + deleted + "?version=1");

        assertThat(projectApi.findActiveOwned(alice.id(), active)).contains(new ProjectRef(active, false));
        assertThat(projectApi.findActiveOwned(alice.id(), archived)).contains(new ProjectRef(archived, true));
        assertThat(projectApi.findActiveOwned(alice.id(), deleted)).isEmpty();
        assertThat(projectApi.findActiveOwned(bob.id(), active)).isEmpty();
        assertThat(projectApi.findActiveOwned(alice.id(), UUID.randomUUID())).isEmpty();
        Optional<ProjectRef> foreignLock = tx.execute(s -> projectApi.lockActiveOwned(bob.id(), active));
        assertThat(foreignLock).isEmpty();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> projectApi.lockActiveOwned(alice.id(), active))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
