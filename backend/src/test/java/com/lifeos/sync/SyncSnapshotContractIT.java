package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SYN-42 (SYNC_PROTOCOL §6.1, D94): {@code data} của pull — và của serverSnapshot vì cùng handler dựng — có ĐÚNG tập
 * trường liệt kê ở §6.1, VÀ trùng (cả giá trị) với Response REST bỏ id/version, cho cả 4 entityType. Chống trôi lệch
 * giữa REST, sync và tài liệu.
 */
class SyncSnapshotContractIT extends SyncTestBase {

    /** Chép nguyên từ SYNC_PROTOCOL §6.1. */
    private static final Map<String, Set<String>> SECTION_6_1 = Map.of(
            "task", Set.of("title", "description", "status", "priority", "dueDate", "dueTime", "projectId",
                    "categoryId", "goalId", "milestoneId", "completedAt", "createdAt", "updatedAt"),
            "project", Set.of("name", "description", "archivedAt", "createdAt", "updatedAt"),
            "category", Set.of("name", "createdAt", "updatedAt"),
            "user_settings", Set.of("timezone", "locale", "defaultCurrency", "weekStartDay", "quietStart", "quietEnd",
                    "showDetailsOnLockScreen", "updatedAt"));

    private Map<String, Object> restWithoutIdAndVersion(TestUser user, String path) throws Exception {
        MvcResult result = get(user, path);
        assertThat(result.getResponse().getStatus()).as(path).isEqualTo(200);
        Map<String, Object> body = new LinkedHashMap<>(readJson(result));
        body.remove("id");
        body.remove("version");
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Object>> pulledData(TestUser user) throws Exception {
        MvcResult result = get(user, "/api/v1/sync/pull?limit=500");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<String, Map<String, Object>> byType = new LinkedHashMap<>();
        for (Map<String, Object> change : (List<Map<String, Object>>) readJson(result).get("changes")) {
            assertThat(change).containsEntry("operation", "UPSERT");
            byType.put((String) change.get("entityType"), (Map<String, Object>) change.get("data"));
        }
        return byType;
    }

    @Test
    void Sync_42_pullDataMatchesSection61AndTheRestResponseForAllEntityTypes() throws Exception {
        TestUser user = newUser();
        UUID settings = UUID.randomUUID();
        jdbc.sql("INSERT INTO user_settings (id, user_id, timezone) VALUES (:id, :u, 'Asia/Ho_Chi_Minh')")
                .param("id", settings).param("u", user.id()).update();
        UUID project = createProject(user);
        UUID category = createCategory(user);
        UUID task = UUID.randomUUID();
        assertThat(pushOne(user, mutation("task", task, "CREATE", 0, payload("title", "Đủ trường",
                "description", "mô tả", "status", "DONE", "priority", "HIGH", "dueDate", "2026-11-30",
                "dueTime", "08:15", "projectId", project.toString(), "categoryId", category.toString()))))
                .containsEntry("status", "APPLIED");
        assertThat(pushOne(user, mutation("project", project, "UPDATE", 1, payload("archived", true))))
                .containsEntry("status", "APPLIED");
        assertThat(pushOne(user, mutation("user_settings", settings, "UPDATE", 1, payload("weekStartDay", 7))))
                .containsEntry("status", "APPLIED");

        Map<String, Map<String, Object>> pulled = pulledData(user);
        Map<String, Map<String, Object>> rest = Map.of(
                "task", restWithoutIdAndVersion(user, "/api/v1/tasks/" + task),
                "project", restWithoutIdAndVersion(user, "/api/v1/projects/" + project),
                "category", restWithoutIdAndVersion(user, "/api/v1/categories/" + category),
                "user_settings", restWithoutIdAndVersion(user, "/api/v1/me/settings"));

        assertThat(pulled.keySet()).containsExactlyInAnyOrderElementsOf(SECTION_6_1.keySet());
        for (String type : SECTION_6_1.keySet()) {
            assertThat(pulled.get(type).keySet()).as(type + ": data = danh sách §6.1")
                    .containsExactlyInAnyOrderElementsOf(SECTION_6_1.get(type));
            assertThat(rest.get(type).keySet()).as(type + ": Response REST bỏ id/version = §6.1")
                    .containsExactlyInAnyOrderElementsOf(SECTION_6_1.get(type));
            assertThat(pulled.get(type)).as(type + ": cùng giá trị, cùng định dạng với REST").isEqualTo(rest.get(type));
        }
        assertThat(pulled.get("task")).containsEntry("dueTime", "08:15").containsEntry("dueDate", "2026-11-30");
        assertThat(pulled.get("task").get("completedAt")).isNotNull();
        assertThat(pulled.get("project").get("archivedAt")).isNotNull();
    }

    /** SYNC_PROTOCOL §6.1 (bổ sung Phase 2). */
    private static final Map<String, Set<String>> SECTION_6_1_PHASE_2 = Map.of(
            "habit", Set.of("name", "description", "color", "icon", "scheduleType", "weekdays", "timesPerWeek",
                    "intervalDays", "startOn", "endOn", "reminderTime", "archivedAt", "createdAt", "updatedAt"),
            "habit_checkin", Set.of("habitId", "localDate", "status", "note", "createdAt", "updatedAt"),
            "routine", Set.of("name", "kind", "weekdays", "reminderTime", "archivedAt", "createdAt", "updatedAt"),
            "routine_step", Set.of("routineId", "title", "position", "durationMinutes", "createdAt", "updatedAt"),
            "routine_log", Set.of("routineId", "localDate", "totalSteps", "startedAt", "completedAt", "createdAt",
                    "updatedAt"),
            "routine_step_log", Set.of("routineLogId", "stepId", "status", "createdAt", "updatedAt"));

    private Map<String, Object> withoutIdAndVersion(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isIn(200, 201);
        Map<String, Object> body = new LinkedHashMap<>(readJson(result));
        body.remove("id");
        body.remove("version");
        return body;
    }

    @Test
    void Sync_42_phase2EntityDataMatchesSection61AndTheRestResponse() throws Exception {
        TestUser user = newUser();
        java.time.LocalDate today = java.time.LocalDate.ofInstant(clock.instant(),
                java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        UUID habit = UUID.randomUUID();
        UUID routine = UUID.randomUUID();
        UUID step = UUID.randomUUID();
        UUID log = UUID.randomUUID();
        Map<String, Map<String, Object>> rest = new LinkedHashMap<>();
        rest.put("habit", withoutIdAndVersion(post(user, "/api/v1/habits", body("id", habit.toString(),
                "name", "Đọc", "scheduleType", "WEEKDAYS", "weekdays", 5, "startOn", today.minusDays(3).toString(),
                "reminderTime", "21:00", "color", "#00AA11"))));
        rest.put("habit_checkin", withoutIdAndVersion(call(user, org.springframework.http.HttpMethod.PUT,
                "/api/v1/habits/" + habit + "/checkins/" + today, body("status", "DONE", "note", "ok"))));
        rest.put("routine", withoutIdAndVersion(post(user, "/api/v1/routines", body("id", routine.toString(),
                "name", "Sáng", "kind", "DAILY", "reminderTime", "06:30"))));
        rest.put("routine_step", withoutIdAndVersion(post(user, "/api/v1/routines/" + routine + "/steps",
                body("id", step.toString(), "title", "Nước", "durationMinutes", 2))));
        rest.put("routine_step_log", null);
        MvcResult started = call(user, org.springframework.http.HttpMethod.PUT,
                "/api/v1/routines/" + routine + "/logs/" + today, body("id", log.toString(), "totalSteps", 2));
        rest.put("routine_step_log", withoutIdAndVersion(call(user, org.springframework.http.HttpMethod.PUT,
                "/api/v1/routine-logs/" + log + "/steps/" + step, body("status", "DONE"))));
        rest.put("routine_log", withoutIdAndVersion(started));

        Map<String, Map<String, Object>> pulled = pulledData(user);

        for (Map.Entry<String, Set<String>> e : SECTION_6_1_PHASE_2.entrySet()) {
            String type = e.getKey();
            assertThat(pulled.get(type).keySet()).as(type + ": data = danh sách §6.1")
                    .containsExactlyInAnyOrderElementsOf(e.getValue());
            assertThat(rest.get(type).keySet()).as(type + ": Response REST bỏ id/version = §6.1")
                    .containsExactlyInAnyOrderElementsOf(e.getValue());
            assertThat(pulled.get(type)).as(type + ": cùng giá trị với REST").isEqualTo(rest.get(type));
        }
        assertThat(pulled.get("habit")).containsEntry("weekdays", 5).containsEntry("reminderTime", "21:00");
    }

    private static final Map<String, Set<String>> SECTION_6_1_PHASE_3 = Map.of(
            "financial_account", Set.of("name", "accountType", "currency", "openingBalance", "archivedAt",
                    "createdAt", "updatedAt"),
            "financial_category", Set.of("name", "kind", "systemKey", "archivedAt", "createdAt", "updatedAt"),
            "financial_transaction", Set.of("accountId", "categoryId", "kind", "amount", "currency", "occurredOn",
                    "note", "recurringTransactionId", "recurrenceDate", "createdAt", "updatedAt"),
            "account_transfer", Set.of("fromAccountId", "toAccountId", "amount", "currency", "occurredOn", "note",
                    "createdAt", "updatedAt"),
            "budget", Set.of("categoryId", "currency", "amount", "archivedAt", "createdAt", "updatedAt"),
            "recurring_transaction", Set.of("accountId", "categoryId", "kind", "amount", "currency", "note",
                    "frequency", "intervalCount", "startOn", "endOn", "nextRunOn", "status", "createdAt", "updatedAt"),
            "saving_goal", Set.of("name", "currency", "targetAmount", "targetOn", "status", "createdAt", "updatedAt"),
            "saving_contribution", Set.of("goalId", "amount", "occurredOn", "note", "createdAt", "updatedAt"));

    @Test
    void Sync_42_phase3FinanceDataMatchesSection61AndTheRestResponse() throws Exception {
        TestUser user = newUser();
        String today = java.time.LocalDate.ofInstant(clock.instant(), java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .toString();
        String f = "/api/v1/finance";
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID cat = UUID.randomUUID();
        UUID goal = UUID.randomUUID();
        Map<String, Map<String, Object>> rest = new LinkedHashMap<>();
        withoutIdAndVersion(post(user, f + "/accounts", body("id", a.toString(), "name", "Ví", "accountType", "CASH",
                "currency", "VND")));
        rest.put("financial_account", withoutIdAndVersion(post(user, f + "/accounts", body("id", b.toString(),
                "name", "Thẻ", "accountType", "CREDIT_CARD", "currency", "VND", "openingBalance", "-1500000"))));
        rest.put("financial_category", withoutIdAndVersion(post(user, f + "/categories", body("id", cat.toString(),
                "name", "Cà phê", "kind", "EXPENSE"))));
        rest.put("financial_transaction", withoutIdAndVersion(post(user, f + "/transactions", body("id",
                UUID.randomUUID().toString(), "accountId", a.toString(), "categoryId", cat.toString(),
                "kind", "EXPENSE", "amount", "45000", "occurredOn", today, "note", "Sữa đá"))));
        rest.put("account_transfer", withoutIdAndVersion(post(user, f + "/transfers", body("id",
                UUID.randomUUID().toString(), "fromAccountId", a.toString(), "toAccountId", b.toString(),
                "amount", "500000", "occurredOn", today))));
        rest.put("budget", withoutIdAndVersion(post(user, f + "/budgets", body("id", UUID.randomUUID().toString(),
                "categoryId", cat.toString(), "currency", "VND", "amount", "900000"))));
        rest.put("recurring_transaction", withoutIdAndVersion(post(user, f + "/recurring", body("id",
                UUID.randomUUID().toString(), "accountId", a.toString(), "kind", "INCOME", "amount", "20000000",
                "frequency", "MONTHLY", "intervalCount", 1, "startOn", today, "endOn", "2030-12-31"))));
        rest.put("saving_goal", withoutIdAndVersion(post(user, f + "/saving-goals", body("id", goal.toString(),
                "name", "Laptop", "currency", "VND", "targetAmount", "30000000", "targetOn", "2027-06-30"))));
        rest.put("saving_contribution", withoutIdAndVersion(post(user, f + "/saving-goals/" + goal + "/contributions",
                body("id", UUID.randomUUID().toString(), "amount", "1000000", "occurredOn", today))));

        Map<String, Map<String, Object>> pulled = pulledData(user);
        for (Map.Entry<String, Set<String>> e : SECTION_6_1_PHASE_3.entrySet()) {
            String type = e.getKey();
            assertThat(pulled.get(type).keySet()).as(type + ": data = danh sách §6.1")
                    .containsExactlyInAnyOrderElementsOf(e.getValue());
            assertThat(rest.get(type).keySet()).as(type + ": Response REST bỏ id/version = §6.1")
                    .containsExactlyInAnyOrderElementsOf(e.getValue());
            assertThat(pulled.get(type)).as(type + ": cùng giá trị với REST").isEqualTo(rest.get(type));
        }
        assertThat(pulled.get("financial_account")).containsEntry("openingBalance", "-1500000");
        assertThat(pulled.get("financial_transaction")).containsEntry("amount", "45000");
    }

    private static final Map<String, Set<String>> SECTION_6_1_PHASE_4 = Map.of(
            "calendar_event", Set.of("title", "description", "location", "allDay", "timezone", "startLocal",
                    "endLocal", "startDate", "endDate", "startAt", "endAt", "recurrence", "recurrenceUntil",
                    "createdAt", "updatedAt"),
            "event_exception", Set.of("eventId", "originalStartLocal", "kind", "title", "description", "location",
                    "startLocal", "endLocal", "createdAt", "updatedAt"),
            "time_block", Set.of("taskId", "startAt", "endAt", "note", "createdAt", "updatedAt"),
            "reminder", Set.of("targetType", "targetId", "mode", "offsetMinutes", "localTime", "localDate",
                    "repeatRule", "urgent", "status", "nextFireAt", "lastFiredAt", "createdAt", "updatedAt"));

    @Test
    void Sync_42_phase4CalendarDataMatchesSection61AndTheRestResponse() throws Exception {
        TestUser user = newUser();
        UUID event = UUID.randomUUID();
        UUID task = UUID.randomUUID();
        Map<String, Map<String, Object>> rest = new LinkedHashMap<>();
        rest.put("calendar_event", withoutIdAndVersion(post(user, "/api/v1/calendar/events", body("id",
                event.toString(), "title", "Họp", "location", "Phòng 1", "timezone", "Asia/Tokyo", "startLocal",
                "2027-03-01T09:00:00", "endLocal", "2027-03-01T10:00:00", "recurrence",
                Map.of("freq", "WEEKLY", "byWeekday", List.of("MO", "WE"), "count", 4)))));
        rest.put("event_exception", withoutIdAndVersion(post(user, "/api/v1/calendar/events/" + event
                + "/exceptions", body("id", UUID.randomUUID().toString(), "originalStartLocal",
                "2027-03-03T09:00:00", "kind", "MODIFIED", "title", "Dời", "startLocal", "2027-03-03T13:00:00",
                "endLocal", "2027-03-03T14:00:00"))));
        withoutIdAndVersion(post(user, "/api/v1/tasks", body("id", task.toString(), "title", "Việc")));
        rest.put("time_block", withoutIdAndVersion(post(user, "/api/v1/time-blocks", body("id",
                UUID.randomUUID().toString(), "taskId", task.toString(), "startAt", "2027-03-01T01:00:00Z",
                "endAt", "2027-03-01T02:00:00Z", "note", "Tập trung"))));
        rest.put("reminder", withoutIdAndVersion(post(user, "/api/v1/reminders", body("id",
                UUID.randomUUID().toString(), "targetType", "NONE", "mode", "LOCAL_AT", "localTime", "07:00",
                "localDate", "2027-03-01", "repeatRule", Map.of("freq", "DAILY", "count", 3), "urgent", true))));
        Map<String, Map<String, Object>> pulled = pulledData(user);
        for (Map.Entry<String, Set<String>> e : SECTION_6_1_PHASE_4.entrySet()) {
            String type = e.getKey();
            assertThat(pulled.get(type).keySet()).as(type).containsExactlyInAnyOrderElementsOf(e.getValue());
            assertThat(rest.get(type).keySet()).as(type).containsExactlyInAnyOrderElementsOf(e.getValue());
            assertThat(pulled.get(type)).as(type + ": cùng giá trị với REST").isEqualTo(rest.get(type));
        }
        assertThat(pulled.get("calendar_event")).containsEntry("startAt", "2027-03-01T00:00:00Z");
        assertThat(pulled.get("reminder")).containsEntry("localTime", "07:00");
    }

    /** SYNC_PROTOCOL §6.1 (bổ sung Phase 6). */
    private static final Map<String, Set<String>> SECTION_6_1_PHASE_6 = Map.of(
            "note", Set.of("title", "content", "pinned", "archivedAt", "createdAt", "updatedAt"),
            "tag", Set.of("name", "color", "createdAt", "updatedAt"),
            "note_tag", Set.of("noteId", "tagId", "createdAt", "updatedAt"),
            "note_link", Set.of("noteId", "targetType", "targetId", "createdAt", "updatedAt"),
            "goal", Set.of("title", "description", "startOn", "targetOn", "status", "statusChangedAt", "createdAt",
                    "updatedAt"),
            "milestone", Set.of("goalId", "title", "dueOn", "position", "completedAt", "createdAt", "updatedAt"));

    @Test
    @SuppressWarnings("unchecked")
    void Sync_42_phase6KnowledgeGoalDataMatchesSection61AndTheRestResponse() throws Exception {
        TestUser user = newUser();
        UUID note = UUID.randomUUID();
        UUID tag = UUID.randomUUID();
        UUID goal = UUID.randomUUID();
        Map<String, Map<String, Object>> rest = new LinkedHashMap<>();
        rest.put("note", withoutIdAndVersion(post(user, "/api/v1/notes", body("id", note.toString(), "title",
                "Tiêu đề", "content", "Nội dung"))));
        rest.put("tag", withoutIdAndVersion(post(user, "/api/v1/tags", body("id", tag.toString(), "name", "Việc",
                "color", "#112233"))));
        rest.put("goal", withoutIdAndVersion(post(user, "/api/v1/goals", body("id", goal.toString(), "title", "G",
                "description", "mô tả", "startOn", "2027-01-01", "targetOn", "2027-06-30"))));
        rest.put("milestone", withoutIdAndVersion(post(user, "/api/v1/goals/" + goal + "/milestones", body("id",
                UUID.randomUUID().toString(), "title", "M", "dueOn", "2027-03-01"))));
        rest.put("note_link", withoutIdAndVersion(post(user, "/api/v1/notes/" + note + "/links", body("id",
                UUID.randomUUID().toString(), "targetType", "GOAL", "targetId", goal.toString()))));
        MvcResult tags = call(user, org.springframework.http.HttpMethod.PUT, "/api/v1/notes/" + note + "/tags",
                body("tagIds", List.of(tag.toString())));
        assertThat(tags.getResponse().getStatus()).isEqualTo(200);
        Map<String, Map<String, Object>> pulled = pulledData(user);
        Map<String, Object> noteTag = new LinkedHashMap<>(pulled.get("note_tag"));
        assertThat(noteTag).containsEntry("noteId", note.toString()).containsEntry("tagId", tag.toString());
        for (Map.Entry<String, Set<String>> e : SECTION_6_1_PHASE_6.entrySet()) {
            String type = e.getKey();
            assertThat(pulled.get(type).keySet()).as(type).containsExactlyInAnyOrderElementsOf(e.getValue());
            if (rest.containsKey(type)) {
                assertThat(rest.get(type).keySet()).as(type).containsExactlyInAnyOrderElementsOf(e.getValue());
                assertThat(pulled.get(type)).as(type + ": cùng giá trị với REST").isEqualTo(rest.get(type));
            }
        }
        assertThat(pulled.get("milestone")).containsEntry("position", 1000).containsEntry("dueOn", "2027-03-01");
    }

    @Test
    void Sync_42_phase7ReviewDataMatchesSection61AndTheRestResponse() throws Exception {
        TestUser user = newUser();
        String today = java.time.LocalDate.ofInstant(clock.instant(), java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                .toString();
        Map<String, Object> rest = withoutIdAndVersion(post(user, "/api/v1/reviews", body("id",
                UUID.randomUUID().toString(), "periodType", "DAY", "periodKey", today, "reflection", "Phản tư",
                "mood", 2)));
        Map<String, Object> pulled = pulledData(user).get("review");
        Set<String> keys = Set.of("periodType", "periodKey", "reflection", "mood", "createdAt", "updatedAt");
        assertThat(pulled.keySet()).containsExactlyInAnyOrderElementsOf(keys);
        assertThat(rest.keySet()).containsExactlyInAnyOrderElementsOf(keys);
        assertThat(pulled).isEqualTo(rest);
    }

    @Test
    void conflictSnapshotDataUsesTheSameShape() throws Exception {
        TestUser user = newUser();
        UUID category = createCategory(user);
        pushOne(user, mutation("category", category, "UPDATE", 1, payload("name", "Đổi tên")));

        Map<String, Object> conflict = pushOne(user, mutation("category", category, "UPDATE", 1,
                payload("name", "Cũ")));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) ((Map<String, Object>) conflict.get("serverSnapshot"))
                .get("data");
        assertThat(data).isEqualTo(restWithoutIdAndVersion(user, "/api/v1/categories/" + category));
    }
}
