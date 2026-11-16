package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.sync.service.SyncConsistencyService;
import com.lifeos.sync.service.SyncRetentionService;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/** Đồng bộ nâng cao phía server (PHASE_5 SYN-A01, A05..A08, A16, A17, A19). */
class SyncAdvancedIT extends SyncTestBase {

    @Autowired
    SyncRetentionService retention;

    @Autowired
    SyncConsistencyService consistency;

    private void old(String table, String column, String where, Object... params) {
        String sql = "UPDATE " + table + " SET " + column + " = :old WHERE " + where;
        var spec = jdbc.sql(sql).param("old", clock.instant().minus(Duration.ofDays(200)).atOffset(ZoneOffset.UTC));
        for (int i = 0; i < params.length; i += 2) {
            spec = spec.param((String) params[i], params[i + 1]);
        }
        spec.update();
    }

    private void seedChangeLogs(UUID userId, int count) {
        jdbc.sql("INSERT INTO user_sync_state (user_id, last_seq) VALUES (:u, :n) ON CONFLICT (user_id) DO UPDATE"
                + " SET last_seq = EXCLUDED.last_seq").param("u", userId).param("n", count).update();
        jdbc.sql("INSERT INTO change_logs (user_id, seq, entity_type, entity_id, operation, entity_version, created_at)"
                        + " SELECT :u, g, 'task', gen_random_uuid(), 'CREATE', 1, now() FROM generate_series(1, :n) g")
                .param("u", userId).param("n", count).update();
    }

    private long minRetained(UUID userId) {
        return jdbc.sql("SELECT min_retained_seq FROM user_sync_state WHERE user_id = :u").param("u", userId)
                .query(Long.class).single();
    }

    @Test
    void SynA_01_SynA_06_changeLogRetentionKeepsTheLatest1000AndOldCursorsGet410() throws Exception {
        TestUser user = newUser();
        seedChangeLogs(user.id(), 1500);
        old("change_logs", "created_at", "user_id = :u AND seq <= 1200", "u", user.id());
        retention.runChangeLogs();
        assertThat(minRetained(user.id())).as("giữ ≥ 1000 seq gần nhất").isEqualTo(500);
        assertThat(jdbc.sql("SELECT min(seq) FROM change_logs WHERE user_id = :u").param("u", user.id())
                .query(Long.class).single()).isEqualTo(501);
        MvcResult gone = get(user, "/api/v1/sync/pull?cursor=" + cursor(499));
        assertThat(gone.getResponse().getStatus()).isEqualTo(410);
        assertThat(readJson(gone)).containsEntry("errorCode", "SYNC_FULL_RESYNC_REQUIRED")
                .containsEntry("minRetainedSeq", 500).containsEntry("currentSeq", 1500);
        assertThat(get(user, "/api/v1/sync/pull?cursor=" + cursor(500)).getResponse().getStatus()).isEqualTo(200);

        TestUser fresh = newUser();
        seedChangeLogs(fresh.id(), 300);
        old("change_logs", "created_at", "user_id = :u", "u", fresh.id());
        retention.runChangeLogs();
        assertThat(minRetained(fresh.id())).as("ít hơn 1000 seq: không dọn").isZero();
        TestUser recent = newUser();
        seedChangeLogs(recent.id(), 1500);
        retention.runChangeLogs();
        assertThat(minRetained(recent.id())).as("chưa quá 180 ngày: không dọn").isZero();
    }

    private String cursor(long seq) {
        return com.lifeos.sync.service.SyncCursorCodec.encode(seq);
    }

