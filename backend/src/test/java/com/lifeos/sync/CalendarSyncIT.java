package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * CAL-07: sync cho calendar_event, event_exception, time_block, reminder (PHASE_4 §7): CONFLICT khi lệch version,
 * REJECTED cho tham chiếu sai/giá trị sai, khóa do server quản (startAt, endAt, nextFireAt, status DONE) bị bỏ qua,
 * DUPLICATE, tombstone, cascade (mutation_id NULL).
 */
class CalendarSyncIT extends SyncTestBase {

    private Map<String, Object> eventCreate(UUID id) {
        return mutation("calendar_event", id, "CREATE", 0, payload("title", "Họp", "timezone", "Asia/Ho_Chi_Minh",
                "startLocal", "2027-03-01T09:00:00", "endLocal", "2027-03-01T10:00:00",
                "recurrence", Map.of("freq", "DAILY", "count", 5),
                "startAt", "2000-01-01T00:00:00Z", "endAt", "2000-01-01T01:00:00Z", "recurrenceUntil", "2000-01-02T00:00:00Z"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void Cal_07_eventAndExceptionConflictRejectMassAssignmentAndCascade() throws Exception {
        TestUser user = newUser();
        UUID event = UUID.randomUUID();
        assertThat(pushOne(user, eventCreate(event))).containsEntry("status", "APPLIED");
        Map<String, Object> row = row("calendar_events", event);
        assertThat(row.get("start_at").toString()).doesNotStartWith("2000");
        Map<String, Object> rename = mutation("calendar_event", event, "UPDATE", 1, payload("title", "Họp tuần"));
        assertThat(pushOne(user, rename)).containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
        assertThat(pushOne(user, rename)).containsEntry("status", "DUPLICATE");
        Map<String, Object> stale = pushOne(user, mutation("calendar_event", event, "UPDATE", 1,
                payload("title", "Cũ")));
        assertThat(stale).containsEntry("status", "CONFLICT");
        assertThat(((Map<String, Object>) ((Map<String, Object>) stale.get("serverSnapshot")).get("data")))
                .containsEntry("title", "Họp tuần").containsKey("recurrence");
        assertThat(pushOne(user, mutation("calendar_event", event, "UPDATE", 2,
                payload("recurrence", Map.of("freq", "HOURLY"))))).containsEntry("errorCode",
                "CALENDAR_INVALID_RECURRENCE");
        assertThat(pushOne(user, mutation("calendar_event", event, "UPDATE", 2, payload("recurrence", "DAILY"))))
                .containsEntry("errorCode", "VALIDATION_FAILED");

        UUID exception = UUID.randomUUID();
        assertThat(pushOne(user, mutation("event_exception", exception, "CREATE", 0, payload("eventId",
                event.toString(), "originalStartLocal", "2027-03-02T09:00:00", "kind", "CANCELLED"))))
                .containsEntry("status", "APPLIED");
        Map<String, Object> sameOccurrence = pushOne(user, mutation("event_exception", UUID.randomUUID(), "CREATE", 0,
                payload("eventId", event.toString(), "originalStartLocal", "2027-03-02T09:00:00", "kind",
                        "MODIFIED", "title", "X")));
        assertThat(sameOccurrence).containsEntry("status", "CONFLICT").containsEntry("conflictReason", "ALREADY_EXISTS");
        assertThat(pushOne(user, mutation("event_exception", UUID.randomUUID(), "CREATE", 0, payload("eventId",
                UUID.randomUUID().toString(), "originalStartLocal", "2027-03-02T09:00:00", "kind", "CANCELLED"))))
                .containsEntry("status", "REJECTED").containsEntry("errorCode", "CALENDAR_EVENT_NOT_FOUND");
        assertThat(pushOne(user, mutation("event_exception", exception, "UPDATE", 1, payload("originalStartLocal",
                "2027-03-03T09:00:00")))).containsEntry("errorCode", "VALIDATION_FAILED");

        UUID reminder = UUID.randomUUID();
        assertThat(pushOne(user, mutation("reminder", reminder, "CREATE", 0, payload("targetType", "EVENT",
                "targetId", event.toString(), "mode", "OFFSET", "offsetMinutes", 15, "nextFireAt",
                "2000-01-01T00:00:00Z", "status", "DONE")))).containsEntry("status", "APPLIED");
        Map<String, Object> reminderRow = row("reminders", reminder);
        assertThat(reminderRow.get("status")).isEqualTo("ACTIVE");
        assertThat(String.valueOf(reminderRow.get("next_fire_at"))).doesNotStartWith("2000");

        Map<String, Object> delete = mutation("calendar_event", event, "DELETE", 2, null);
        assertThat(pushOne(user, delete)).containsEntry("status", "APPLIED");
        for (UUID child : List.of(exception, reminder)) {
            assertThat(changeLogRows(user.id(), child)).last().satisfies(l -> assertThat(l)
                    .containsEntry("operation", "DELETE").containsEntry("mutation_id", null));
        }
        assertThat(changeLogRows(user.id(), event)).last().satisfies(l -> assertThat(l)
                .containsEntry("mutation_id", mutationIdOf(delete)));
        assertThat(pushOne(user, mutation("calendar_event", event, "UPDATE", 3, payload("title", "Y"))))
                .containsEntry("status", "CONFLICT");
    }

    @Test
    void Cal_07_timeBlockAndReminderReferencesAreRejectedTheSameWay() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID foreignTask = UUID.randomUUID();
        assertThat(pushOne(other, mutation("task", foreignTask, "CREATE", 0, payload("title", "Của người khác"))))
                .containsEntry("status", "APPLIED");
        List<Map<String, Object>> results = push(user,
                mutation("time_block", UUID.randomUUID(), "CREATE", 0, payload("taskId", foreignTask.toString(),
                        "startAt", "2027-01-01T01:00:00Z", "endAt", "2027-01-01T02:00:00Z")),
                mutation("time_block", UUID.randomUUID(), "CREATE", 0, payload("taskId", UUID.randomUUID().toString(),
                        "startAt", "2027-01-01T01:00:00Z", "endAt", "2027-01-01T02:00:00Z")),
                mutation("reminder", UUID.randomUUID(), "CREATE", 0, payload("targetType", "TASK", "targetId",
                        foreignTask.toString(), "mode", "OFFSET", "offsetMinutes", 5)));
        assertThat(results.get(0)).containsEntry("errorCode", "TASK_NOT_FOUND");
        assertThat(results.get(1)).containsEntry("errorCode", "TASK_NOT_FOUND");
        assertThat(results.get(2)).containsEntry("errorCode", "REMINDER_TARGET_NOT_FOUND");

        UUID task = UUID.randomUUID();
        pushOne(user, mutation("task", task, "CREATE", 0, payload("title", "Của tôi")));
        UUID block = UUID.randomUUID();
        assertThat(pushOne(user, mutation("time_block", block, "CREATE", 0, payload("taskId", task.toString(),
                "startAt", "2027-01-01T01:00:00Z", "endAt", "2027-01-01T02:00:00Z")))).containsEntry("status",
                "APPLIED");
        assertThat(pushOne(user, mutation("time_block", block, "UPDATE", 1, payload("endAt",
                "2027-01-02T03:00:00Z")))).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(pushOne(user, mutation("time_block", block, "UPDATE", 0, payload("note", "x"))))
                .containsEntry("status", "CONFLICT");
        assertThat(pushOne(user, mutation("time_block", block, "DELETE", 1, null))).containsEntry("status",
                "APPLIED");
        assertThat(pushOne(user, mutation("time_block", block, "DELETE", 1, null))).as("tombstone DELETE")
                .containsEntry("status", "APPLIED");
    }
}
