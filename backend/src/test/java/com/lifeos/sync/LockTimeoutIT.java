package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Hết lock_timeout → 503 + Retry-After cho REST và sync (D42, D85, D87). Context RIÊNG với lifeos.db.lock-timeout=1s
 * (D94) để mỗi lần chờ ngắn; giá trị mặc định 5s được khẳng định ở LockTimeoutDefaultIT. Một kết nối JDBC đóng vai giao
 * dịch đối thủ và giữ khóa dòng chưa commit trong suốt request, nên request chắc chắn hết thời gian chờ (xác định,
 * không sleep). Phủ cả bốn chỗ có thể chờ khóa: SELECT … FOR UPDATE, SELECT … FOR SHARE, flush của Hibernate (UPDATE
 * dòng nghiệp vụ) và câu upsert user_sync_state của ChangeRecorder (JdbcClient). SYN-17, SYN-45.
 */
@TestPropertySource(properties = "lifeos.db.lock-timeout=1s")
class LockTimeoutIT extends SyncTestBase {

    @Autowired
    DataSource dataSource;

    @Autowired
    MeterRegistry meters;

    private Connection competitor;

    @BeforeEach
    void open() throws Exception {
        competitor = dataSource.getConnection();
        competitor.setAutoCommit(false);
    }

    @AfterEach
    void close() throws Exception {
        if (!competitor.isClosed()) {
            competitor.rollback();
            competitor.close();
        }
    }

    private void hold(String sql, Object... params) throws Exception {
        try (PreparedStatement ps = competitor.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.execute();
        }
    }

    private static void assert503(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(503);
        assertThat(result.getResponse().getHeader("Retry-After")).isEqualTo("2");
        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"SERVICE_UNAVAILABLE\"")
                .doesNotContain("55P03").doesNotContain("lock timeout");
    }

    private double lockTimeouts() {
        return meters.counter("lifeos.sync.lock.timeouts").count();
    }

    @Test
    void contextUsesTheOverriddenLockTimeout() {
        assertThat(jdbc.sql("SHOW lock_timeout").query(String.class).single()).isEqualTo("1s");
    }

    @Test
    void restWaitingOnSelectForUpdateIs503() throws Exception {
        TestUser user = newUser();
        UUID project = createProject(user);
        hold("SELECT 1 FROM projects WHERE id = ? FOR UPDATE", project);

        assert503(delete(user, "/api/v1/projects/" + project + "?version=1"));
        competitor.rollback();
        assertThat(row("projects", project).get("deleted_at")).isNull();
    }

    @Test
    void restWaitingOnSelectForShareIs503() throws Exception {
        TestUser user = newUser();
        UUID project = createProject(user);
        hold("SELECT 1 FROM projects WHERE id = ? FOR UPDATE", project);

        assert503(post(user, "/api/v1/tasks", body("id", UUID.randomUUID().toString(), "title", "T",
                "projectId", project.toString())));
    }

    @Test
    void restWaitingAtFlushIs503() throws Exception {
        TestUser user = newUser();
        UUID project = createProject(user);
        hold("UPDATE projects SET description = 'đối thủ' WHERE id = ?", project);

        assert503(patch(user, "/api/v1/projects/" + project, body("version", 1, "name", "Mới")));
        competitor.rollback();
        assertThat(row("projects", project)).containsEntry("version", 1L);
    }

    @Test
    void restWaitingOnTheUserSyncStateRowIs503AndNothingIsWritten() throws Exception {
        TestUser user = newUser();
        UUID project = createProject(user);
        hold("UPDATE user_sync_state SET last_seq = last_seq WHERE user_id = ?", user.id());

        assert503(patch(user, "/api/v1/projects/" + project, body("version", 1, "name", "Mới")));
        competitor.rollback();
        assertThat(row("projects", project)).containsEntry("version", 1L);
    }

    @Test
    void Sync_17_lockTimeoutIs503AndResendingTheSameMutationIdSucceeds() throws Exception {
        TestUser user = newUser();
        UUID project = createProject(user);
        Map<String, Object> rename = mutation("project", project, "UPDATE", 1, payload("name", "Mới"));
        hold("SELECT 1 FROM projects WHERE id = ? FOR UPDATE", project);
        hold("UPDATE projects SET description = 'đối thủ' WHERE id = ?", project);
        double before = lockTimeouts();

        assert503(pushResult(user, rename));
        assertThat(lockTimeouts()).isEqualTo(before + 1);
        assertThat(syncMutations(user.id(), mutationIdOf(rename))).as("không lưu kết quả").isZero();

        competitor.rollback();
        assertThat(pushOne(user, rename)).containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
    }

    @Test
    void Sync_45_lockTimeoutMidBatchKeepsEarlierAppliedMutationsAndTheResentBatchCompletes() throws Exception {
        TestUser user = newUser();
        UUID free = UUID.randomUUID();
        UUID busy = createProject(user);
        Map<String, Object> first = mutation("project", free, "CREATE", 0, payload("name", "Trước"));
        Map<String, Object> blocked = mutation("project", busy, "UPDATE", 1, payload("name", "Bị chặn"));
        Map<String, Object> after = mutation("category", UUID.randomUUID(), "CREATE", 0, payload("name", "Sau"));
        // Giữ khóa dòng Project thứ hai: mutation 1 commit xong, mutation 2 hết lock_timeout lúc flush.
        hold("UPDATE projects SET description = 'đối thủ' WHERE id = ?", busy);
        double before = lockTimeouts();

        MvcResult result = pushResult(user, first, blocked, after);

        assert503(result);
        assertThat(lockTimeouts()).isEqualTo(before + 1);
        assertThat(row("projects", free)).containsEntry("version", 1L);
        assertThat(syncMutations(user.id(), mutationIdOf(first))).isEqualTo(1);
        assertThat(syncMutations(user.id(), mutationIdOf(blocked))).isZero();
        assertThat(syncMutations(user.id(), mutationIdOf(after))).as("dừng cả request").isZero();

        competitor.rollback();
        List<Map<String, Object>> resent = push(user, first, blocked, after);
        assertThat(resent).extracting(r -> r.get("status")).containsExactly("DUPLICATE", "APPLIED", "APPLIED");
    }
}
