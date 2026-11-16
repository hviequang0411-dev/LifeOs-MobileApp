package com.lifeos.goal;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.test.web.servlet.MvcResult;

/**
 * KNW-13 (PHASE_6 §9; D86, D186): xóa Goal đồng thời với sửa/gắn lại Task và gắn note_link tới Goal — không deadlock,
 * không lỗi 500 (LockOrderGuard), dữ liệu nhất quán: Goal đã xóa thì không còn Task/liên kết sống trỏ tới nó, Task không
 * bị xóa, change_logs mới nhất của Task khớp version. Lặp 20 lần.
 */
class GoalCascadeConcurrencyIT extends ApiTestBase {

    private ExecutorService pool;

    @BeforeEach
    void open() {
        pool = Executors.newFixedThreadPool(3);
    }

    @AfterEach
    void close() {
        pool.shutdownNow();
    }

    private UUID created(MvcResult r) throws Exception {
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return UUID.fromString((String) readJson(r).get("id"));
    }

    private Future<MvcResult> async(CountDownLatch go, Callable<MvcResult> request) {
        return pool.submit(() -> {
            go.await();
            return request.call();
        });
    }

    @RepeatedTest(20)
    void Knw_13_goalDeletionRacingTaskWritesAndNoteLinksStaysConsistent() throws Exception {
        TestUser user = newUser();
        UUID goal = created(post(user, "/api/v1/goals", body("id", UUID.randomUUID().toString(), "title", "G")));
        UUID m1 = created(post(user, "/api/v1/goals/" + goal + "/milestones", body("id", UUID.randomUUID().toString(),
                "title", "M1")));
        UUID m2 = created(post(user, "/api/v1/goals/" + goal + "/milestones", body("id", UUID.randomUUID().toString(),
                "title", "M2")));
        UUID edited = created(post(user, "/api/v1/tasks", body("id", UUID.randomUUID().toString(), "title", "A",
                "milestoneId", m1.toString())));
        UUID relinked = created(post(user, "/api/v1/tasks", body("id", UUID.randomUUID().toString(), "title", "B",
                "goalId", goal.toString())));
        UUID note = created(post(user, "/api/v1/notes", body("id", UUID.randomUUID().toString(), "content", "n")));

        CountDownLatch go = new CountDownLatch(1);
        Future<MvcResult> deletion = async(go, () -> delete(user, "/api/v1/goals/" + goal + "?version=1"));
        Future<MvcResult> edit = async(go, () -> patch(user, "/api/v1/tasks/" + edited, body("version", 1,
                "title", "A2")));
        Future<MvcResult> relink = async(go, () -> patch(user, "/api/v1/tasks/" + relinked, body("version", 1,
                "milestoneId", m2.toString())));
        go.countDown();
        Future<MvcResult> link = pool.submit(() -> post(user, "/api/v1/notes/" + note + "/links", body("id",
                UUID.randomUUID().toString(), "targetType", "GOAL", "targetId", goal.toString())));

        int deleted = deletion.get(20, TimeUnit.SECONDS).getResponse().getStatus();
        int edit1 = edit.get(20, TimeUnit.SECONDS).getResponse().getStatus();
        int edit2 = relink.get(20, TimeUnit.SECONDS).getResponse().getStatus();
        int linked = link.get(20, TimeUnit.SECONDS).getResponse().getStatus();
        assertThat(deleted).isIn(204, 409);
        assertThat(edit1).isIn(200, 409);
        assertThat(edit2).isIn(200, 409, 422);
        assertThat(linked).isIn(201, 422);

        boolean goalGone = jdbc.sql("SELECT deleted_at IS NOT NULL FROM goals WHERE id = :id").param("id", goal)
                .query(Boolean.class).single();
        assertThat(goalGone).isEqualTo(deleted == 204);
        if (goalGone) {
            assertThat(jdbc.sql("SELECT count(*) FROM tasks WHERE deleted_at IS NULL AND (goal_id = :g"
                    + " OR milestone_id IN (:m1, :m2))").param("g", goal).param("m1", m1).param("m2", m2)
                    .query(Integer.class).single()).as("không có Task sống trỏ tới Goal đã xóa").isZero();
            assertThat(jdbc.sql("SELECT count(*) FROM note_links WHERE deleted_at IS NULL AND target_id = :g")
                    .param("g", goal).query(Integer.class).single()).as("không có liên kết sống tới Goal đã xóa")
                    .isZero();
            assertThat(jdbc.sql("SELECT count(*) FROM milestones WHERE goal_id = :g AND deleted_at IS NULL")
                    .param("g", goal).query(Integer.class).single()).isZero();
        }
        for (UUID t : new UUID[] {edited, relinked}) {
            Map<String, Object> row = jdbc.sql("SELECT version, deleted_at FROM tasks WHERE id = :id").param("id", t)
                    .query().singleRow();
            assertThat(row.get("deleted_at")).as("Task không bao giờ bị xóa theo Goal").isNull();
            assertThat(jdbc.sql("SELECT entity_version FROM change_logs WHERE entity_id = :id ORDER BY seq DESC"
                    + " LIMIT 1").param("id", t).query(Long.class).single()).as("change_logs khớp version")
                    .isEqualTo(((Number) row.get("version")).longValue());
        }
    }
}
