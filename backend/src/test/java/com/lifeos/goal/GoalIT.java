package com.lifeos.goal;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Goal, Milestone, liên kết Task và tiến độ qua REST (PHASE_6 §6; KNW-09..12, KNW-15). Cascade xóa Goal/Milestone chỉ gỡ
 * liên kết Task (D186), change_logs với mutation_id NULL.
 */
class GoalIT extends ApiTestBase {

    private static final String GOALS = "/api/v1/goals";

    private UUID goal(TestUser user) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult r = post(user, GOALS, body("id", id.toString(), "title", "Chạy 10 km"));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    private UUID milestone(TestUser user, UUID goal, Long position) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult r = post(user, GOALS + "/" + goal + "/milestones", position == null
                ? body("id", id.toString(), "title", "Mốc")
                : body("id", id.toString(), "title", "Mốc", "position", position));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    private MvcResult createTask(TestUser user, UUID id, Object... links) throws Exception {
        Object[] kv = new Object[4 + links.length];
        kv[0] = "id";
        kv[1] = id.toString();
        kv[2] = "title";
        kv[3] = "Việc";
        System.arraycopy(links, 0, kv, 4, links.length);
        return post(user, "/api/v1/tasks", body(kv));
    }

    private UUID task(TestUser user, Object... links) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult r = createTask(user, id, links);
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> progress(TestUser user, UUID goal) throws Exception {
        MvcResult r = get(user, GOALS + "/" + goal);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        return (Map<String, Object>) readJson(r).get("progress");
    }

    private static Double value(Map<String, Object> progress) {
        Object v = progress.get("value");
        return v == null ? null : ((Number) v).doubleValue();
    }

    private Map<String, Object> taskRow(UUID id) {
        return jdbc.sql("SELECT goal_id, milestone_id, version, deleted_at FROM tasks WHERE id = :id")
                .param("id", id).query().singleRow();
    }

