package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-TK-01, 03, 04, 05, 06 qua REST (API_CONVENTIONS §8.4; D58, D62, D78): mặc định, kiểm giá trị (TESTING_RULES
 * §5.1), dueTime cần dueDate, OPEN↔DONE đặt/xóa completedAt, version, xóa mềm. IDOR-01/02/03 và SEC-08 phần Task.
 */
class TaskIT extends TaskApiTestBase {

    @Test
    void quickCaptureNeedsOnlyATitleAndDefaultsToOpenNoneInbox() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();

        MvcResult result = createTask(user, id, "title", "  Mua sữa  ");

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(TASKS + "/" + id);
        Map<String, Object> body = readJson(result);
        assertThat(body).containsOnlyKeys("id", "title", "description", "status", "priority", "dueDate", "dueTime",
                "completedAt", "projectId", "categoryId", "goalId", "milestoneId", "version", "createdAt",
                "updatedAt"); // goalId, milestoneId: WP6, BACKEND_MASTER_PLAN §5 #2
        assertThat(body).containsEntry("title", "Mua sữa").containsEntry("status", "OPEN")
                .containsEntry("priority", "NONE").containsEntry("projectId", null).containsEntry("categoryId", null)
                .containsEntry("dueDate", null).containsEntry("dueTime", null).containsEntry("completedAt", null);
        assertThat(version(body)).isEqualTo(1);
        assertThat(changeLogs(user.id(), "task", id, "CREATE")).isEqualTo(1);
    }

    @Test
    void fullCreateWithProjectCategoryDueDateAndTime() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        UUID category = category(user);

        Map<String, Object> body = readJson(createTask(user, UUID.randomUUID(), "description", "  giữ nguyên  ",
                "priority", "HIGH", "dueDate", "2026-11-07", "dueTime", "08:30", "projectId", project.toString(),
                "categoryId", category.toString()));

        assertThat(body).containsEntry("description", "  giữ nguyên  ").containsEntry("priority", "HIGH")
                .containsEntry("dueDate", "2026-11-07").containsEntry("dueTime", "08:30")
                .containsEntry("projectId", project.toString()).containsEntry("categoryId", category.toString());
    }

    @Test
    void creatingAsDoneSetsCompletedAtOnTheServer() throws Exception {
        TestUser user = newUser();
        clock.advance(Duration.ofSeconds(1));
        Instant now = clock.instant();
        Map<String, Object> body = readJson(createTask(user, UUID.randomUUID(), "status", "DONE",
                "completedAt", "2000-01-01T00:00:00Z"));
        assertThat(body).containsEntry("status", "DONE");
        assertThat(Instant.parse((String) body.get("completedAt"))).isEqualTo(now);
    }

    @Test
    void invalidValuesAreRejectedWithFieldErrors() throws Exception {
        TestUser user = newUser();
        Object[][] cases = {
            {"title", "   ", "title", "REQUIRED"},
            {"title", "t".repeat(201), "title", "SIZE"},
            {"description", "d".repeat(10_001), "description", "SIZE"},
            {"status", "DOING", "status", "INVALID_VALUE"},
            {"priority", "URGENT", "priority", "INVALID_VALUE"},
            {"dueDate", "07/11/2026", "dueDate", "PATTERN"},
            {"dueDate", "2026-02-30", "dueDate", "PATTERN"},
            {"dueTime", "8h30", "dueTime", "PATTERN"},
        };
        for (Object[] c : cases) {
            UUID id = UUID.randomUUID();
            Object[] extra = "dueTime".equals(c[0]) ? new Object[] {c[0], c[1], "dueDate", "2026-11-07"}
                    : new Object[] {c[0], c[1]};
            MvcResult result = createTask(user, id, extra);
            assertThat(result.getResponse().getStatus()).as(c[0] + "=" + c[1]).isEqualTo(400);
            assertThat(result.getResponse().getContentAsString()).as(c[0] + "=" + c[1])
                    .contains("\"errorCode\":\"VALIDATION_FAILED\"").contains("\"field\":\"" + c[2] + "\"")
                    .contains("\"code\":\"" + c[3] + "\"");
            assertThat(jdbc.sql("SELECT count(*) FROM tasks WHERE id = :id").param("id", id)
                    .query(Integer.class).single()).isZero();
        }
        assertThat(createTask(user, UUID.randomUUID(), "title", " " + "t".repeat(200) + " ",
                "description", "d".repeat(10_000)).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void dueTimeWithoutDueDateIsRejectedOnCreateAndPatch() throws Exception {
        TestUser user = newUser();
        MvcResult onCreate = createTask(user, UUID.randomUUID(), "dueTime", "08:00");
        assertThat(onCreate.getResponse().getStatus()).isEqualTo(400);
        assertThat(onCreate.getResponse().getContentAsString()).contains("\"field\":\"dueTime\"")
                .contains("\"code\":\"INVALID_VALUE\"");

        UUID id = task(user, "dueDate", "2026-11-07", "dueTime", "08:00");
        // D78: bỏ dueDate mà vẫn giữ dueTime → 400, không tự xóa dueTime.
        MvcResult onPatch = patchTask(user, id, "version", 1, "dueDate", null);
        assertThat(onPatch.getResponse().getStatus()).isEqualTo(400);
        assertThat(onPatch.getResponse().getContentAsString()).contains("\"field\":\"dueTime\"");
        assertThat(taskRow(id).get("due_date")).isNotNull();
        // Bỏ cả hai cùng lúc thì được.
        Map<String, Object> cleared = readJson(patchTask(user, id, "version", 1, "dueDate", null, "dueTime", null));
        assertThat(cleared).containsEntry("dueDate", null).containsEntry("dueTime", null);
        assertThat(version(cleared)).isEqualTo(2);
    }

    @Test
    void existingIdIsAConflictIndistinguishableAcrossOwnersAndTombstones() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        UUID aliceTask = task(alice);
        UUID tomb = task(bob);
        delete(bob, TASKS + "/" + tomb + "?version=1");

        MvcResult own = createTask(alice, aliceTask);
        MvcResult foreign = createTask(bob, aliceTask);     // IDOR-03
        MvcResult tombstone = createTask(bob, tomb);        // D78
        for (MvcResult r : new MvcResult[] {own, foreign, tombstone}) {
            assertThat(r.getResponse().getStatus()).isEqualTo(409);
            assertThat(problem(r)).isEqualTo(problem(own)).containsEntry("errorCode", "ENTITY_ALREADY_EXISTS");
        }
        assertThat(taskRow(aliceTask).get("user_id")).isEqualTo(alice.id());
    }

    @Test
    void serverControlledFieldsCannotBeMassAssigned() throws Exception {
        TestUser user = newUser();
        UUID id = task(user, "userId", newUser().id().toString(), "version", 9, "completedAt",
                "2020-01-01T00:00:00Z", "createdAt", "2000-01-01T00:00:00Z", "updatedAt", "2000-01-01T00:00:00Z",
                "deletedAt", "2020-01-01T00:00:00Z");
        Map<String, Object> row = taskRow(id);
        assertThat(row.get("user_id")).isEqualTo(user.id());
        assertThat(row.get("version")).isEqualTo(1L);
        assertThat(row.get("completed_at")).isNull();
        assertThat(row.get("deleted_at")).isNull();

        // PATCH cũng bỏ qua completedAt/userId; completedAt chỉ đổi theo status.
        Map<String, Object> patched = readJson(patchTask(user, id, "version", 1, "title", "Mới",
                "completedAt", "2020-01-01T00:00:00Z", "userId", newUser().id().toString()));
        assertThat(patched.get("completedAt")).isNull();
        assertThat(taskRow(id).get("user_id")).isEqualTo(user.id());
    }

    @Test
    void anotherUsersTaskIsIndistinguishableFromAMissingOne() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        UUID id = task(alice, "title", "Việc riêng");
        String path = TASKS + "/" + id;

        MvcResult foreign = get(bob, path);                                  // IDOR-01
        MvcResult missing = get(bob, TASKS + "/" + UUID.randomUUID());
        assertThat(foreign.getResponse().getStatus()).isEqualTo(404);
        assertThat(problem(foreign)).isEqualTo(problem(missing)).containsEntry("errorCode", "TASK_NOT_FOUND");
        assertThat(foreign.getResponse().getContentAsString()).doesNotContain("Việc riêng");

        // IDOR-02: mọi thao tác ghi của Bob → 404, Task của Alice không đổi.
        assertThat(patchTask(bob, id, "version", 1, "title", "Bị sửa").getResponse().getStatus()).isEqualTo(404);
        assertThat(post(bob, path + "/complete", body("version", 1)).getResponse().getStatus()).isEqualTo(404);
        assertThat(post(bob, path + "/reopen", body("version", 1)).getResponse().getStatus()).isEqualTo(404);
        assertThat(delete(bob, path + "?version=1").getResponse().getStatus()).isEqualTo(404);
        Map<String, Object> row = taskRow(id);
        assertThat(row.get("title")).isEqualTo("Việc riêng");
        assertThat(row.get("version")).isEqualTo(1L);
        assertThat(row.get("status")).isEqualTo("OPEN");
        assertThat(row.get("deleted_at")).isNull();
    }

    @Test
    void patchCanChangeEveryEditableFieldIncludingStatus() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        UUID category = category(user);
        UUID id = task(user);
        clock.advance(Duration.ofSeconds(1));
        Instant now = clock.instant();

        Map<String, Object> body = readJson(patchTask(user, id, "version", 1, "title", " Mua bánh ",
                "description", "mô tả", "status", "DONE", "priority", "LOW", "dueDate", "2026-12-01",
                "dueTime", "21:15", "projectId", project.toString(), "categoryId", category.toString()));

        assertThat(body).containsEntry("title", "Mua bánh").containsEntry("description", "mô tả")
                .containsEntry("status", "DONE").containsEntry("priority", "LOW")
                .containsEntry("dueDate", "2026-12-01").containsEntry("dueTime", "21:15")
                .containsEntry("projectId", project.toString()).containsEntry("categoryId", category.toString());
        assertThat(Instant.parse((String) body.get("completedAt"))).isEqualTo(now);
        assertThat(version(body)).isEqualTo(2);
        assertThat(changeLogs(user.id(), "task", id, "UPDATE")).isEqualTo(1);

        // null tường minh gỡ Project/Category (về Inbox / không danh mục); status OPEN xóa completedAt.
        Map<String, Object> cleared = readJson(patchTask(user, id, "version", 2, "projectId", null,
                "categoryId", null, "status", "OPEN", "description", null));
        assertThat(cleared).containsEntry("projectId", null).containsEntry("categoryId", null)
                .containsEntry("status", "OPEN").containsEntry("completedAt", null).containsEntry("description", null);
        assertThat(version(cleared)).isEqualTo(3);
    }

    @Test
    void patchRequiresVersionRejectsStaleAndNoOpKeepsVersion() throws Exception {
        TestUser user = newUser();
        UUID id = task(user);

        assertThat(patchTask(user, id, "title", "X").getResponse().getContentAsString())
                .contains("\"field\":\"version\"").contains("\"code\":\"REQUIRED\"");
        assertThat(version(readJson(patchTask(user, id, "version", 1, "title", " Mua sữa ")))).isEqualTo(1);
        assertThat(changeLogs(user.id(), "task", id, "UPDATE")).isZero();

        patchTask(user, id, "version", 1, "title", "V2");
        MvcResult stale = patchTask(user, id, "version", 1, "title", "V3");
        assertThat(stale.getResponse().getStatus()).isEqualTo(409);
        assertThat(readJson(stale)).containsEntry("errorCode", "ENTITY_VERSION_CONFLICT")
                .containsEntry("currentVersion", 2);
        for (String nonNullable : new String[] {"title", "status", "priority"}) {
            MvcResult nulled = patchTask(user, id, "version", 2, nonNullable, null);
            assertThat(nulled.getResponse().getStatus()).as(nonNullable).isEqualTo(400);
            assertThat(nulled.getResponse().getContentAsString()).contains("\"field\":\"" + nonNullable + "\"")
                    .contains("\"code\":\"REQUIRED\"");
        }
    }

    @Test
    void completeAndReopenAreVersionedAndIdempotent() throws Exception {
        TestUser user = newUser();
        UUID id = task(user);
        String path = TASKS + "/" + id;
        clock.advance(Duration.ofSeconds(1));
        Instant now = clock.instant();

        Map<String, Object> done = readJson(post(user, path + "/complete", body("version", 1)));
        assertThat(done).containsEntry("status", "DONE");
        assertThat(Instant.parse((String) done.get("completedAt"))).isEqualTo(now);
        assertThat(version(done)).isEqualTo(2);

        MvcResult again = post(user, path + "/complete", body("version", 2));
        assertThat(again.getResponse().getStatus()).isEqualTo(200);
        assertThat(version(readJson(again))).isEqualTo(2);
        assertThat(post(user, path + "/complete", body("version", 1)).getResponse().getStatus()).isEqualTo(409);

        Map<String, Object> reopened = readJson(post(user, path + "/reopen", body("version", 2)));
        assertThat(reopened).containsEntry("status", "OPEN").containsEntry("completedAt", null);
        assertThat(version(reopened)).isEqualTo(3);
        assertThat(version(readJson(post(user, path + "/reopen", body("version", 3))))).isEqualTo(3);
        assertThat(changeLogs(user.id(), "task", id, "UPDATE")).isEqualTo(2);
        assertThat(post(user, path + "/reopen", "{}").getResponse().getContentAsString())
                .contains("\"field\":\"version\"");
    }

    @Test
    void deleteIsVersionedSoftAndFinal() throws Exception {
        TestUser user = newUser();
        UUID id = task(user);
        String path = TASKS + "/" + id;

        assertThat(delete(user, path).getResponse().getContentAsString()).contains("\"field\":\"version\"");
        assertThat(delete(user, path + "?version=5").getResponse().getStatus()).isEqualTo(409);
        assertThat(delete(user, path + "?version=1").getResponse().getStatus()).isEqualTo(204);

        Map<String, Object> row = taskRow(id);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(row.get("version")).isEqualTo(2L);
        assertThat(changeLogs(user.id(), "task", id, "DELETE")).isEqualTo(1);
        assertThat(get(user, path).getResponse().getStatus()).isEqualTo(404);
        assertThat(patchTask(user, id, "version", 2, "title", "X").getResponse().getStatus()).isEqualTo(404);
        assertThat(post(user, path + "/complete", body("version", 2)).getResponse().getStatus()).isEqualTo(404);
        assertThat(delete(user, path + "?version=2").getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void taskMayBelongToAnArchivedProject() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        post(user, PROJECTS + "/" + project + "/archive", body("version", 1));

        assertThat(createTask(user, UUID.randomUUID(), "projectId", project.toString()).getResponse().getStatus())
                .as("D78: gán Task vào Project đã lưu trữ được phép").isEqualTo(201);
    }
}
