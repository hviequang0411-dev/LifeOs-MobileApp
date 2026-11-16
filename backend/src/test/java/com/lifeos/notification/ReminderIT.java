package com.lifeos.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.notification.service.ReminderRunService;
import com.lifeos.support.ApiTestBase;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/** Reminder và ReminderScheduler (PHASE_4 D129–D134, NTF-01, NTF-02). Job tắt; test gọi ReminderRunService. */
class ReminderIT extends ApiTestBase {

    @Autowired
    ReminderRunService runs;

    @Autowired
    MeterRegistry meters;

    private UUID task(TestUser user, LocalDate due, String dueTime) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult r = post(user, "/api/v1/tasks", body("id", id.toString(), "title", "Nộp thuế", "dueDate",
                due.toString(), "dueTime", dueTime));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    private Map<String, Object> reminder(TestUser user, Object... fields) throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("id", UUID.randomUUID().toString());
        for (int i = 0; i < fields.length; i += 2) {
            body.put((String) fields[i], fields[i + 1]);
        }
        MvcResult r = post(user, "/api/v1/reminders", json.writeValueAsString(body));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return readJson(r);
    }

    private void zone(TestUser user, String zone) {
        jdbc.sql("INSERT INTO user_settings (id, user_id, timezone) VALUES (:id, :u, :z)"
                        + " ON CONFLICT (user_id) DO UPDATE SET timezone = EXCLUDED.timezone")
                .param("id", UUID.randomUUID()).param("u", user.id()).param("z", zone).update();
    }

    private int notifications(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM notifications WHERE user_id = :u").param("u", userId)
                .query(Integer.class).single();
    }

    private void due(UUID reminderId, Instant at) {
        jdbc.sql("UPDATE reminders SET next_fire_at = :at WHERE id = :id")
                .param("at", at.atOffset(java.time.ZoneOffset.UTC)).param("id", reminderId).update();
    }

    @Test
    void Ntf_01_firingTwiceCreatesOneNotificationAndComputesTheNextOccurrence() throws Exception {
        TestUser user = newUser();
        zone(user, "Asia/Ho_Chi_Minh");
        Map<String, Object> r = reminder(user, "targetType", "NONE", "mode", "LOCAL_AT", "localTime", "07:00");
        UUID id = UUID.fromString((String) r.get("id"));
        Instant next = Instant.parse((String) r.get("nextFireAt"));
        ZonedDateTime local = next.atZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        assertThat(local.toLocalTime().toString()).isEqualTo("07:00");
        assertThat(next).isAfterOrEqualTo(clock.instant()).isBefore(clock.instant().plus(Duration.ofDays(1)));

        Instant fireAt = clock.instant().minusSeconds(30);
        due(id, fireAt);
        assertThat(runs.fire(id)).isTrue();
        due(id, fireAt);
        assertThat(runs.fire(id)).as("cùng mốc → cùng dedupe_key").isFalse();
        assertThat(notifications(user.id())).isEqualTo(1);
        assertThat(jdbc.sql("SELECT dedupe_key FROM notifications WHERE user_id = :u").param("u", user.id())
                .query(String.class).single()).isEqualTo("reminder:" + id + ":" + fireAt.getEpochSecond());
        Map<String, Object> row = jdbc.sql("SELECT * FROM reminders WHERE id = :id").param("id", id).query()
                .singleRow();
        assertThat(row.get("status")).isEqualTo("ACTIVE");
        Object nextFire = row.get("next_fire_at");
        Instant nextAt = nextFire instanceof java.sql.Timestamp t ? t.toInstant()
                : ((java.time.OffsetDateTime) nextFire).toInstant();
        assertThat(nextAt).isAfter(clock.instant());
        assertThat(changeLogs(user.id(), "reminder", id, "UPDATE")).isEqualTo(2);
        assertThat(jdbc.sql("SELECT count(*) FROM change_logs WHERE entity_id = :id AND operation = 'UPDATE'"
                + " AND mutation_id IS NOT NULL").param("id", id).query(Integer.class).single()).isZero();
    }

    @RepeatedTest(20)
    void Ntf_01_parallelRunsCreateExactlyOneNotification() throws Exception {
        TestUser user = newUser();
        Map<String, Object> r = reminder(user, "targetType", "NONE", "mode", "LOCAL_AT", "localTime", "07:00");
        UUID id = UUID.fromString((String) r.get("id"));
        due(id, clock.instant().minusSeconds(5));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                results.add(pool.submit(() -> {
                    go.await();
                    return runs.fire(id);
                }));
            }
            go.countDown();
            int fired = 0;
            for (Future<Boolean> f : results) {
                fired += f.get(30, TimeUnit.SECONDS) ? 1 : 0;
            }
            assertThat(fired).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(notifications(user.id())).isEqualTo(1);
    }

    @Test
    void Ntf_02_missedWithin24HoursFiresOnceOlderIsSkippedWithAMetric() throws Exception {
        TestUser user = newUser();
        UUID recent = UUID.fromString((String) reminder(user, "targetType", "NONE", "mode", "LOCAL_AT",
                "localTime", "07:00").get("id"));
        due(recent, clock.instant().minus(Duration.ofHours(23)));
        assertThat(runs.fire(recent)).isTrue();
        assertThat(runs.fire(recent)).as("chỉ bù một lần").isFalse();
        UUID old = UUID.fromString((String) reminder(user, "targetType", "NONE", "mode", "LOCAL_AT",
                "localTime", "07:00").get("id"));
        double skippedBefore = meters.counter("lifeos.notification.reminder.skipped").count();
        due(old, clock.instant().minus(Duration.ofHours(25)));
        assertThat(runs.fire(old)).isFalse();
        assertThat(meters.counter("lifeos.notification.reminder.skipped").count()).isEqualTo(skippedBefore + 1);
        assertThat(notifications(user.id())).isEqualTo(1);
        Instant next = jdbc.sql("SELECT next_fire_at FROM reminders WHERE id = :id").param("id", old)
                .query(java.time.OffsetDateTime.class).single().toInstant();
        assertThat(next).isAfter(clock.instant());
    }

    @Test
    void taskReminderFollowsTheDueDateCascadesOnDeleteAndRejectsForeignTargets() throws Exception {
        TestUser user = newUser();
        zone(user, "Asia/Ho_Chi_Minh");
        LocalDate dueDate = LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(3);
        UUID task = task(user, dueDate, "10:00");
        Map<String, Object> r = reminder(user, "targetType", "TASK", "targetId", task.toString(), "mode", "OFFSET",
                "offsetMinutes", 30);
        UUID id = UUID.fromString((String) r.get("id"));
        Instant expected = ZonedDateTime.of(dueDate.atTime(9, 30), ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        assertThat(r.get("nextFireAt")).isEqualTo(expected.toString());
        Map<String, Object> moved = readJson(patch(user, "/api/v1/tasks/" + task, body("version", 1, "dueTime",
                "15:00")));
        assertThat(moved.get("version")).isEqualTo(2);
        assertThat(readJson(get(user, "/api/v1/reminders/" + id)).get("nextFireAt"))
                .isEqualTo(expected.plus(Duration.ofHours(5)).toString());

        TestUser other = newUser();
        MvcResult foreign = post(other, "/api/v1/reminders", body("id", UUID.randomUUID().toString(), "targetType",
                "TASK", "targetId", task.toString(), "mode", "OFFSET", "offsetMinutes", 5));
        assertThat(foreign.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(foreign).get("errorCode")).isEqualTo("REMINDER_TARGET_NOT_FOUND");
        MvcResult offsetOnHabit = post(user, "/api/v1/reminders", body("id", UUID.randomUUID().toString(),
                "targetType", "NONE", "mode", "OFFSET", "offsetMinutes", 5));
        assertThat(offsetOnHabit.getResponse().getStatus()).isEqualTo(400);
        for (int i = 0; i < 9; i++) {
            reminder(user, "targetType", "TASK", "targetId", task.toString(), "mode", "OFFSET", "offsetMinutes", i);
        }
        MvcResult eleventh = post(user, "/api/v1/reminders", body("id", UUID.randomUUID().toString(), "targetType",
                "TASK", "targetId", task.toString(), "mode", "OFFSET", "offsetMinutes", 99));
        assertThat(readJson(eleventh).get("errorCode")).isEqualTo("LIMIT_EXCEEDED");

        assertThat(delete(user, "/api/v1/tasks/" + task + "?version=2").getResponse().getStatus()).isEqualTo(204);
        assertThat(jdbc.sql("SELECT count(*) FROM reminders WHERE target_id = :t AND deleted_at IS NULL")
                .param("t", task).query(Integer.class).single()).isZero();
        List<String> order = jdbc.sql("SELECT entity_type FROM change_logs WHERE user_id = :u AND operation ="
                + " 'DELETE' ORDER BY seq").param("u", user.id()).query(String.class).list();
        assertThat(order).hasSize(11).last().isEqualTo("task");
    }

    @Test
    void timezoneChangeRecomputesLocalTimeRemindersInTheSameTransaction() throws Exception {
        TestUser user = newUser();
        zone(user, "Asia/Ho_Chi_Minh");
        UUID settingsId = jdbc.sql("SELECT id FROM user_settings WHERE user_id = :u").param("u", user.id())
                .query(UUID.class).single();
        Map<String, Object> r = reminder(user, "targetType", "NONE", "mode", "LOCAL_AT", "localTime", "07:00");
        UUID id = UUID.fromString((String) r.get("id"));
        MvcResult changed = patch(user, "/api/v1/me/settings", body("version", 1, "timezone", "Asia/Tokyo"));
        assertThat(changed.getResponse().getStatus()).as(changed.getResponse().getContentAsString()).isEqualTo(200);
        Instant next = Instant.parse((String) readJson(get(user, "/api/v1/reminders/" + id)).get("nextFireAt"));
        assertThat(next.atZone(ZoneId.of("Asia/Tokyo")).toLocalTime().toString()).isEqualTo("07:00");
        List<Map<String, Object>> logs = jdbc.sql("SELECT entity_id, operation FROM change_logs WHERE user_id = :u"
                + " AND operation = 'UPDATE' ORDER BY seq").param("u", user.id()).query().listOfRows();
        assertThat(logs).extracting(l -> l.get("entity_id")).containsExactly(id, settingsId);
        assertThat(LocalDateTime.ofInstant(next, ZoneId.of("UTC"))).isNotNull();
    }
}
