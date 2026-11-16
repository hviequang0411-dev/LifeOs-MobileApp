package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.test.web.servlet.MvcResult;

/**
 * Sync cho habit và habit_checkin (PHASE_2 §7, D235, D236, D237): HAB-07, HAB-08, HAB-10 (sync) và các kết quả
 * tương đương SYN (CONFLICT, REJECTED, DUPLICATE, tombstone, cascade, mutation_id).
 */
class HabitSyncIT extends SyncTestBase {

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private UUID habit(TestUser user) throws Exception {
        UUID id = UUID.randomUUID();
        assertThat(pushOne(user, mutation("habit", id, "CREATE", 0, payload("name", "Thiền", "scheduleType", "DAILY",
                "startOn", today().minusDays(10).toString())))).containsEntry("status", "APPLIED");
        return id;
    }

    private Map<String, Object> checkinCreate(UUID id, UUID habitId, LocalDate day, String status) {
        return mutation("habit_checkin", id, "CREATE", 0, payload("habitId", habitId.toString(),
                "localDate", day.toString(), "status", status));
    }

    @Test
    void Hab_07_sameDayCheckinFromTwoDevicesIsConflictAlreadyExistsWithTheServerRow() throws Exception {
        TestUser user = newUser();
        UUID habit = habit(user);
        LocalDate day = today();
        UUID phone = UUID.randomUUID();
        UUID tablet = UUID.randomUUID();
        assertThat(pushOne(user, checkinCreate(phone, habit, day, "DONE"))).containsEntry("status", "APPLIED");

        Map<String, Object> second = pushOne(user, checkinCreate(tablet, habit, day, "SKIPPED"));

        assertThat(second).containsEntry("status", "CONFLICT").containsEntry("conflictReason", "ALREADY_EXISTS");
        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot = (Map<String, Object>) second.get("serverSnapshot");
        assertThat(snapshot).containsEntry("entityType", "habit_checkin").containsEntry("entityId", phone.toString())
                .containsEntry("version", 1).containsEntry("deleted", false);
        assertThat(snapshot.get("data")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("status", "DONE").containsEntry("localDate", day.toString())
                .containsEntry("habitId", habit.toString());
        assertThat(jdbc.sql("SELECT count(*) FROM habit_checkins WHERE id = :id").param("id", tablet)
                .query(Integer.class).single()).isZero();
        // Phát lại giữ nguyên lý do.
        assertThat(pushOne(user, checkinCreate(tablet, habit, day, "SKIPPED"))).containsEntry("status", "CONFLICT")
                .containsEntry("conflictReason", "ALREADY_EXISTS");
    }

    @Test
    void checkinMatrixUpdateDeleteTombstoneReferenceAndImmutableFields() throws Exception {
        TestUser user = newUser();
        UUID habit = habit(user);
        UUID checkin = UUID.randomUUID();
        LocalDate day = today().minusDays(1);
        pushOne(user, checkinCreate(checkin, habit, day, "DONE"));

        Map<String, Object> update = mutation("habit_checkin", checkin, "UPDATE", 1,
                payload("status", "SKIPPED", "note", "Ốm"));
        assertThat(pushOne(user, update)).containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
        assertThat(changeLogRows(user.id(), checkin)).last().satisfies(l -> assertThat(l)
                .containsEntry("mutation_id", mutationIdOf(update)));
        assertThat(pushOne(user, mutation("habit_checkin", checkin, "UPDATE", 1, payload("status", "DONE"))))
                .containsEntry("status", "CONFLICT").doesNotContainKey("conflictReason");
        assertThat(pushOne(user, mutation("habit_checkin", checkin, "UPDATE", 2,
                payload("localDate", day.minusDays(1).toString())))).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(pushOne(user, mutation("habit_checkin", checkin, "DELETE", 2, null))).containsEntry("status",
                "APPLIED").containsEntry("entityVersion", 3);
        assertThat(pushOne(user, mutation("habit_checkin", checkin, "DELETE", 2, null))).as("tombstone DELETE")
                .containsEntry("status", "APPLIED").containsEntry("entityVersion", 3);
        assertThat(pushOne(user, mutation("habit_checkin", checkin, "UPDATE", 3, payload("status", "DONE"))))
                .containsEntry("status", "CONFLICT");
        // Sau khi hoàn tác, check-in lại cùng ngày (id mới) được.
        assertThat(pushOne(user, checkinCreate(UUID.randomUUID(), habit, day, "DONE"))).containsEntry("status",
                "APPLIED");
        // Tham chiếu: habit không tồn tại / của user khác → REJECTED HABIT_NOT_FOUND (không phải tombstone của đích).
        TestUser other = newUser();
        UUID foreign = habit(other);
        assertThat(push(user, checkinCreate(UUID.randomUUID(), foreign, day, "DONE"),
                checkinCreate(UUID.randomUUID(), UUID.randomUUID(), day, "DONE")))
                .allSatisfy(r -> assertThat(r).containsEntry("status", "REJECTED")
                        .containsEntry("errorCode", "HABIT_NOT_FOUND"));
        assertThat(pushOne(user, checkinCreate(UUID.randomUUID(), habit, today().plusDays(2), "DONE")))
                .containsEntry("errorCode", "HABIT_DATE_OUT_OF_RANGE");
        assertThat(pushOne(user, mutation("habit_checkin", UUID.randomUUID(), "UPDATE", 1, payload("status", "DONE"))))
                .containsEntry("errorCode", "HABIT_CHECKIN_NOT_FOUND");
    }

    @Test
    void habitMatrixDuplicateConflictAndCascadeDeleteWithNullMutationIdOnChildren() throws Exception {
        TestUser user = newUser();
        UUID habit = habit(user);
        List<UUID> checkins = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            UUID id = UUID.randomUUID();
            checkins.add(id);
            pushOne(user, checkinCreate(id, habit, today().minusDays(i), "DONE"));
        }
        Map<String, Object> rename = mutation("habit", habit, "UPDATE", 1, payload("name", "Thiền sâu"));
        assertThat(pushOne(user, rename)).containsEntry("entityVersion", 2);
        assertThat(pushOne(user, rename)).containsEntry("status", "DUPLICATE");
        assertThat(pushOne(user, mutation("habit", habit, "UPDATE", 1, payload("name", "Cũ"))))
                .containsEntry("status", "CONFLICT");
        assertThat(pushOne(user, mutation("habit", habit, "UPDATE", 2, payload("scheduleType", "WEEKDAYS"))))
                .containsEntry("errorCode", "HABIT_INVALID_SCHEDULE");

        Map<String, Object> delete = mutation("habit", habit, "DELETE", 2, null);
        assertThat(pushOne(user, delete)).containsEntry("status", "APPLIED").containsEntry("entityVersion", 3);

        for (UUID c : checkins) {
            assertThat(jdbc.sql("SELECT deleted_at IS NOT NULL FROM habit_checkins WHERE id = :id").param("id", c)
                    .query(Boolean.class).single()).isTrue();
            assertThat(changeLogRows(user.id(), c)).last().satisfies(l -> assertThat(l)
                    .containsEntry("operation", "DELETE").containsEntry("mutation_id", null));
        }
        Map<String, Object> habitLog = changeLogRows(user.id(), habit).get(2);
        assertThat(habitLog).containsEntry("operation", "DELETE").containsEntry("mutation_id", mutationIdOf(delete));
        assertThat(pushOne(user, mutation("habit", habit, "UPDATE", 3, payload("name", "X"))))
                .containsEntry("status", "CONFLICT");
        assertThat(pushOne(user, checkinCreate(UUID.randomUUID(), habit, today(), "DONE")))
                .containsEntry("errorCode", "HABIT_NOT_FOUND");
    }