    private long goalVersion(TestUser user, UUID goal) throws Exception {
        return version(map(readJson(get(user, GOALS + "/" + goal)).get("goal")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    @Test
    void Knw_09_progressIsComputedFromLiveTasksAndMilestones() throws Exception {
        TestUser user = newUser();
        UUID g = goal(user);
        assertThat(progress(user, g)).containsEntry("method", "NONE").containsEntry("value", null);

        UUID direct = task(user, "goalId", g.toString());
        assertThat(value(progress(user, g))).isEqualTo(0.0);
        assertThat(post(user, "/api/v1/tasks/" + direct + "/complete", body("version", 1)).getResponse()
                .getStatus()).isEqualTo(200);
        assertThat(progress(user, g)).containsEntry("method", "TASKS");
        assertThat(value(progress(user, g))).isEqualTo(1.0);

        UUID m1 = milestone(user, g, null);
        UUID m2 = milestone(user, g, null);
        UUID t1 = task(user, "milestoneId", m1.toString());
        task(user, "goalId", g.toString(), "milestoneId", m1.toString(), "status", "DONE");
        assertThat(taskRow(t1)).containsEntry("goal_id", g).as("goal suy ra từ milestone");
        Map<String, Object> p = progress(user, g);
        assertThat(p).containsEntry("method", "MILESTONES").containsEntry("totalMilestones", 2)
                .containsEntry("completedMilestones", 0).containsEntry("allMilestonesCompleted", false);
        assertThat(value(p)).as("(1/2 + 0) / 2").isEqualTo(0.25);
        assertThat(((Number) p.get("totalTasks")).longValue()).isEqualTo(3);

        assertThat(post(user, "/api/v1/milestones/" + m2 + "/complete", body("version", 1)).getResponse()
                .getStatus()).isEqualTo(200);
        assertThat(value(progress(user, g))).isEqualTo(0.75);
        assertThat(delete(user, "/api/v1/tasks/" + t1 + "?version=1").getResponse().getStatus()).isEqualTo(204);
        assertThat(value(progress(user, g))).as("Task đã xóa không tính (UC-GOAL-03)").isEqualTo(1.0);
        assertThat(progress(user, g)).containsEntry("allMilestonesCompleted", false);
        assertThat(post(user, "/api/v1/milestones/" + m1 + "/complete", body("version", 1)).getResponse()
                .getStatus()).isEqualTo(200);
        assertThat(progress(user, g)).as("chỉ gợi ý, không tự đổi trạng thái (UC-GOAL-04)")
                .containsEntry("allMilestonesCompleted", true);
        assertThat(map(readJson(get(user, GOALS + "/" + g)).get("goal"))).containsEntry("status", "ACTIVE");

        MvcResult list = get(user, GOALS);
        assertThat(items(list)).hasSize(1);
        assertThat(map(items(list).get(0).get("progress"))).containsEntry("method", "MILESTONES");
        assertThat((List<?>) items(list).get(0).get("milestones")).hasSize(2);
    }

    @Test
    void Knw_10_linksAreCheckedAndDeletionDetachesTasksWithoutDeletingThem() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID g = goal(user);
        UUID g2 = goal(user);
        UUID m1 = milestone(user, g, null);
        UUID m2 = milestone(user, g, null);

        assertThat(readJson(createTask(user, UUID.randomUUID(), "goalId", g2.toString(), "milestoneId",
                m1.toString()))).containsEntry("errorCode", "GOAL_LINK_MISMATCH");
        assertThat(readJson(createTask(user, UUID.randomUUID(), "goalId", goal(other).toString())))
                .containsEntry("errorCode", "GOAL_NOT_FOUND");
        assertThat(readJson(createTask(user, UUID.randomUUID(), "milestoneId", UUID.randomUUID().toString())))
                .containsEntry("errorCode", "MILESTONE_NOT_FOUND");

        UUID onM1 = task(user, "milestoneId", m1.toString());
        UUID onM2 = task(user, "milestoneId", m2.toString());
        UUID onGoal = task(user, "goalId", g.toString());
        UUID moved = task(user, "milestoneId", m1.toString());

        MvcResult patched = patch(user, "/api/v1/tasks/" + moved, body("version", 1, "goalId", g2.toString()));
        assertThat(readJson(patched)).containsEntry("goalId", g2.toString()).containsEntry("milestoneId", null)
                .as("đổi goal gỡ milestone cũ");
        assertThat(readJson(patch(user, "/api/v1/tasks/" + moved, body("version", 2, "milestoneId",
                m2.toString())))).containsEntry("goalId", g.toString()).containsEntry("milestoneId", m2.toString());

        MvcResult tasks = get(user, GOALS + "/" + g + "/tasks");
        assertThat(items(tasks)).extracting(t -> t.get("id")).containsExactlyInAnyOrder(onM1.toString(),
                onM2.toString(), onGoal.toString(), moved.toString());

        assertThat(delete(user, "/api/v1/milestones/" + m1 + "?version=1").getResponse().getStatus()).isEqualTo(204);
        assertThat(taskRow(onM1)).containsEntry("goal_id", g).containsEntry("milestone_id", null)
                .containsEntry("version", 2L).containsEntry("deleted_at", null);
        assertThat(changeLogs(user.id(), "task", onM1, "UPDATE")).isEqualTo(1);

        assertThat(delete(user, GOALS + "/" + g + "?version=1").getResponse().getStatus()).isEqualTo(204);
        for (UUID t : List.of(onM1, onM2, onGoal, moved)) {
            assertThat(taskRow(t)).as("Task không bị xóa, chỉ gỡ liên kết").containsEntry("goal_id", null)
                    .containsEntry("milestone_id", null).containsEntry("deleted_at", null);
        }
        assertThat(jdbc.sql("SELECT mutation_id FROM change_logs WHERE user_id = :u AND entity_id = :e"
                + " ORDER BY seq DESC LIMIT 1").param("u", user.id()).param("e", onM2).query().singleRow())
                .containsEntry("mutation_id", null);
        assertThat(changeLogs(user.id(), "milestone", m2, "DELETE")).isEqualTo(1);
        assertThat(jdbc.sql("SELECT max(seq) FROM change_logs WHERE entity_id = :e").param("e", g)
                .query(Long.class).single()).as("change_logs của Goal ghi sau cùng (D86)").isGreaterThan(
                        jdbc.sql("SELECT max(seq) FROM change_logs WHERE entity_id IN (:ids)")
                                .param("ids", List.of(onM2, onGoal, m2)).query(Long.class).single());
        assertThat(get(user, GOALS + "/" + g).getResponse().getStatus()).isEqualTo(404);
        assertThat(get(user, GOALS + "/" + g + "/tasks").getResponse().getStatus()).isEqualTo(404);
        assertThat(readJson(patch(user, "/api/v1/tasks/" + onGoal, body("version", 2, "goalId", g.toString()))))
                .containsEntry("errorCode", "GOAL_NOT_FOUND");
    }

    @Test
    void Knw_11_statusTransitionsFollowD188AndRecordTheTime() throws Exception {
        TestUser user = newUser();
        UUID g = goal(user);
        MvcResult paused = post(user, GOALS + "/" + g + "/status", body("version", 1, "status", "PAUSED"));
        assertThat(readJson(paused)).containsEntry("status", "PAUSED");
        assertThat(readJson(paused).get("statusChangedAt")).isNotNull();
        MvcResult invalid = post(user, GOALS + "/" + g + "/status", body("version", 2, "status", "ACHIEVED"));
        assertThat(invalid.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(invalid)).containsEntry("errorCode", "GOAL_INVALID_STATUS_TRANSITION");
        assertThat(version(readJson(post(user, GOALS + "/" + g + "/status", body("version", 2, "status",
                "PAUSED"))))).as("cùng trạng thái: không đổi").isEqualTo(2);
        for (String[] step : new String[][] {{"ACTIVE", "2"}, {"ACHIEVED", "3"}, {"ACTIVE", "4"},
                {"ABANDONED", "5"}, {"ACTIVE", "6"}}) {
            MvcResult r = post(user, GOALS + "/" + g + "/status", body("version", Long.parseLong(step[1]),
                    "status", step[0]));
            assertThat(r.getResponse().getStatus()).as(step[0]).isEqualTo(200);
        }
        assertThat(readJson(post(user, GOALS + "/" + g + "/status", body("version", 7, "status", "DONE"))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(goalVersion(user, g)).isEqualTo(7);
    }

    @Test
    void Knw_12_milestoneLimitPositionsAndReordering() throws Exception {
        TestUser user = newUser();
        UUID g = goal(user);
        UUID first = milestone(user, g, null);
        UUID second = milestone(user, g, null);
        MvcResult dup = post(user, GOALS + "/" + g + "/milestones", body("id", UUID.randomUUID().toString(),
                "title", "Trùng", "position", 1000));
        assertThat(dup.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(dup)).containsEntry("errorCode", "VALIDATION_FAILED");
        UUID middle = milestone(user, g, 1500L);
        assertThat(readJson(patch(user, "/api/v1/milestones/" + second, body("version", 1, "position", 500))))
                .containsEntry("position", 500);
        assertThat(readJson(patch(user, "/api/v1/milestones/" + first, body("version", 1, "position", 500))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ordered = (List<Map<String, Object>>) readJson(get(user, GOALS + "/" + g))
                .get("milestones");
        assertThat(ordered).extracting(v -> map(v.get("milestone")).get("id")).containsExactly(second.toString(),
                first.toString(), middle.toString());

        for (int i = 3; i < 20; i++) {
            milestone(user, g, null);
        }
        MvcResult over = post(user, GOALS + "/" + g + "/milestones", body("id", UUID.randomUUID().toString(),
                "title", "Thứ 21"));
        assertThat(over.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(over)).containsEntry("errorCode", "MILESTONE_LIMIT_EXCEEDED");
        assertThat(delete(user, "/api/v1/milestones/" + first + "?version=1").getResponse().getStatus())
                .isEqualTo(204);
        milestone(user, g, null);

        MvcResult reopened = post(user, "/api/v1/milestones/" + second + "/reopen", body("version", 2));
        assertThat(version(readJson(reopened))).as("chưa hoàn thành: không đổi").isEqualTo(2);
        assertThat(readJson(post(user, "/api/v1/milestones/" + second + "/complete", body("version", 2))).get(
                "completedAt")).isNotNull();
        assertThat(readJson(post(user, "/api/v1/milestones/" + second + "/reopen", body("version", 3))))
                .containsEntry("completedAt", null);
    }

    @Test
    void Knw_15_serverFieldsCannotBeMassAssignedAndValuesAreValidated() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        MvcResult created = post(user, GOALS, body("id", id.toString(), "title", "  Mục tiêu  ", "status",
                "ACHIEVED", "statusChangedAt", "2020-01-01T00:00:00Z", "userId", UUID.randomUUID().toString()));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        assertThat(readJson(created)).containsEntry("status", "ACTIVE").containsEntry("statusChangedAt", null)
                .containsEntry("title", "Mục tiêu");
        assertThat(jdbc.sql("SELECT user_id FROM goals WHERE id = :id").param("id", id).query(UUID.class).single())
                .isEqualTo(user.id());
        UUID m = milestone(user, id, null);
        assertThat(readJson(patch(user, "/api/v1/milestones/" + m, body("version", 1, "completedAt",
                "2020-01-01T00:00:00Z", "goalId", UUID.randomUUID().toString())))).containsEntry("completedAt", null)
                .containsEntry("goalId", id.toString());

        assertThat(readJson(post(user, GOALS, body("id", UUID.randomUUID().toString(), "title", "x", "startOn",
                "2027-02-01", "targetOn", "2027-01-01")))).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(readJson(post(user, GOALS, body("id", UUID.randomUUID().toString(), "title", "t".repeat(201)))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(readJson(post(user, GOALS, body("id", UUID.randomUUID().toString(), "title", "   "))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(readJson(post(user, GOALS, body("id", id.toString(), "title", "Lại")))).containsEntry("errorCode",
                "ENTITY_ALREADY_EXISTS");
        assertThat(get(user, GOALS + "?status=DONE").getResponse().getStatus()).isEqualTo(400);
        assertThat(items(get(user, GOALS + "?status=ACTIVE"))).hasSize(1);
        assertThat(patch(user, GOALS + "/" + id, body("version", 9, "title", "Cũ")).getResponse().getStatus())
                .isEqualTo(409);
    }
}
