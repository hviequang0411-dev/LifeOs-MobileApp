package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * POST /sync/push với entityType task (bước 3b, SYNC_PROTOCOL §3.1, §4): ma trận kết quả, payload, tham chiếu, cùng
 * validation với REST (SYN-34) và phân biệt NotFound của entity đích với *_NOT_FOUND 422 của tham chiếu (Q14, D94).
 */
class SyncPushTaskIT extends SyncTestBase {

    private static final String TASKS = "/api/v1/tasks";

    private Map<String, Object> createTask(UUID id, Object... payloadKeyValues) {
        return mutation("task", id, "CREATE", 0, payload(payloadKeyValues));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(Map<String, Object> result) {
        return (Map<String, Object>) ((Map<String, Object>) result.get("serverSnapshot")).get("data");
    }

    @Test
    void Sync_01_createTaskAppliesWithDefaultsVersion1AndOneChangeLog() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        Map<String, Object> m = createTask(id, "title", "  Mua sữa  ", "description", null, "dueDate", "2026-11-06",
                "dueTime", null, "projectId", null, "categoryId", null);
        long seqBefore = lastSeq(user.id());

        assertThat(pushOne(user, m)).containsEntry("status", "APPLIED").containsEntry("entityVersion", 1);

        Map<String, Object> task = row("tasks", id);
        assertThat(task).containsEntry("title", "Mua sữa").containsEntry("status", "OPEN")
                .containsEntry("priority", "NONE").containsEntry("version", 1L);
        assertThat(changeLogRows(user.id(), id)).singleElement().satisfies(log -> assertThat(log)
                .containsEntry("operation", "CREATE").containsEntry("mutation_id", mutationIdOf(m)));
        assertThat(lastSeq(user.id())).isEqualTo(seqBefore + 1);
    }

    @Test
    void Sync_03_updateWithMatchingVersionAndServerSetsCompletedAt() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        pushOne(user, createTask(id, "title", "T"));

        assertThat(pushOne(user, mutation("task", id, "UPDATE", 1, payload("status", "DONE", "priority", "HIGH"))))
                .containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
        Map<String, Object> done = row("tasks", id);
        assertThat(done).containsEntry("status", "DONE").containsEntry("priority", "HIGH");
        assertThat(done.get("completed_at")).isNotNull();

