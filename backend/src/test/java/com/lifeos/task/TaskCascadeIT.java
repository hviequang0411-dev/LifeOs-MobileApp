package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * SYN-13 / SYN-14 phần REST (SYSTEM_ARCHITECTURE §3.8, D20, D80): xóa Project/Category thì server gỡ tham chiếu khỏi
 * mọi Task còn sống (kể cả DONE) trong cùng transaction; mỗi Task tăng version và có đúng một change_logs UPDATE; seq
 * của các Task đứng TRƯỚC seq xóa Project/Category; Task đã xóa và Task của Project/Category khác không bị đụng tới.
 * Bản đồng thời (CascadeConcurrencyIT) ở bước 2e.
 */
class TaskCascadeIT extends TaskApiTestBase {

    private List<Map<String, Object>> changeLogsOf(UUID userId) {
        return jdbc.sql("SELECT seq, entity_type, entity_id, operation, entity_version FROM change_logs"
                + " WHERE user_id = :u ORDER BY seq").param("u", userId).query().listOfRows();
    }

    private long seqOf(List<Map<String, Object>> logs, UUID entityId, String operation) {
        return logs.stream()
                .filter(l -> l.get("entity_id").equals(entityId) && l.get("operation").equals(operation))
                .mapToLong(l -> (Long) l.get("seq"))
                .reduce((a, b) -> b).orElseThrow();
    }

    @Test
    void deletingAProjectMovesItsLiveTasksToInboxWithOneChangeEach() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        UUID otherProject = project(user);
        UUID open = task(user, "projectId", project.toString());
        UUID done = task(user, "projectId", project.toString(), "status", "DONE");
        UUID deletedTask = task(user, "projectId", project.toString());
        delete(user, TASKS + "/" + deletedTask + "?version=1");
        UUID elsewhere = task(user, "projectId", otherProject.toString());

        assertThat(delete(user, PROJECTS + "/" + project + "?version=1").getResponse().getStatus()).isEqualTo(204);

        for (UUID moved : List.of(open, done)) {
            Map<String, Object> row = taskRow(moved);
            assertThat(row.get("project_id")).as("về Inbox").isNull();
            assertThat(row.get("version")).isEqualTo(2L);
            assertThat(changeLogs(user.id(), "task", moved, "UPDATE")).isEqualTo(1);
        }
        assertThat(taskRow(done).get("status")).isEqualTo("DONE");
        // Task đã xóa và Task của Project khác: không đổi.
        assertThat(taskRow(deletedTask).get("project_id")).isEqualTo(project);
        assertThat(taskRow(deletedTask).get("version")).isEqualTo(2L);
        assertThat(changeLogs(user.id(), "task", deletedTask, "UPDATE")).isZero();
        assertThat(taskRow(elsewhere).get("project_id")).isEqualTo(otherProject);
        assertThat(changeLogs(user.id(), "task", elsewhere, "UPDATE")).isZero();

        List<Map<String, Object>> logs = changeLogsOf(user.id());
        long projectDeleteSeq = seqOf(logs, project, "DELETE");
        assertThat(seqOf(logs, open, "UPDATE")).isLessThan(projectDeleteSeq);
        assertThat(seqOf(logs, done, "UPDATE")).isLessThan(projectDeleteSeq);
        // seq liên tục (I2): không thủng.
        for (int i = 0; i < logs.size(); i++) {
            assertThat((Long) logs.get(i).get("seq")).isEqualTo(i + 1L);
        }
    }

    @Test
    void deletingACategoryLeavesItsLiveTasksUncategorizedWithOneChangeEach() throws Exception {
        TestUser user = newUser();
        UUID category = category(user);
        UUID project = project(user);
        UUID a = task(user, "categoryId", category.toString(), "projectId", project.toString());
        UUID b = task(user, "categoryId", category.toString(), "status", "DONE");
        UUID untouched = task(user);

        assertThat(delete(user, CATEGORIES + "/" + category + "?version=1").getResponse().getStatus())
                .isEqualTo(204);

        for (UUID moved : List.of(a, b)) {
            Map<String, Object> row = taskRow(moved);
            assertThat(row.get("category_id")).isNull();
            assertThat(row.get("version")).isEqualTo(2L);
            assertThat(changeLogs(user.id(), "task", moved, "UPDATE")).isEqualTo(1);
        }
        assertThat(taskRow(a).get("project_id")).as("Project giữ nguyên").isEqualTo(project);
        assertThat(changeLogs(user.id(), "task", untouched, "UPDATE")).isZero();
        List<Map<String, Object>> logs = changeLogsOf(user.id());
        assertThat(seqOf(logs, a, "UPDATE")).isLessThan(seqOf(logs, category, "DELETE"));
        assertThat(seqOf(logs, b, "UPDATE")).isLessThan(seqOf(logs, category, "DELETE"));
    }

    @Test
    void cascadeRollsBackWithTheDeletionWhenTheVersionIsStale() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        UUID task = task(user, "projectId", project.toString());

        assertThat(delete(user, PROJECTS + "/" + project + "?version=9").getResponse().getStatus()).isEqualTo(409);

        assertThat(taskRow(task).get("project_id")).isEqualTo(project);
        assertThat(taskRow(task).get("version")).isEqualTo(1L);
        assertThat(changeLogs(user.id(), "task", task, "UPDATE")).isZero();
    }
}
