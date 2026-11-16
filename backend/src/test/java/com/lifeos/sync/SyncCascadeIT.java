package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Cascade xóa Project/Category đi qua sync (SYSTEM_ARCHITECTURE §3.8, SYNC_PROTOCOL §3.2, D20, D80, D86):
 * SYN-13, SYN-14, SYN-21, SYN-35.
 */
class SyncCascadeIT extends SyncTestBase {

    private List<UUID> tasksIn(TestUser user, String key, UUID parent, int count) throws Exception {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID id = UUID.randomUUID();
            assertThat(pushOne(user, mutation("task", id, "CREATE", 0, payload("title", "T" + i,
                    key, parent.toString())))).containsEntry("status", "APPLIED");
            ids.add(id);
        }
        return ids;
    }

    @Test
    void Sync_13_and_35_deletingAProjectDetachesEveryLiveTaskWithItsOwnChangeLogBeforeTheProjectDelete()
            throws Exception {
        TestUser user = newUser();
        UUID project = createProject(user);
        List<UUID> tasks = tasksIn(user, "projectId", project, 3);
        UUID done = tasks.get(2);
        pushOne(user, mutation("task", done, "UPDATE", 1, payload("status", "DONE")));
        UUID deletedTask = UUID.randomUUID();
        pushOne(user, mutation("task", deletedTask, "CREATE", 0, payload("title", "Xóa",
                "projectId", project.toString())));
        pushOne(user, mutation("task", deletedTask, "DELETE", 1, null));
        Map<String, Object> delete = mutation("project", project, "DELETE", 1, null);

        assertThat(pushOne(user, delete)).containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);

        for (UUID task : tasks) {
            Map<String, Object> row = row("tasks", task);
            assertThat(row.get("project_id")).as("về Inbox, kể cả Task DONE").isNull();
            assertThat(row.get("version")).isEqualTo(task.equals(done) ? 3L : 2L);
        }
        assertThat(row("tasks", deletedTask)).as("tombstone không bị cascade").containsEntry("version", 2L)
                .containsEntry("project_id", project);

        List<Map<String, Object>> taskLogs = jdbc.sql("""
                SELECT seq, mutation_id FROM change_logs
                WHERE user_id = :u AND entity_type = 'task' AND operation = 'UPDATE'
                  AND entity_id IN (:ids) AND seq > (SELECT max(seq) FROM change_logs WHERE user_id = :u
                                                     AND entity_type = 'task' AND entity_id = :lastBefore)
                ORDER BY seq""")
                .param("u", user.id()).param("ids", tasks).param("lastBefore", deletedTask)
                .query().listOfRows();
        assertThat(taskLogs).hasSize(3).allSatisfy(log -> assertThat(log.get("mutation_id")).isNull());
        Map<String, Object> projectLog = changeLogRows(user.id(), project).get(1);
        assertThat(projectLog).containsEntry("operation", "DELETE").containsEntry("mutation_id", mutationIdOf(delete));
        assertThat((Long) projectLog.get("seq")).as("seq Task UPDATE < Project DELETE")
                .isGreaterThan((Long) taskLogs.get(2).get("seq"));
    }

    @Test
    void Sync_14_deletingACategoryClearsTheCategoryOfLiveTasks() throws Exception {
        TestUser user = newUser();
        UUID category = createCategory(user);
        List<UUID> tasks = tasksIn(user, "categoryId", category, 2);
        Map<String, Object> delete = mutation("category", category, "DELETE", 1, null);

        assertThat(pushOne(user, delete)).containsEntry("status", "APPLIED");

        for (UUID task : tasks) {
            assertThat(row("tasks", task)).containsEntry("category_id", null).containsEntry("version", 2L);
            assertThat(changeLogRows(user.id(), task)).last().satisfies(log -> assertThat(log)
                    .containsEntry("operation", "UPDATE").containsEntry("mutation_id", null));
        }
        assertThat(changeLogRows(user.id(), category)).last().satisfies(log -> assertThat(log)
                .containsEntry("operation", "DELETE").containsEntry("mutation_id", mutationIdOf(delete)));
    }

    @Test
    void Sync_21_pendingTaskUpdateWithOldBaseVersionAfterAnotherDevicesCascadeIsConflictAndNotApplied()
            throws Exception {
        TestUser phone = newUser();
        TestUser tablet = new TestUser(phone.id(), jwtService.issue(phone.id(), UUID.randomUUID()).token());
        UUID project = createProject(phone);
        UUID task = tasksIn(phone, "projectId", project, 1).get(0);

        assertThat(pushOne(phone, mutation("project", project, "DELETE", 1, null))).containsEntry("status", "APPLIED");
        Map<String, Object> pending = pushOne(tablet, mutation("task", task, "UPDATE", 1,
                payload("title", "Sửa offline", "projectId", project.toString())));

        assertThat(pending).containsEntry("status", "CONFLICT");
        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot = (Map<String, Object>) pending.get("serverSnapshot");
        assertThat(snapshot).containsEntry("version", 2);
        assertThat(snapshot.get("data")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("projectId", null).containsEntry("title", "T0");
        assertThat(row("tasks", task)).containsEntry("title", "T0").containsEntry("project_id", null)
                .containsEntry("version", 2L);
    }
}
