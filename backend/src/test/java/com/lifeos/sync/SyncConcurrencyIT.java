package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Đồng thời qua sync (SYNC_RULES §9.3, SYNC_PROTOCOL §5.2, D86; TESTING_RULES §6): SYN-11, SYN-12, SYN-32, SYN-44.
 * Hai request THẬT chạy song song, khởi động cùng lúc bằng CountDownLatch, timeout cứng; mỗi lần lặp một user mới.
 * Lặp 20 lần (@RepeatedTest) vì thứ tự xen kẽ thay đổi giữa các lần. "Không deadlock" = không request nào nhận 503
 * (40P01/55P03 đều ánh xạ 503) và không request nào treo quá timeout.
 */
class SyncConcurrencyIT extends SyncTestBase {

    private static final int RUNS = 20;

    @Autowired
    DataSource dataSource;

    private ExecutorService pool;

    @BeforeEach
    void startPool() {
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void stopPool() {
        pool.shutdownNow();
    }

    /** Chạy hai request cùng lúc; trả kết quả theo thứ tự truyền vào. */
    private List<MvcResult> together(Callable<MvcResult> first, Callable<MvcResult> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<MvcResult>> futures = new ArrayList<>();
        for (Callable<MvcResult> request : List.of(first, second)) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                return request.call();
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        go.countDown();
        List<MvcResult> results = new ArrayList<>();
        for (Future<MvcResult> future : futures) {
            MvcResult result = future.get(30, TimeUnit.SECONDS);
            assertThat(result.getResponse().getStatus()).as("không 503 (deadlock/hết khóa): "
                    + result.getResponse().getContentAsString()).isEqualTo(200);
            results.add(result);
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> only(MvcResult result) throws Exception {
        return ((List<Map<String, Object>>) readJson(result).get("results")).get(0);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> all(MvcResult result) throws Exception {
        return (List<Map<String, Object>>) readJson(result).get("results");
    }

    /** seq của user là 1..last_seq, liên tục, không trùng (I2). */
    private void assertSeqContiguous(UUID userId) {
        List<Long> seqs = jdbc.sql("SELECT seq FROM change_logs WHERE user_id = :u ORDER BY seq")
                .param("u", userId).query(Long.class).list();
        long last = jdbc.sql("SELECT last_seq FROM user_sync_state WHERE user_id = :u").param("u", userId)
                .query(Long.class).single();
        assertThat(seqs).hasSize((int) last);
        for (int i = 0; i < seqs.size(); i++) {
            assertThat(seqs.get(i)).isEqualTo(i + 1L);
        }
    }

    private void assertNoOrphans(UUID userId) {
        assertThat(jdbc.sql("""
                SELECT count(*) FROM tasks t
                WHERE t.user_id = :u AND t.deleted_at IS NULL
                  AND (EXISTS (SELECT 1 FROM projects p WHERE p.id = t.project_id AND p.deleted_at IS NOT NULL)
                    OR EXISTS (SELECT 1 FROM categories c WHERE c.id = t.category_id AND c.deleted_at IS NOT NULL))
                """).param("u", userId).query(Integer.class).single()).as("Task mồ côi").isZero();
    }

    private TestUser secondDevice(TestUser user) {
        return new TestUser(user.id(), jwtService.issue(user.id(), UUID.randomUUID()).token());
    }

    // ---------- SYN-11

    @RepeatedTest(RUNS)
    void Sync_11_twoDevicesOfOneUserPushConcurrentlyWithUniqueContiguousSeq() throws Exception {
        TestUser phone = newUser();
        TestUser tablet = secondDevice(phone);
        List<Map<String, Object>> a = new ArrayList<>();
        List<Map<String, Object>> b = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            a.add(mutation("project", UUID.randomUUID(), "CREATE", 0, payload("name", "A" + i)));
            b.add(mutation("task", UUID.randomUUID(), "CREATE", 0, payload("title", "B" + i)));
        }

        List<MvcResult> results = together(
                () -> post(phone, PUSH, json.writeValueAsString(Map.of("mutations", a))),
                () -> post(tablet, PUSH, json.writeValueAsString(Map.of("mutations", b))));

        for (MvcResult result : results) {
            assertThat(all(result)).allSatisfy(r -> assertThat(r).containsEntry("status", "APPLIED"));
        }
        assertSeqContiguous(phone.id());
        assertThat(changeLogsOfUser(phone.id())).isEqualTo(10);
    }

    // ---------- SYN-12 (xác định: khóa của user A đang bị giữ, user B vẫn ghi được ngay)

    @Test
    void Sync_12_aWriterHoldingOneUsersSeqLockDoesNotBlockAnotherUser() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();
        createProject(a);
        try (Connection holder = dataSource.getConnection()) {
            holder.setAutoCommit(false);
            try (PreparedStatement ps = holder.prepareStatement(
                    "UPDATE user_sync_state SET last_seq = last_seq WHERE user_id = ?")) {
                ps.setObject(1, a.id());
                ps.executeUpdate();
            }
            Future<MvcResult> blocked = pool.submit(() -> pushResult(a,
                    mutation("project", UUID.randomUUID(), "CREATE", 0, payload("name", "A chờ"))));
            await().atMost(Duration.ofSeconds(4)).until(() -> jdbc.sql("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE wait_event_type = 'Lock' AND query ILIKE 'insert into user_sync_state%'""")
                    .query(Integer.class).single() > 0);

            long started = System.nanoTime();
            assertThat(pushOne(b, mutation("project", UUID.randomUUID(), "CREATE", 0, payload("name", "B"))))
                    .containsEntry("status", "APPLIED");
            assertThat((System.nanoTime() - started) / 1_000_000).as("B không chờ khóa của A").isLessThan(3_000);
            assertThat(blocked.isDone()).as("A vẫn đang chờ").isFalse();

            holder.rollback();
            assertThat(only(blocked.get(10, TimeUnit.SECONDS))).containsEntry("status", "APPLIED");
        }
    }

    // ---------- SYN-32

    @RepeatedTest(RUNS)
    void Sync_32_twoParallelRequestsWithTheSameMutationIdApplyExactlyOnce() throws Exception {
        TestUser phone = newUser();
        TestUser retry = secondDevice(phone);
        UUID id = UUID.randomUUID();
        Map<String, Object> m = mutation("project", id, "CREATE", 0, payload("name", "Một lần"));

        List<MvcResult> results = together(() -> pushResult(phone, m), () -> pushResult(retry, m));

        List<String> statuses = new ArrayList<>();
        for (MvcResult result : results) {
            Map<String, Object> r = only(result);
            statuses.add((String) r.get("status"));
            if ("DUPLICATE".equals(r.get("status"))) {
                assertThat(r).containsEntry("originalStatus", "APPLIED").containsEntry("entityVersion", 1);
            }
        }
        assertThat(statuses).containsExactlyInAnyOrder("APPLIED", "DUPLICATE");
        assertThat(changeLogRows(phone.id(), id)).hasSize(1);
        assertThat(syncMutations(phone.id(), mutationIdOf(m))).isEqualTo(1);
    }

    @RepeatedTest(RUNS)
    void Sync_32_sameMutationIdUpdateInParallelAppliesOnce() throws Exception {
        TestUser phone = newUser();
        TestUser retry = secondDevice(phone);
        UUID id = createProject(phone);
        Map<String, Object> m = mutation("project", id, "UPDATE", 1, payload("name", "Đổi một lần"));

        List<MvcResult> results = together(() -> pushResult(phone, m), () -> pushResult(retry, m));

        assertThat(List.of(only(results.get(0)).get("status"), only(results.get(1)).get("status")))
                .containsExactlyInAnyOrder("APPLIED", "DUPLICATE");
        assertThat(row("projects", id)).containsEntry("version", 2L);
        assertThat(changeLogRows(phone.id(), id)).hasSize(2);
    }

    // ---------- SYN-44: cascade xóa Project song song với ghi Task

    @RepeatedTest(RUNS)
    void Sync_44a_projectDeletionVersusEditingATaskOfTheProject() throws Exception {
        TestUser phone = newUser();
        TestUser tablet = secondDevice(phone);
        UUID project = createProject(phone);
        UUID task = UUID.randomUUID();
        pushOne(phone, mutation("task", task, "CREATE", 0, payload("title", "T", "projectId", project.toString())));

        List<MvcResult> results = together(
                () -> pushResult(phone, mutation("project", project, "DELETE", 1, null)),
                () -> pushResult(tablet, mutation("task", task, "UPDATE", 1, payload("title", "Sửa"))));

        String deletion = (String) only(results.get(0)).get("status");
        String edit = (String) only(results.get(1)).get("status");
        assertThat(deletion).isIn("APPLIED", "CONFLICT");
        assertThat(edit).isIn("APPLIED", "CONFLICT");
        assertThat(List.of(deletion, edit)).as("ít nhất một bên thành công").contains("APPLIED");
        assertNoOrphans(phone.id());
        assertSeqContiguous(phone.id());
    }

    @RepeatedTest(RUNS)
    void Sync_44b_projectDeletionVersusCreatingATaskThatReferencesIt() throws Exception {
        TestUser phone = newUser();
        TestUser tablet = secondDevice(phone);
        UUID project = createProject(phone);
        UUID task = UUID.randomUUID();

        List<MvcResult> results = together(
                () -> pushResult(phone, mutation("project", project, "DELETE", 1, null)),
                () -> pushResult(tablet, mutation("task", task, "CREATE", 0,
                        payload("title", "Mới", "projectId", project.toString()))));

        assertThat(only(results.get(0))).containsEntry("status", "APPLIED");
        Map<String, Object> created = only(results.get(1));
        if ("REJECTED".equals(created.get("status"))) {
            assertThat(created).containsEntry("errorCode", "PROJECT_NOT_FOUND");
        } else {
            assertThat(created).containsEntry("status", "APPLIED");
            assertThat(row("tasks", task).get("project_id")).as("cascade gỡ Task vừa tạo").isNull();
        }
        assertNoOrphans(phone.id());
        assertSeqContiguous(phone.id());
    }

    @RepeatedTest(RUNS)
    void Sync_44c_projectDeletionVersusChangingTheCategoryOfATaskInTheProject() throws Exception {
        TestUser phone = newUser();
        TestUser tablet = secondDevice(phone);
        UUID project = createProject(phone);
        UUID category = createCategory(phone);
        UUID task = UUID.randomUUID();
        pushOne(phone, mutation("task", task, "CREATE", 0, payload("title", "T", "projectId", project.toString())));

        List<MvcResult> results = together(
                () -> pushResult(phone, mutation("project", project, "DELETE", 1, null)),
                () -> pushResult(tablet, mutation("task", task, "UPDATE", 1,
                        payload("categoryId", category.toString()))));

        String deletion = (String) only(results.get(0)).get("status");
        String edit = (String) only(results.get(1)).get("status");
        assertThat(deletion).isIn("APPLIED", "CONFLICT");
        assertThat(edit).isIn("APPLIED", "CONFLICT");
        assertThat(List.of(deletion, edit)).contains("APPLIED");
        assertNoOrphans(phone.id());
        assertSeqContiguous(phone.id());
    }
}
