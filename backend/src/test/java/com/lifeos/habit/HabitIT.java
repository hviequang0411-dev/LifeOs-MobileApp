package com.lifeos.habit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
import org.springframework.test.web.servlet.MvcResult;

/** Habit qua REST (PHASE_2 §6, §9): HAB-01, 02, 03, 04, 09, 10. */
class HabitIT extends HabitTestBase {

    @Test
    void Hab_01_scheduleRulesForTheFourTypesAndBitmaskBounds() throws Exception {
        TestUser user = newUser();
        Object[][] valid = {
            {"scheduleType", "DAILY"},
            {"scheduleType", "WEEKDAYS", "weekdays", 1},
            {"scheduleType", "WEEKDAYS", "weekdays", 127},
            {"scheduleType", "TIMES_PER_WEEK", "timesPerWeek", 1},
            {"scheduleType", "TIMES_PER_WEEK", "timesPerWeek", 7},
            {"scheduleType", "EVERY_N_DAYS", "intervalDays", 2},
            {"scheduleType", "EVERY_N_DAYS", "intervalDays", 30}};
        for (Object[] v : valid) {
            assertThat(createHabit(user, UUID.randomUUID(), v).getResponse().getStatus()).as(v[1] + "").isEqualTo(201);
        }
        Object[][] invalid = {
            {"scheduleType", "WEEKDAYS"},
            {"scheduleType", "WEEKDAYS", "weekdays", 0},
            {"scheduleType", "WEEKDAYS", "weekdays", 128},
            {"scheduleType", "DAILY", "weekdays", 3},
            {"scheduleType", "TIMES_PER_WEEK", "timesPerWeek", 8},
            {"scheduleType", "TIMES_PER_WEEK", "timesPerWeek", 0},
            {"scheduleType", "EVERY_N_DAYS", "intervalDays", 1},
            {"scheduleType", "EVERY_N_DAYS", "intervalDays", 31},
            {"scheduleType", "DAILY", "endOn", "2000-01-01"}};
        for (Object[] v : invalid) {
            MvcResult r = createHabit(user, UUID.randomUUID(), v);
            assertThat(r.getResponse().getStatus()).as(java.util.Arrays.toString(v)).isEqualTo(422);
            assertThat(readJson(r)).containsEntry("errorCode", "HABIT_INVALID_SCHEDULE");
        }
        assertThat(readJson(createHabit(user, UUID.randomUUID(), "scheduleType", "HOURLY")))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        // CHECK ở DB chặn cả khi đi vòng service.
        UUID id = habit(user);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.sql(
                "UPDATE habits SET weekdays = 5 WHERE id = :id").param("id", id).update())
                .hasMessageContaining("ck_habits_weekdays_iff");
    }

    @Test
    void Hab_02_checkinDateRangeFollowsStartEndAndTodayPlusOneInTheUsersTimezone() throws Exception {
        for (String zone : List.of("Pacific/Kiritimati", "Pacific/Pago_Pago", "Asia/Ho_Chi_Minh")) {
            TestUser user = newUser();
            timezone(user, zone);
            LocalDate today = today(user);
            UUID id = habit(user, "startOn", today.minusDays(5).toString(), "endOn", today.plusDays(10).toString());
            assertThat(checkin(user, id, today.minusDays(6), "DONE").getResponse().getStatus()).as(zone)
                    .isEqualTo(422);
            assertThat(readJson(checkin(user, id, today.minusDays(6), "DONE")))
                    .containsEntry("errorCode", "HABIT_DATE_OUT_OF_RANGE");
            assertThat(checkin(user, id, today.minusDays(5), "DONE").getResponse().getStatus()).isEqualTo(201);
            assertThat(checkin(user, id, today, "DONE").getResponse().getStatus()).isEqualTo(201);
            assertThat(checkin(user, id, today.plusDays(1), "DONE").getResponse().getStatus()).as(zone + " +1")
                    .isEqualTo(201);
            assertThat(checkin(user, id, today.plusDays(2), "DONE").getResponse().getStatus()).as(zone + " +2")
                    .isEqualTo(422);
        }
        TestUser user = newUser();
        LocalDate today = today(user);
        UUID ended = habit(user, "startOn", today.minusDays(10).toString(), "endOn", today.minusDays(3).toString());
        assertThat(checkin(user, ended, today.minusDays(2), "DONE").getResponse().getStatus()).isEqualTo(422);
        assertThat(checkin(user, ended, today.minusDays(3), "DONE").getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void Hab_03_repeatedPutIsIdempotent() throws Exception {
        TestUser user = newUser();
        UUID id = habit(user);
        LocalDate day = today(user);
        assertThat(checkin(user, id, day, "DONE").getResponse().getStatus()).isEqualTo(201);
        MvcResult again = checkin(user, id, day, "DONE");
        assertThat(again.getResponse().getStatus()).isEqualTo(200);
        assertThat(readJson(again)).containsEntry("version", 1);
        MvcResult changed = checkin(user, id, day, "SKIPPED");
        assertThat(readJson(changed)).containsEntry("status", "SKIPPED").containsEntry("version", 2);
        assertThat(liveCheckins(id, day)).isEqualTo(1);
    }

    @RepeatedTest(20)
    void Hab_03_concurrentPutsOnTheSameDayLeaveOneRowAndNo5xx() throws Exception {
        TestUser user = newUser();
        UUID id = habit(user);
        LocalDate day = today(user).minusDays(1);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                results.add(pool.submit(() -> {
                    go.await();
                    return checkin(user, id, day, "DONE").getResponse().getStatus();
                }));
            }
            go.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : results) {
                statuses.add(f.get(30, TimeUnit.SECONDS));
            }
            assertThat(statuses).allSatisfy(s -> assertThat(s).isIn(200, 201));
            assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(liveCheckins(id, day)).isEqualTo(1);
    }

    @Test
    void Hab_04_undoThenCheckinAgainCreatesANewRow() throws Exception {
        TestUser user = newUser();
        UUID id = habit(user);
        LocalDate day = today(user);
        UUID first = UUID.fromString((String) readJson(checkin(user, id, day, "DONE")).get("id"));
        assertThat(delete(user, HABITS + "/" + id + "/checkins/" + day).getResponse().getStatus()).isEqualTo(204);
        assertThat(delete(user, HABITS + "/" + id + "/checkins/" + day).getResponse().getStatus()).as("idempotent")
                .isEqualTo(204);
        assertThat(row("habit_checkins", first).get("deleted_at")).isNotNull();
        MvcResult again = checkin(user, id, day, "DONE");
        assertThat(again.getResponse().getStatus()).isEqualTo(201);
        assertThat(readJson(again).get("id")).isNotEqualTo(first.toString());
        assertThat(liveCheckins(id, day)).isEqualTo(1);
    }

    @Test
    void Hab_09_archivingKeepsHistoryAndStatsAndEndedHabitsStayReadable() throws Exception {
        TestUser user = newUser();
        LocalDate today = today(user);
        UUID id = habit(user, "startOn", today.minusDays(3).toString());
        for (int i = 3; i >= 1; i--) {
            checkin(user, id, today.minusDays(i), "DONE");
        }
        assertThat(post(user, HABITS + "/" + id + "/archive", body("version", 1)).getResponse().getStatus())
                .isEqualTo(200);
        assertThat(items(get(user, HABITS))).extracting(h -> h.get("id")).doesNotContain(id.toString());
        assertThat(items(get(user, HABITS + "?archived=true"))).extracting(h -> h.get("id")).contains(id.toString());
        Map<String, Object> view = readJson(get(user, HABITS + "/" + id));
        assertThat(view).containsEntry("currentStreak", 3).containsEntry("longestStreak", 3);
        assertThat(view.get("archivedAt")).isNotNull();
        Map<String, Object> stats = readJson(get(user, HABITS + "/" + id + "/stats?from=" + today.minusDays(3)
                + "&to=" + today));
        assertThat(stats).containsEntry("done", 3).containsEntry("scheduled", 3);
        assertThat(items(get(user, HABITS + "/" + id + "/checkins"))).hasSize(3);

        UUID ended = habit(user, "startOn", today.minusDays(9).toString(), "endOn", today.minusDays(5).toString());
        assertThat(get(user, HABITS + "/" + ended).getResponse().getStatus()).isEqualTo(200);
        assertThat(items(get(user, HABITS + "?scheduledOn=" + today))).extracting(h -> h.get("id"))
                .as("habit hết hạn không đến lịch hôm nay").doesNotContain(ended.toString());
    }

    @Test
    void Hab_10_limitOf200HabitsIncludingArchived() throws Exception {
        TestUser user = newUser();
        jdbc.sql("""
                INSERT INTO habits (id, user_id, name, schedule_type, start_on)
                SELECT gen_random_uuid(), :u, 'H' || g, 'DAILY', DATE '2026-01-01' FROM generate_series(1, 199) g""")
                .param("u", user.id()).update();
        UUID last = habit(user);
        assertThat(post(user, HABITS + "/" + last + "/archive", body("version", 1)).getResponse().getStatus())
                .isEqualTo(200);
        MvcResult over = createHabit(user, UUID.randomUUID());
        assertThat(over.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(over)).containsEntry("errorCode", "LIMIT_EXCEEDED");
        assertThat(delete(user, HABITS + "/" + last + "?version=2").getResponse().getStatus()).isEqualTo(204);
        assertThat(createHabit(user, UUID.randomUUID()).getResponse().getStatus()).as("xóa thì có chỗ")
                .isEqualTo(201);
    }

    @Test
    void validationAndStatsRangeAndMassAssignment() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        MvcResult created = createHabit(user, id, "name", "  Đọc sách  ", "color", "#a1b2c3", "reminderTime", "07:30",
                "version", 9, "userId", UUID.randomUUID().toString(), "archivedAt", "2020-01-01T00:00:00Z");
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        assertThat(readJson(created)).containsEntry("name", "Đọc sách").containsEntry("color", "#A1B2C3")
                .containsEntry("reminderTime", "07:30").containsEntry("version", 1).containsEntry("archivedAt", null);
        assertThat(row("habits", id)).containsEntry("user_id", user.id());
        LocalDate today = today(user);
        assertThat(get(user, HABITS + "/" + id + "/stats?from=" + today.minusDays(366) + "&to=" + today)
                .getResponse().getStatus()).isEqualTo(400);
        assertThat(get(user, HABITS + "/" + id + "/stats?from=" + today.minusDays(365) + "&to=" + today)
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(get(user, HABITS + "/stats?from=" + today + "&to=" + today).getResponse().getStatus())
                .isEqualTo(200);
        assertThat(readJson(checkin(user, id, today, "MAYBE"))).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(readJson(createHabit(user, UUID.randomUUID(), "name", " "))).containsEntry("errorCode",
                "VALIDATION_FAILED");
        // PATCH lịch thay cả bộ: đổi sang WEEKDAYS cần weekdays; đổi về DAILY xóa weekdays.
        assertThat(patch(user, HABITS + "/" + id, body("version", 1, "scheduleType", "WEEKDAYS", "weekdays", 5))
                .getResponse().getStatus()).isEqualTo(200);
        MvcResult back = patch(user, HABITS + "/" + id, body("version", 2, "scheduleType", "DAILY"));
        assertThat(readJson(back)).containsEntry("weekdays", null).containsEntry("version", 3);
    }
}
