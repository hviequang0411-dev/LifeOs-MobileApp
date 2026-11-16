package com.lifeos.routine;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

/** Routine qua REST (PHASE_2 §6, §9): RTN-01..06. */
class RoutineIT extends ApiTestBase {

    private static final String ROUTINES = "/api/v1/routines";

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private UUID routine(TestUser user, Object... extra) throws Exception {
        UUID id = UUID.randomUUID();
        Object[] fields = new Object[extra.length + 6];
        fields[0] = "id";
        fields[1] = id.toString();
        fields[2] = "name";
        fields[3] = "Buổi sáng";
        fields[4] = "kind";
        fields[5] = "DAILY";
        System.arraycopy(extra, 0, fields, 6, extra.length);
        MvcResult r = post(user, ROUTINES, body(fields));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    private MvcResult addStep(TestUser user, UUID routine, Object... extra) throws Exception {
        Object[] fields = new Object[extra.length + 4];
        fields[0] = "id";
        fields[1] = UUID.randomUUID().toString();
        fields[2] = "title";
        fields[3] = "Uống nước";
        System.arraycopy(extra, 0, fields, 4, extra.length);
        return post(user, ROUTINES + "/" + routine + "/steps", body(fields));
    }

    private UUID step(TestUser user, UUID routine, Object... extra) throws Exception {
        MvcResult r = addStep(user, routine, extra);
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return UUID.fromString((String) readJson(r).get("id"));
    }

    private MvcResult start(TestUser user, UUID routine, LocalDate day, int total) throws Exception {
        return call(user, HttpMethod.PUT, ROUTINES + "/" + routine + "/logs/" + day, body("totalSteps", total));
    }

    private MvcResult result(TestUser user, UUID log, UUID step, String status) throws Exception {
        return call(user, HttpMethod.PUT, "/api/v1/routine-logs/" + log + "/steps/" + step, body("status", status));
    }

    private Map<String, Object> logRow(UUID id) {
        return jdbc.sql("SELECT * FROM routine_logs WHERE id = :id").param("id", id).query().singleRow();
    }

    @Test
    void Rtn_01_stepsLimitPositionUniquenessAppendAndInsertBetween() throws Exception {
        TestUser user = newUser();
        UUID routine = routine(user);
        UUID first = step(user, routine);
        UUID second = step(user, routine);
        List<Map<String, Object>> steps = steps(user, routine);
        assertThat(steps).extracting(s -> ((Number) s.get("position")).longValue()).containsExactly(1000L, 2000L);
        UUID middle = step(user, routine, "position", 1500);
        assertThat(steps(user, routine)).extracting(s -> s.get("id"))
                .containsExactly(first.toString(), middle.toString(), second.toString());
        MvcResult taken = addStep(user, routine, "position", 1500);
        assertThat(taken.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(taken).get("fieldErrors").toString()).contains("position");
        for (int i = 3; i < 30; i++) {
            step(user, routine);
        }
        MvcResult over = addStep(user, routine);
        assertThat(over.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(over)).containsEntry("errorCode", "LIMIT_EXCEEDED");
        assertThat(readJson(addStep(user, routine, "durationMinutes", 241))).containsEntry("errorCode",
                "VALIDATION_FAILED");
        assertThat(readJson(post(user, ROUTINES, body("id", UUID.randomUUID().toString(), "name", "R",
                "kind", "WEEKLY")))).containsEntry("errorCode", "ROUTINE_INVALID_SCHEDULE");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> steps(TestUser user, UUID routine) throws Exception {
        return (List<Map<String, Object>>) readJson(get(user, ROUTINES + "/" + routine)).get("steps");
    }

    @Test
    void Rtn_02_startIsIdempotentOneRunPerDayAndTotalStepsBounds() throws Exception {
        TestUser user = newUser();
        UUID routine = routine(user);
        LocalDate day = today();
        MvcResult first = start(user, routine, day, 3);
        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        MvcResult again = start(user, routine, day, 5);
        assertThat(again.getResponse().getStatus()).isEqualTo(200);
        assertThat(readJson(again)).containsEntry("id", readJson(first).get("id")).containsEntry("totalSteps", 3);
        assertThat(start(user, routine, day.minusDays(1), 0).getResponse().getStatus()).isEqualTo(400);
        assertThat(start(user, routine, day.minusDays(1), 31).getResponse().getStatus()).isEqualTo(400);
    }

    @RepeatedTest(20)
    void Rtn_02_concurrentStartsCreateOneRun() throws Exception {
        TestUser user = newUser();
        UUID routine = routine(user);
        LocalDate day = today();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                results.add(pool.submit(() -> {
                    go.await();
                    return start(user, routine, day, 2).getResponse().getStatus();
                }));
            }
            go.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : results) {
                statuses.add(f.get(30, TimeUnit.SECONDS));
            }
            assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
            assertThat(statuses).allSatisfy(s -> assertThat(s).isIn(200, 201));
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.sql("SELECT count(*) FROM routine_logs WHERE routine_id = :r AND deleted_at IS NULL")
                .param("r", routine).query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void Rtn_03_stepResultsUndoToggleAndServerSetCompletion() throws Exception {
        TestUser user = newUser();
        UUID routine = routine(user);
        UUID a = step(user, routine);
        UUID b = step(user, routine);
        UUID log = UUID.fromString((String) readJson(start(user, routine, today(), 2)).get("id"));

        assertThat(result(user, log, a, "DONE").getResponse().getStatus()).isEqualTo(201);
        assertThat(logRow(log).get("completed_at")).isNull();
        assertThat(result(user, log, b, "SKIPPED").getResponse().getStatus()).isEqualTo(201);
        assertThat(logRow(log).get("completed_at")).as("done + skipped = total").isNotNull();
        assertThat(result(user, log, b, "DONE").getResponse().getStatus()).as("đổi SKIPPED→DONE").isEqualTo(200);
        assertThat(result(user, log, b, "DONE").getResponse().getStatus()).as("idempotent").isEqualTo(200);
        assertThat(delete(user, "/api/v1/routine-logs/" + log + "/steps/" + a).getResponse().getStatus())
                .isEqualTo(204);
        assertThat(logRow(log).get("completed_at")).as("hoàn tác làm lượt chưa xong").isNull();
        assertThat(result(user, log, a, "DONE").getResponse().getStatus()).isEqualTo(201);
        assertThat(logRow(log).get("completed_at")).isNotNull();
        // Bước của routine khác → 404 (không lộ tồn tại).
        UUID otherRoutine = routine(user);
        UUID foreignStep = step(user, otherRoutine);
        assertThat(result(user, log, foreignStep, "DONE").getResponse().getStatus()).isEqualTo(404);
        assertThat(readJson(result(user, log, a, "LATER"))).containsEntry("errorCode", "VALIDATION_FAILED");
    }

    @Test
    void Rtn_04_editingOrDeletingStepsDoesNotChangeStartedOrFinishedRuns() throws Exception {
        TestUser user = newUser();
        UUID routine = routine(user);
        UUID a = step(user, routine);
        UUID b = step(user, routine);
        UUID done = UUID.fromString((String) readJson(start(user, routine, today().minusDays(1), 2)).get("id"));
        result(user, done, a, "DONE");
        result(user, done, b, "DONE");
        UUID running = UUID.fromString((String) readJson(start(user, routine, today(), 2)).get("id"));
        result(user, running, a, "DONE");

        assertThat(delete(user, "/api/v1/routine-steps/" + b + "?version=1").getResponse().getStatus())
                .isEqualTo(204);
        step(user, routine);
        assertThat(patch(user, "/api/v1/routine-steps/" + a, body("version", 1, "title", "Đổi tên"))
                .getResponse().getStatus()).isEqualTo(200);

        assertThat(logRow(done)).containsEntry("total_steps", 2);
        assertThat(logRow(done).get("completed_at")).isNotNull();
        assertThat(logRow(running)).containsEntry("total_steps", 2);
        assertThat(logRow(running).get("completed_at")).isNull();
        assertThat(jdbc.sql("SELECT count(*) FROM routine_step_logs WHERE routine_log_id = :l AND deleted_at IS NULL")
                .param("l", done).query(Integer.class).single()).as("kết quả bước cũ giữ nguyên").isEqualTo(2);
        // Bước đã xóa vẫn ghi được kết quả cho lượt dở (lượt dựa trên ảnh chụp lúc Start).
        assertThat(result(user, running, b, "DONE").getResponse().getStatus()).isEqualTo(201);
        assertThat(logRow(running).get("completed_at")).isNotNull();
    }

    @Test
    void Rtn_05_statsNullWhenNothingScheduledAndPartialCounting() throws Exception {
        TestUser user = newUser();
        UUID routine = routine(user);
        jdbc.sql("UPDATE routines SET created_at = :t WHERE id = :id")
                .param("t", java.time.OffsetDateTime.ofInstant(clock.instant().minusSeconds(4 * 86400L),
                        java.time.ZoneOffset.UTC)).param("id", routine).update();
        UUID a = step(user, routine);
        UUID b = step(user, routine);
        LocalDate today = today();
        UUID full = UUID.fromString((String) readJson(start(user, routine, today.minusDays(2), 2)).get("id"));
        result(user, full, a, "DONE");
        result(user, full, b, "DONE");
        UUID half = UUID.fromString((String) readJson(start(user, routine, today.minusDays(1), 2)).get("id"));
        result(user, half, a, "DONE");

        Map<String, Object> stats = readJson(get(user, ROUTINES + "/" + routine + "/stats?from=" + today.minusDays(3)
                + "&to=" + today));
        assertThat(stats).containsEntry("scheduled", 3).containsEntry("completed", 1).containsEntry("partial", 1)
                .containsEntry("completionRate", 0.3333);
        Map<String, Object> future = readJson(get(user, ROUTINES + "/" + routine + "/stats?from="
                + today.plusDays(1) + "&to=" + today.plusDays(5)));
        assertThat(future).containsEntry("scheduled", 0).containsEntry("completionRate", null);
        List<?> history = json.readValue(get(user, ROUTINES + "/" + routine + "/logs?from=" + today.minusDays(3)
                + "&to=" + today).getResponse().getContentAsString(), List.class);
        assertThat(history).hasSize(2);
    }

    @Test
    void Rtn_06_deletingARoutineCascadesStepsRunsAndResults() throws Exception {
        TestUser user = newUser();
        UUID routine = routine(user);
        UUID a = step(user, routine);
        UUID log = UUID.fromString((String) readJson(start(user, routine, today(), 1)).get("id"));
        UUID result = UUID.fromString((String) readJson(result(user, log, a, "DONE")).get("id"));

        assertThat(delete(user, ROUTINES + "/" + routine + "?version=1").getResponse().getStatus()).isEqualTo(204);

        for (String table : List.of("routine_steps", "routine_logs", "routine_step_logs")) {
            assertThat(jdbc.sql("SELECT count(*) FROM " + table + " WHERE user_id = :u AND deleted_at IS NULL")
                    .param("u", user.id()).query(Integer.class).single()).as(table).isZero();
        }
        assertThat(jdbc.sql("SELECT mutation_id FROM change_logs WHERE user_id = :u AND entity_id IN (:ids)"
                        + " AND operation = 'DELETE'").param("u", user.id()).param("ids", List.of(a, log, result))
                .query().listOfRows()).hasSize(3).allSatisfy(r -> assertThat(r.get("mutation_id")).isNull());
        assertThat(get(user, ROUTINES + "/" + routine).getResponse().getStatus()).isEqualTo(404);
        assertThat(start(user, routine, today(), 1).getResponse().getStatus()).isEqualTo(404);
    }
}