    @Test
    void Hab_10_limitExceededAlsoViaSync() throws Exception {
        TestUser user = newUser();
        jdbc.sql("""
                INSERT INTO habits (id, user_id, name, schedule_type, start_on)
                SELECT gen_random_uuid(), :u, 'H' || g, 'DAILY', DATE '2026-01-01' FROM generate_series(1, 200) g""")
                .param("u", user.id()).update();
        assertThat(pushOne(user, mutation("habit", UUID.randomUUID(), "CREATE", 0, payload("name", "Quá",
                "scheduleType", "DAILY")))).containsEntry("status", "REJECTED").containsEntry("errorCode",
                "LIMIT_EXCEEDED");
    }

    /** HAB-08: xóa Habit (cascade) đồng thời với check-in mới: không deadlock, không check-in mồ côi, seq liên tục. */
    @RepeatedTest(20)
    void Hab_08_cascadeDeleteVersusConcurrentCheckin() throws Exception {
        TestUser user = newUser();
        UUID habit = habit(user);
        pushOne(user, checkinCreate(UUID.randomUUID(), habit, today().minusDays(2), "DONE"));
        UUID racing = UUID.randomUUID();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch go = new CountDownLatch(1);
            Future<MvcResult> delete = pool.submit(() -> {
                go.await();
                return pushResult(user, mutation("habit", habit, "DELETE", 1, null));
            });
            Future<MvcResult> add = pool.submit(() -> {
                go.await();
                return pushResult(user, checkinCreate(racing, habit, today(), "DONE"));
            });
            go.countDown();
            MvcResult deleted = delete.get(30, TimeUnit.SECONDS);
            MvcResult added = add.get(30, TimeUnit.SECONDS);
            assertThat(deleted.getResponse().getStatus()).as("không 503").isEqualTo(200);
            assertThat(added.getResponse().getStatus()).isEqualTo(200);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.sql("""
                SELECT count(*) FROM habit_checkins c JOIN habits h ON h.id = c.habit_id
                WHERE c.user_id = :u AND c.deleted_at IS NULL AND h.deleted_at IS NOT NULL""")
                .param("u", user.id()).query(Integer.class).single()).as("check-in mồ côi").isZero();
        List<Long> seqs = jdbc.sql("SELECT seq FROM change_logs WHERE user_id = :u ORDER BY seq")
                .param("u", user.id()).query(Long.class).list();
        for (int i = 0; i < seqs.size(); i++) {
            assertThat(seqs.get(i)).isEqualTo(i + 1L);
        }
    }
}
