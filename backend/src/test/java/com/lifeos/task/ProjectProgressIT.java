package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * GET /projects/{id}/progress (FR-PJ-05, UC-PJ-05, API_CONVENTIONS §8.5, D79): Task đã xóa không tính; không có Task
 * → 0/0, completionRatio 0; tỉ lệ 4 chữ số HALF_UP; Project không tồn tại/đã xóa/của user khác → 404 giống hệt nhau.
 */
class ProjectProgressIT extends TaskApiTestBase {

    private Map<String, Object> progress(TestUser user, UUID project) throws Exception {
        MvcResult result = get(user, PROJECTS + "/" + project + "/progress");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return readJson(result);
    }

    private static BigDecimal ratio(Map<String, Object> body) {
        return new BigDecimal(body.get("completionRatio").toString());
    }

    @Test
    void emptyProjectHasZeroProgress() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        Map<String, Object> body = progress(user, project);
        assertThat(body).containsOnlyKeys("projectId", "totalTasks", "completedTasks", "openTasks",
                "completionRatio");
        assertThat(body).containsEntry("projectId", project.toString()).containsEntry("totalTasks", 0)
                .containsEntry("completedTasks", 0).containsEntry("openTasks", 0);
        assertThat(ratio(body)).isEqualByComparingTo("0");
    }

    @Test
    void countsLiveTasksAndRoundsTheRatioHalfUp() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        task(user, "projectId", project.toString(), "status", "DONE");
        task(user, "projectId", project.toString(), "status", "DONE");
        task(user, "projectId", project.toString());
        UUID deletedDone = task(user, "projectId", project.toString(), "status", "DONE");
        delete(user, TASKS + "/" + deletedDone + "?version=1");
        task(user); // Inbox, không thuộc Project
        task(user, "projectId", project(user).toString(), "status", "DONE"); // Project khác

        Map<String, Object> body = progress(user, project);
        assertThat(body).containsEntry("totalTasks", 3).containsEntry("completedTasks", 2)
                .containsEntry("openTasks", 1);
        assertThat(ratio(body)).isEqualByComparingTo("0.6667"); // 2/3 làm tròn HALF_UP 4 chữ số
    }

    @Test
    void archivedProjectStillHasProgress() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        task(user, "projectId", project.toString(), "status", "DONE");
        post(user, PROJECTS + "/" + project + "/archive", body("version", 1));

        assertThat(ratio(progress(user, project))).isEqualByComparingTo("1");
    }

    @Test
    void foreignDeletedOrMissingProjectIsIndistinguishable404() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        UUID aliceProject = project(alice);
        UUID bobDeleted = project(bob);
        delete(bob, PROJECTS + "/" + bobDeleted + "?version=1");

        MvcResult foreign = get(bob, PROJECTS + "/" + aliceProject + "/progress");
        MvcResult deleted = get(bob, PROJECTS + "/" + bobDeleted + "/progress");
        MvcResult missing = get(bob, PROJECTS + "/" + UUID.randomUUID() + "/progress");
        for (MvcResult r : new MvcResult[] {foreign, deleted, missing}) {
            assertThat(r.getResponse().getStatus()).isEqualTo(404);
            assertThat(problem(r)).isEqualTo(problem(missing)).containsEntry("errorCode", "PROJECT_NOT_FOUND");
        }
    }
}