    @Test
    void SynA_07_oldMutationIdsArePurgedAndAReplayedCreateIsRejectedAsAlreadyExisting() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        Map<String, Object> create = mutation("project", id, "CREATE", 0, payload("name", "Dự án " + id));
        assertThat(pushOne(user, create)).containsEntry("status", "APPLIED");
        old("sync_mutations", "created_at", "user_id = :u", "u", user.id());
        retention.runMutations();
        assertThat(syncMutations(user.id(), mutationIdOf(create))).isZero();
        assertThat(pushOne(user, create)).containsEntry("status", "REJECTED")
                .containsEntry("errorCode", "ENTITY_ALREADY_EXISTS");
    }

    @Test
    void SynA_08_tombstonesArePurgedChildFirstOnlyWhenUnreferencedAndForgotten() throws Exception {
        TestUser user = newUser();
        UUID task = UUID.randomUUID();
        pushOne(user, mutation("task", task, "CREATE", 0, payload("title", "Cũ")));
        UUID block = UUID.randomUUID();
        pushOne(user, mutation("time_block", block, "CREATE", 0, payload("taskId", task.toString(), "startAt",
                "2027-01-01T01:00:00Z", "endAt", "2027-01-01T02:00:00Z")));
        pushOne(user, mutation("task", task, "DELETE", 1, null));
        UUID project = UUID.randomUUID();
        pushOne(user, mutation("project", project, "CREATE", 0, payload("name", "Còn nhớ")));
        pushOne(user, mutation("project", project, "DELETE", 1, null));
        old("tasks", "deleted_at", "id = :id", "id", task);
        old("time_blocks", "deleted_at", "id = :id", "id", block);
        old("projects", "deleted_at", "id = :id", "id", project);
        // Thiết bị hợp lệ không còn cần: change_logs của task/block đã bị dọn; của project thì chưa.
        jdbc.sql("DELETE FROM change_logs WHERE user_id = :u AND entity_id IN (:ids)").param("u", user.id())
                .param("ids", List.of(task, block)).update();

        UUID account = UUID.randomUUID();
        pushOne(user, mutation("financial_account", account, "CREATE", 0, payload("name", "Ví", "accountType", "CASH",
                "currency", "VND")));
        UUID recurring = UUID.randomUUID();
        UUID generated = UUID.randomUUID();
        jdbc.sql("INSERT INTO recurring_transactions (id, user_id, account_id, kind, amount, currency, frequency,"
                + " start_on, next_run_on) VALUES (:r, :u, :a, 'EXPENSE', 1000, 'VND', 'DAILY', DATE '2026-01-01',"
                + " DATE '2026-01-02')").param("r", recurring).param("u", user.id()).param("a", account).update();
        jdbc.sql("INSERT INTO financial_transactions (id, user_id, account_id, kind, amount, currency, occurred_on,"
                + " recurring_transaction_id, recurrence_date, deleted_at) VALUES (:t, :u, :a, 'EXPENSE', 1000, 'VND',"
                + " DATE '2026-01-01', :r, DATE '2026-01-01', :old)").param("t", generated).param("u", user.id())
                .param("a", account).param("r", recurring)
                .param("old", clock.instant().minus(Duration.ofDays(200)).atOffset(ZoneOffset.UTC)).update();

        retention.runTombstones();
        assertThat(count("tasks", task)).as("task: con đã dọn trước trong cùng lượt").isZero();
        assertThat(count("time_blocks", block)).isZero();
        assertThat(count("projects", project)).as("còn change_logs → thiết bị có thể cần").isOne();
        assertThat(count("financial_transactions", generated)).as("khóa UNIQUE định kỳ còn tham chiếu").isOne();
        assertThat(count("financial_accounts", account)).isOne();
    }

    private int count(String table, UUID id) {
        return jdbc.sql("SELECT count(*) FROM " + table + " WHERE id = :id").param("id", id).query(Integer.class)
                .single();
    }

    @Test
    @SuppressWarnings("unchecked")
    void SynA_05_snapshotPagesCoverLiveStateAndPullFromSnapshotSeqFillsLaterWrites() throws Exception {
        TestUser user = newUser();
        Set<UUID> live = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            live.add(createProject(user));
        }
        UUID gone = createProject(user);
        pushOne(user, mutation("project", gone, "DELETE", 1, null));
        live.add(createCategory(user));
        Set<UUID> seen = new HashSet<>();
        String token = null;
        String next = null;
        long snapshotSeq = -1;
        int pages = 0;
        do {
            Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("limit", 2);
            if (token != null) {
                body.put("snapshotToken", token);
                body.put("next", next);
            }
            MvcResult r = post(user, "/api/v1/sync/snapshot", json.writeValueAsString(body));
            assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
            Map<String, Object> page = readJson(r);
            token = (String) page.get("snapshotToken");
            next = (String) page.get("next");
            long seq = ((Number) page.get("snapshotSeq")).longValue();
            if (snapshotSeq >= 0) {
                assertThat(seq).as("cùng snapshotSeq mọi trang").isEqualTo(snapshotSeq);
            }
            snapshotSeq = seq;
            for (Map<String, Object> entity : (List<Map<String, Object>>) page.get("entities")) {
                for (Map<String, Object> item : (List<Map<String, Object>>) entity.get("items")) {
                    assertThat(seen.add(UUID.fromString((String) item.get("entityId")))).as("không trùng").isTrue();
                    assertThat(item).containsKey("data").containsEntry("deleted", false);
                }
            }
            if (pages++ == 0) {
                // Ghi đồng thời sau snapshotSeq: đến qua pull, không cần có trong snapshot.
                live.add(createProject(user));
            }
        } while (next != null);
        assertThat(seen).doesNotContain(gone);
        Set<UUID> afterSnapshot = new HashSet<>(seen);
        Map<String, Object> pulled = readJson(get(user, "/api/v1/sync/pull?limit=500&cursor=" + cursor(snapshotSeq)));
        for (Map<String, Object> change : (List<Map<String, Object>>) pulled.get("changes")) {
            afterSnapshot.add(UUID.fromString((String) change.get("entityId")));
        }
        assertThat(afterSnapshot).containsAll(live);
    }

    @Test
    void SynA_19_snapshotTokensEntitiesAndDiagnosticsAreScopedToTheUser() throws Exception {
        TestUser owner = newUser();
        TestUser other = newUser();
        UUID project = createProject(owner);
        Map<String, Object> first = readJson(post(owner, "/api/v1/sync/snapshot", "{\"limit\":1}"));
        MvcResult stolen = post(other, "/api/v1/sync/snapshot", body("snapshotToken", first.get("snapshotToken"),
                "next", first.get("next")));
        assertThat(stolen.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(get(owner, "/api/v1/sync/entities/project/" + project))).containsEntry("version", 1);
        MvcResult foreign = get(other, "/api/v1/sync/entities/project/" + project);
        MvcResult missing = get(other, "/api/v1/sync/entities/project/" + UUID.randomUUID());
        assertThat(foreign.getResponse().getStatus()).isEqualTo(404);
        assertThat(problem(foreign)).isEqualTo(problem(missing));
        assertThat(get(owner, "/api/v1/sync/entities/nope/" + project).getResponse().getStatus()).isEqualTo(400);
        pushOne(owner, mutation("project", project, "DELETE", 1, null));
        assertThat(readJson(get(owner, "/api/v1/sync/entities/project/" + project))).containsEntry("deleted", true);
        clock.advance(Duration.ofMinutes(31));
        TestUser renewed = new TestUser(owner.id(), jwtService.issue(owner.id(), UUID.randomUUID()).token());
        MvcResult expired = post(renewed, "/api/v1/sync/snapshot", body("snapshotToken", first.get("snapshotToken"),
                "next", first.get("next")));
        assertThat(expired.getResponse().getStatus()).isEqualTo(410);
        assertThat(readJson(expired)).containsEntry("errorCode", "SYNC_SNAPSHOT_EXPIRED");
    }

    @Test
    void SynA_16_diagnosticsAcceptOnlyMetadataAndAreRateLimited() throws Exception {
        TestUser user = newUser();
        String ok = json.writeValueAsString(Map.of("appVersion", "1.4.0", "cursor", "djE6MTI", "mutationCounts",
                Map.of("pending", 3, "conflict", 1), "errors", List.of(Map.of("code", "VALIDATION_FAILED",
                        "entityType", "task", "at", "2026-11-07T00:00:00Z")), "integrity", Map.of("quickCheck", true)));
        assertThat(post(user, "/api/v1/sync/diagnostics", ok).getResponse().getStatus()).isEqualTo(202);
        MvcResult withData = post(user, "/api/v1/sync/diagnostics", json.writeValueAsString(Map.of("errors",
                List.of(Map.of("code", "X", "title", "Nội dung riêng")))));
        assertThat(withData.getResponse().getStatus()).isEqualTo(400);
        assertThat(post(user, "/api/v1/sync/diagnostics", "{\"payload\":{}}").getResponse().getStatus())
                .isEqualTo(400);
        // Mỗi lần gửi (kể cả bị từ chối) tính vào hạn mức 5/giờ.
        assertThat(post(user, "/api/v1/sync/diagnostics", ok).getResponse().getStatus()).isEqualTo(202);
        assertThat(post(user, "/api/v1/sync/diagnostics", ok).getResponse().getStatus()).isEqualTo(202);
        assertThat(post(user, "/api/v1/sync/diagnostics", ok).getResponse().getStatus()).isEqualTo(429);
        assertThat(jdbc.sql("SELECT count(*) FROM sync_diagnostics WHERE user_id = :u").param("u", user.id())
                .query(Integer.class).single()).isEqualTo(3);
        assertThat(jdbc.sql("SELECT payload::text FROM sync_diagnostics WHERE user_id = :u LIMIT 1")
                .param("u", user.id()).query(String.class).single()).doesNotContain("Nội dung riêng");
        old("sync_diagnostics", "created_at", "user_id = :u", "u", user.id());
        retention.runDiagnostics();
        assertThat(jdbc.sql("SELECT count(*) FROM sync_diagnostics WHERE user_id = :u").param("u", user.id())
                .query(Integer.class).single()).isZero();
    }

    @Test
    void SynA_17_consistencyCheckDetectsPlantedViolationsWithoutWriting() throws Exception {
        Map<String, Long> before = consistency.run();
        TestUser user = newUser();
        UUID project = createProject(user);
        jdbc.sql("UPDATE projects SET version = 7 WHERE id = :id").param("id", project).update();
        jdbc.sql("UPDATE user_sync_state SET last_seq = 0, min_retained_seq = 0 WHERE user_id = :u")
                .param("u", user.id()).update();
        jdbc.sql("UPDATE change_logs SET mutation_id = :m WHERE user_id = :u").param("m", UUID.randomUUID())
                .param("u", user.id()).update();
        long projectVersion = jdbc.sql("SELECT version FROM projects WHERE id = :id").param("id", project)
                .query(Long.class).single();
        Map<String, Long> after = consistency.run();
        assertThat(after.get("version")).isEqualTo(before.get("version") + 1);
        assertThat(after.get("seq")).isEqualTo(before.get("seq") + 1);
        assertThat(after.get("mutation")).isGreaterThan(before.get("mutation"));
        assertThat(jdbc.sql("SELECT version FROM projects WHERE id = :id").param("id", project).query(Long.class)
                .single()).as("không tự sửa").isEqualTo(projectVersion);
        // Dọn để không ảnh hưởng test khác dùng chung DB.
        jdbc.sql("UPDATE projects SET version = 1 WHERE id = :id").param("id", project).update();
        jdbc.sql("UPDATE user_sync_state SET last_seq = (SELECT max(seq) FROM change_logs WHERE user_id = :u)"
                + " WHERE user_id = :u").param("u", user.id()).update();
        jdbc.sql("UPDATE change_logs SET mutation_id = NULL WHERE user_id = :u").param("u", user.id()).update();
        assertThat(new ArrayList<>(after.keySet())).containsExactly("seq", "version", "mutation");
        assertThat(Instant.now()).isNotNull();
    }
}