        assertThat(pushOne(user, mutation("task", id, "UPDATE", 2, payload("status", "OPEN"))))
                .containsEntry("entityVersion", 3);
        assertThat(row("tasks", id).get("completed_at")).isNull();
    }

    @Test
    void Sync_04_staleBaseVersionIsConflictWithFullTaskSnapshot() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        pushOne(user, createTask(id, "title", "Gốc", "dueDate", "2026-11-06", "dueTime", "09:30"));
        pushOne(user, mutation("task", id, "UPDATE", 1, payload("title", "V2")));

        Map<String, Object> r = pushOne(user, mutation("task", id, "UPDATE", 1, payload("title", "Cũ")));

        assertThat(r).containsEntry("status", "CONFLICT");
        assertThat(data(r)).containsOnlyKeys("title", "description", "status", "priority", "dueDate", "dueTime",
                "projectId", "categoryId", "goalId", "milestoneId", "completedAt", "createdAt", "updatedAt")
                .containsEntry("title", "V2").containsEntry("dueDate", "2026-11-06").containsEntry("dueTime", "09:30");
        assertThat(row("tasks", id)).containsEntry("title", "V2").containsEntry("version", 2L);
    }

    @Test
    void Sync_28_futureBaseVersionIsConflict() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        pushOne(user, createTask(id, "title", "T"));
        assertThat(pushOne(user, mutation("task", id, "UPDATE", 2, payload("title", "X"))))
                .containsEntry("status", "CONFLICT");
    }

    @Test
    void Sync_24_and_25_tombstoneUpdateIsConflictAndTombstoneDeleteIsAppliedWithoutChange() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        pushOne(user, createTask(id, "title", "T"));
        assertThat(pushOne(user, mutation("task", id, "DELETE", 1, null))).containsEntry("entityVersion", 2);
        int logs = changeLogsOfUser(user.id());

        Map<String, Object> update = pushOne(user, mutation("task", id, "UPDATE", 2, payload("title", "X")));
        Map<String, Object> delete = pushOne(user, mutation("task", id, "DELETE", 1, null));

        assertThat(update).containsEntry("status", "CONFLICT");
        assertThat(update.get("serverSnapshot")).isEqualTo(Map.of("entityType", "task", "entityId", id.toString(),
                "version", 2, "deleted", true));
        assertThat(delete).containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
        assertThat(changeLogsOfUser(user.id())).isEqualTo(logs);
    }

    @Test
    void Sync_26_updateWithSameValuesKeepsVersionAndWritesNoChangeLog() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        pushOne(user, createTask(id, "title", "T", "status", "OPEN", "priority", "LOW"));

        assertThat(pushOne(user, mutation("task", id, "UPDATE", 1, payload("title", "T", "status", "OPEN",
                "priority", "LOW", "description", null, "dueDate", null, "dueTime", null, "projectId", null,
                "categoryId", null)))).containsEntry("status", "APPLIED").containsEntry("entityVersion", 1);
        assertThat(changeLogRows(user.id(), id)).hasSize(1);
    }

    @Test
    void Sync_27_absentKeepsExplicitNullClearsAndNullTitleIsRejected() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        UUID category = createCategory(user);
        pushOne(user, createTask(id, "title", "T", "description", "mô tả", "dueDate", "2026-12-01",
                "categoryId", category.toString()));

        assertThat(pushOne(user, mutation("task", id, "UPDATE", 1, payload("title", "T2"))))
                .containsEntry("entityVersion", 2);
        assertThat(row("tasks", id)).containsEntry("description", "mô tả").containsEntry("category_id", category);

        assertThat(pushOne(user, mutation("task", id, "UPDATE", 2, payload("description", null, "categoryId", null,
                "dueDate", null)))).containsEntry("entityVersion", 3);
        Map<String, Object> cleared = row("tasks", id);
        assertThat(cleared.get("description")).isNull();
        assertThat(cleared.get("category_id")).isNull();
        assertThat(cleared.get("due_date")).isNull();

        assertThat(pushOne(user, mutation("task", id, "UPDATE", 3, payload("title", null))))
                .containsEntry("status", "REJECTED").containsEntry("errorCode", "VALIDATION_FAILED")
                .containsEntry("fieldErrors", List.of(Map.of("field", "title", "code", "REQUIRED")));
    }

    @Test
    void Sync_18_serverManagedKeysAreIgnored() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        pushOne(user, createTask(id, "title", "T", "version", 42, "completedAt", "2020-01-01T00:00:00Z",
                "updatedAt", "2020-01-01T00:00:00Z", "userId", UUID.randomUUID().toString()));
        Map<String, Object> task = row("tasks", id);
        assertThat(task).containsEntry("version", 1L).containsEntry("user_id", user.id());
        assertThat(task.get("completed_at")).isNull();
        assertThat(task.get("updated_at").toString()).doesNotStartWith("2020");
    }

    @Test
    void Sync_15_referenceToDeletedMissingOrForeignProjectIsRejectedIdentically() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID deleted = createProject(user);
        pushOne(user, mutation("project", deleted, "DELETE", 1, null));
        UUID foreign = createProject(other);

        List<Map<String, Object>> results = push(user,
                createTask(UUID.randomUUID(), "title", "T", "projectId", deleted.toString()),
                createTask(UUID.randomUUID(), "title", "T", "projectId", UUID.randomUUID().toString()),
                createTask(UUID.randomUUID(), "title", "T", "projectId", foreign.toString()),
                createTask(UUID.randomUUID(), "title", "T", "categoryId", UUID.randomUUID().toString()));

        assertThat(results.subList(0, 3)).allSatisfy(r -> assertThat(r)
                .containsOnlyKeys("mutationId", "status", "errorCode", "message")
                .containsEntry("status", "REJECTED").containsEntry("errorCode", "PROJECT_NOT_FOUND"));
        assertThat(results.get(3)).containsEntry("errorCode", "CATEGORY_NOT_FOUND");
    }

    @Test
    void referenceNotFoundOnUpdateIsNotMistakenForTheTargetBeingMissing() throws Exception {
        TestUser user = newUser();
        UUID task = UUID.randomUUID();
        pushOne(user, createTask(task, "title", "T"));
        UUID project = createProject(user);
        pushOne(user, mutation("project", project, "DELETE", 1, null));

        Map<String, Object> r = pushOne(user, mutation("task", task, "UPDATE", 1,
                payload("projectId", project.toString())));

        assertThat(r).containsEntry("status", "REJECTED").containsEntry("errorCode", "PROJECT_NOT_FOUND")
                .doesNotContainKey("serverSnapshot");
        assertThat(row("tasks", task)).containsEntry("version", 1L);
    }

    @Test
    void dependentMutationsAreProcessedInOrderAndAChildOfARejectedParentIsRejected() throws Exception {
        TestUser user = newUser();
        UUID project = UUID.randomUUID();
        UUID badProject = UUID.randomUUID();
        List<Map<String, Object>> results = push(user,
                mutation("project", project, "CREATE", 0, payload("name", "P")),
                createTask(UUID.randomUUID(), "title", "Con", "projectId", project.toString()),
                mutation("project", badProject, "CREATE", 0, payload("name", "")),
                createTask(UUID.randomUUID(), "title", "Con mồ côi", "projectId", badProject.toString()));

        assertThat(results).extracting(r -> r.get("status") + ":" + r.getOrDefault("errorCode", ""))
                .containsExactly("APPLIED:", "APPLIED:", "REJECTED:VALIDATION_FAILED", "REJECTED:PROJECT_NOT_FOUND");
    }

    @Test
    void wrongJsonTypesInTaskPayloadAreRejectedPerField() throws Exception {
        TestUser user = newUser();
        List<Map<String, Object>> results = push(user,
                createTask(UUID.randomUUID(), "title", "T", "projectId", "khong-phai-uuid"),
                createTask(UUID.randomUUID(), "title", List.of("a")),
                createTask(UUID.randomUUID(), "title", "T", "dueDate", 20261106));

        assertThat(results).extracting(r -> r.get("fieldErrors")).containsExactly(
                List.of(Map.of("field", "projectId", "code", "INVALID_VALUE")),
                List.of(Map.of("field", "title", "code", "INVALID_VALUE")),
                List.of(Map.of("field", "dueDate", "code", "INVALID_VALUE")));
    }

    /** SYN-34: cùng đầu vào sai qua REST và sync → cùng errorCode và fieldErrors (field, code). */
    @Test
    void Sync_34_validationIsIdenticalToRest() throws Exception {
        TestUser user = newUser();
        List<Object[]> invalid = List.of(
                new Object[] {"title", ""},
                new Object[] {"title", "x".repeat(201)},
                new Object[] {"title", "T", "description", "d".repeat(10_001)},
                new Object[] {"title", "T", "status", "DOING"},
                new Object[] {"title", "T", "priority", "URGENT"},
                new Object[] {"title", "T", "dueDate", "2026-13-01"},
                new Object[] {"title", "T", "dueDate", "2026-11-06", "dueTime", "9:00"},
                new Object[] {"title", "T", "dueTime", "09:00"},
                new Object[] {"title", "T", "projectId", UUID.randomUUID().toString()},
                new Object[] {"title", "T", "categoryId", UUID.randomUUID().toString()});

        List<String> mismatches = new ArrayList<>();
        for (Object[] fields : invalid) {
            UUID id = UUID.randomUUID();
            Object[] restBody = new Object[fields.length + 2];
            restBody[0] = "id";
            restBody[1] = id.toString();
            System.arraycopy(fields, 0, restBody, 2, fields.length);
            MvcResult rest = post(user, TASKS, body(restBody));
            Map<String, Object> problem = readJson(rest);
            Map<String, Object> sync = pushOne(user, createTask(UUID.randomUUID(), fields));

            Object restFieldErrors = problem.get("fieldErrors") == null ? null
                    : ((List<?>) problem.get("fieldErrors")).stream()
                    .map(e -> Map.of("field", ((Map<?, ?>) e).get("field"), "code", ((Map<?, ?>) e).get("code")))
                    .toList();
            if (!problem.get("errorCode").equals(sync.get("errorCode"))
                    || !java.util.Objects.equals(restFieldErrors, sync.get("fieldErrors"))) {
                mismatches.add(fields[fields.length - 2] + ": REST " + problem.get("errorCode") + restFieldErrors
                        + " vs sync " + sync.get("errorCode") + sync.get("fieldErrors"));
            }
        }
        assertThat(mismatches).isEmpty();
    }

    private long lastSeq(UUID userId) {
        return jdbc.sql("SELECT coalesce(max(last_seq), 0) FROM user_sync_state WHERE user_id = :u")
                .param("u", userId).query(Long.class).single();
    }
}
