package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Sync cho routine, routine_step, routine_log, routine_step_log (PHASE_2 §7, D241–D246): CREATE/UPDATE/DELETE, khóa duy
 * nhất theo ngày/bước → CONFLICT ALREADY_EXISTS, trường bất biến, tham chiếu 422, cascade với mutation_id NULL.
 */
class RoutineSyncIT extends SyncTestBase {

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private UUID routine(TestUser user) throws Exception {
        UUID id = UUID.randomUUID();
        assertThat(pushOne(user, mutation("routine", id, "CREATE", 0, payload("name", "Tối", "kind", "WEEKLY",
                "weekdays", 127)))).containsEntry("status", "APPLIED");
        return id;
    }

    private UUID step(TestUser user, UUID routine, long position) throws Exception {
        UUID id = UUID.randomUUID();
        assertThat(pushOne(user, mutation("routine_step", id, "CREATE", 0, payload("routineId", routine.toString(),
                "title", "Đánh răng", "position", position)))).containsEntry("status", "APPLIED");
        return id;
    }

    private Map<String, Object> startLog(UUID id, UUID routine, LocalDate day, int total) {
        return mutation("routine_log", id, "CREATE", 0, payload("routineId", routine.toString(),
                "localDate", day.toString(), "totalSteps", total));
    }

    private Map<String, Object> stepResult(UUID id, UUID log, UUID step, String status) {
        return mutation("routine_step_log", id, "CREATE", 0, payload("routineLogId", log.toString(),
                "stepId", step.toString(), "status", status));
    }

    @Test
    void runsAndStepResultsUseAlreadyExistsForTheirNaturalKeysAndServerSetCompletion() throws Exception {
        TestUser user = newUser();
        UUID routine = routine(user);
        UUID a = step(user, routine, 1000);
        UUID b = step(user, routine, 2000);
        UUID log = UUID.randomUUID();
        assertThat(pushOne(user, startLog(log, routine, today(), 2))).containsEntry("status", "APPLIED");
        Map<String, Object> dup = pushOne(user, startLog(UUID.randomUUID(), routine, today(), 2));
        assertThat(dup).containsEntry("status", "CONFLICT").containsEntry("conflictReason", "ALREADY_EXISTS");
        assertThat(((Map<?, ?>) dup.get("serverSnapshot")).get("entityId")).isEqualTo(log.toString());

        UUID resultA = UUID.randomUUID();
        assertThat(pushOne(user, stepResult(resultA, log, a, "DONE"))).containsEntry("status", "APPLIED");
        assertThat(pushOne(user, stepResult(UUID.randomUUID(), log, a, "SKIPPED"))).containsEntry("status",
                "CONFLICT").containsEntry("conflictReason", "ALREADY_EXISTS");
        assertThat(pushOne(user, stepResult(UUID.randomUUID(), log, b, "SKIPPED"))).containsEntry("status",
                "APPLIED");
        assertThat(jdbc.sql("SELECT completed_at IS NOT NULL FROM routine_logs WHERE id = :id").param("id", log)
                .query(Boolean.class).single()).isTrue();
        assertThat(changeLogRows(user.id(), log)).last().satisfies(l -> assertThat(l)
                .containsEntry("operation", "UPDATE").containsEntry("mutation_id", null));

        assertThat(pushOne(user, mutation("routine_log", log, "UPDATE", 2, payload("totalSteps", 3))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(pushOne(user, mutation("routine_step_log", resultA, "UPDATE", 1, payload("stepId",
                b.toString())))).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(pushOne(user, mutation("routine_step_log", resultA, "DELETE", 1, null))).containsEntry("status",
                "APPLIED");
        assertThat(jdbc.sql("SELECT completed_at IS NULL FROM routine_logs WHERE id = :id").param("id", log)
                .query(Boolean.class).single()).as("hoàn tác bước làm lượt chưa xong").isTrue();

        assertThat(pushOne(user, startLog(UUID.randomUUID(), routine, today().minusDays(1), 31)))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(pushOne(user, startLog(UUID.randomUUID(), UUID.randomUUID(), today(), 1)))
                .containsEntry("errorCode", "ROUTINE_NOT_FOUND");
        UUID otherRoutine = routine(user);
        UUID foreignStep = step(user, otherRoutine, 1000);
        assertThat(pushOne(user, stepResult(UUID.randomUUID(), log, foreignStep, "DONE")))
                .containsEntry("errorCode", "ROUTINE_STEP_NOT_FOUND");
        assertThat(pushOne(user, stepResult(UUID.randomUUID(), UUID.randomUUID(), a, "DONE")))
                .containsEntry("errorCode", "ROUTINE_LOG_NOT_FOUND");
    }

    @Test
    void stepsPositionsAndRoutineCascadeOverSync() throws Exception {
        TestUser user = newUser();
        UUID routine = routine(user);
        UUID a = step(user, routine, 1000);
        assertThat(pushOne(user, mutation("routine_step", UUID.randomUUID(), "CREATE", 0, payload("routineId",
                routine.toString(), "title", "Trùng", "position", 1000)))).containsEntry("errorCode",
                "VALIDATION_FAILED");
        assertThat(pushOne(user, mutation("routine_step", a, "UPDATE", 1, payload("position", 500))))
                .containsEntry("entityVersion", 2);
        assertThat(pushOne(user, mutation("routine_step", a, "UPDATE", 2, payload("routineId",
                UUID.randomUUID().toString())))).containsEntry("errorCode", "VALIDATION_FAILED");
        UUID log = UUID.randomUUID();
        pushOne(user, startLog(log, routine, today(), 1));
        UUID result = UUID.randomUUID();
        pushOne(user, stepResult(result, log, a, "DONE"));

        Map<String, Object> delete = mutation("routine", routine, "DELETE", 1, null);
        assertThat(pushOne(user, delete)).containsEntry("status", "APPLIED");

        for (UUID child : List.of(a, log, result)) {
            assertThat(changeLogRows(user.id(), child)).last().satisfies(l -> assertThat(l)
                    .containsEntry("operation", "DELETE").containsEntry("mutation_id", null));
        }
        assertThat(changeLogRows(user.id(), routine)).last().satisfies(l -> assertThat(l)
                .containsEntry("mutation_id", mutationIdOf(delete)));
        assertThat(pushOne(user, mutation("routine_step", a, "UPDATE", 2, payload("title", "X"))))
                .as("tombstone").containsEntry("status", "CONFLICT");
        assertThat(pushOne(user, mutation("routine_step", a, "DELETE", 2, null))).containsEntry("status", "APPLIED");
    }

    @Test
    void routineLimitAndScheduleRulesOverSync() throws Exception {
        TestUser user = newUser();
        assertThat(pushOne(user, mutation("routine", UUID.randomUUID(), "CREATE", 0, payload("name", "R",
                "kind", "DAILY", "weekdays", 3)))).containsEntry("errorCode", "ROUTINE_INVALID_SCHEDULE");
        jdbc.sql("""
                INSERT INTO routines (id, user_id, name, kind)
                SELECT gen_random_uuid(), :u, 'R' || g, 'DAILY' FROM generate_series(1, 50) g""")
                .param("u", user.id()).update();
        assertThat(pushOne(user, mutation("routine", UUID.randomUUID(), "CREATE", 0, payload("name", "Quá",
                "kind", "DAILY")))).containsEntry("errorCode", "LIMIT_EXCEEDED");
    }
}
