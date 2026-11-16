package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * IDOR-04 (TESTING_RULES §7, SECURITY_RULES §7.4, D61): Task trỏ projectId/categoryId không tồn tại, đã xóa hoặc của
 * user khác → 422 PROJECT_NOT_FOUND / CATEGORY_NOT_FOUND với thân lỗi GIỐNG HỆT nhau (không lộ sự tồn tại), khi tạo
 * lẫn khi sửa; không có gì được ghi.
 */
class TaskReferenceIT extends TaskApiTestBase {

    private List<UUID> unusableProjects(TestUser user, TestUser other) throws Exception {
        UUID deleted = project(user);
        delete(user, PROJECTS + "/" + deleted + "?version=1");
        return List.of(UUID.randomUUID(), deleted, project(other));
    }

    private List<UUID> unusableCategories(TestUser user, TestUser other) throws Exception {
        UUID deleted = category(user);
        delete(user, CATEGORIES + "/" + deleted + "?version=1");
        return List.of(UUID.randomUUID(), deleted, category(other));
    }

    private void assertAllIdentical422(List<MvcResult> results, String errorCode) throws Exception {
        Map<String, Object> first = problem(results.get(0));
        for (MvcResult r : results) {
            assertThat(r.getResponse().getStatus()).isEqualTo(422);
            assertThat(problem(r)).isEqualTo(first).containsEntry("errorCode", errorCode);
        }
    }

    @Test
    void createWithUnusableProjectIsIndistinguishable422() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        List<MvcResult> results = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        for (UUID project : unusableProjects(user, other)) {
            UUID id = UUID.randomUUID();
            ids.add(id);
            results.add(createTask(user, id, "projectId", project.toString()));
        }
        assertAllIdentical422(results, "PROJECT_NOT_FOUND");
        for (UUID id : ids) {
            assertThat(jdbc.sql("SELECT count(*) FROM tasks WHERE id = :id").param("id", id)
                    .query(Integer.class).single()).isZero();
        }
    }

    @Test
    void createWithUnusableCategoryIsIndistinguishable422() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        List<MvcResult> results = new ArrayList<>();
        for (UUID category : unusableCategories(user, other)) {
            results.add(createTask(user, UUID.randomUUID(), "categoryId", category.toString()));
        }
        assertAllIdentical422(results, "CATEGORY_NOT_FOUND");
    }

    @Test
    void patchToUnusableReferencesIs422AndChangesNothing() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID ownProject = project(user);
        UUID task = task(user, "projectId", ownProject.toString());

        List<MvcResult> projectResults = new ArrayList<>();
        for (UUID project : unusableProjects(user, other)) {
            projectResults.add(patchTask(user, task, "version", 1, "projectId", project.toString()));
        }
        assertAllIdentical422(projectResults, "PROJECT_NOT_FOUND");

        List<MvcResult> categoryResults = new ArrayList<>();
        for (UUID category : unusableCategories(user, other)) {
            categoryResults.add(patchTask(user, task, "version", 1, "categoryId", category.toString()));
        }
        assertAllIdentical422(categoryResults, "CATEGORY_NOT_FOUND");

        Map<String, Object> row = taskRow(task);
        assertThat(row.get("project_id")).isEqualTo(ownProject);
        assertThat(row.get("category_id")).isNull();
        assertThat(row.get("version")).isEqualTo(1L);
        assertThat(changeLogs(user.id(), "task", task, "UPDATE")).isZero();
    }

    @Test
    void ownLiveReferencesAreAccepted() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        UUID category = category(user);
        UUID task = task(user);

        MvcResult result = patchTask(user, task, "version", 1, "projectId", project.toString(),
                "categoryId", category.toString());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(taskRow(task).get("project_id")).isEqualTo(project);
        assertThat(taskRow(task).get("category_id")).isEqualTo(category);
    }
}
