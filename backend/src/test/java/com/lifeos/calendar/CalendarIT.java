package com.lifeos.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.support.ApiTestBase;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Lịch: cửa sổ hợp nhất, ngoại lệ, tách/cắt chuỗi, múi giờ, cả ngày, time block (PHASE_4 CAL-02..06). */
class CalendarIT extends ApiTestBase {

    static final String EVENTS = "/api/v1/calendar/events";

    private MvcResult createEvent(TestUser user, UUID id, Object... fields) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id.toString());
        body.put("title", "Họp nhóm");
        body.put("timezone", "Asia/Ho_Chi_Minh");
        for (int i = 0; i < fields.length; i += 2) {
            body.put((String) fields[i], fields[i + 1]);
        }
        return post(user, EVENTS, json.writeValueAsString(body));
    }

    private UUID event(TestUser user, Object... fields) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult r = createEvent(user, id, fields);
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> window(TestUser user, LocalDate from, LocalDate to) throws Exception {
        MvcResult r = get(user, "/api/v1/calendar?from=" + from + "&to=" + to);
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
        return (List<Map<String, Object>>) readJson(r).get("items");
    }

    private String errorCode(MvcResult r) throws Exception {
        return (String) readJson(r).get("errorCode");
    }

    @Test
    void Cal_02_windowLongerThan366DaysOrOver1000OccurrencesIsRejected() throws Exception {
        TestUser user = newUser();
        LocalDate from = LocalDate.of(2027, 1, 1);
        MvcResult tooLong = get(user, "/api/v1/calendar?from=" + from + "&to=" + from.plusDays(366));
        assertThat(tooLong.getResponse().getStatus()).isEqualTo(400);
        assertThat(errorCode(tooLong)).isEqualTo("VALIDATION_FAILED");
        for (int i = 0; i < 3; i++) {
            event(user, "startLocal", "2027-01-01T0" + (7 + i) + ":00:00", "endLocal", "2027-01-01T0" + (7 + i)
                    + ":30:00", "recurrence", Map.of("freq", "DAILY"));
        }
        MvcResult tooMany = get(user, "/api/v1/calendar?from=" + from + "&to=" + from.plusDays(365));
        assertThat(tooMany.getResponse().getStatus()).isEqualTo(422);
        assertThat(errorCode(tooMany)).isEqualTo("LIMIT_EXCEEDED");
        assertThat(window(user, from, from.plusDays(9))).hasSize(30);
        MvcResult badRule = createEvent(user, UUID.randomUUID(), "startLocal", "2027-01-01T08:00:00", "endLocal",
                "2027-01-01T09:00:00", "recurrence", Map.of("freq", "DAILY", "count", 5000));
        assertThat(badRule.getResponse().getStatus()).isEqualTo(422);
        assertThat(errorCode(badRule)).isEqualTo("CALENDAR_INVALID_RECURRENCE");
    }

    @Test
    void Cal_03_exceptionsSplitTruncateAndCascadingSeriesDelete() throws Exception {
        TestUser user = newUser();
        UUID id = event(user, "startLocal", "2027-02-01T09:00:00", "endLocal", "2027-02-01T10:00:00",
                "recurrence", Map.of("freq", "DAILY", "count", 10));
        UUID cancelled = UUID.randomUUID();
        assertThat(post(user, EVENTS + "/" + id + "/exceptions", body("id", cancelled.toString(),
                "originalStartLocal", "2027-02-02T09:00:00", "kind", "CANCELLED")).getResponse().getStatus())
                .isEqualTo(201);
        UUID moved = UUID.randomUUID();
        assertThat(post(user, EVENTS + "/" + id + "/exceptions", body("id", moved.toString(),
                "originalStartLocal", "2027-02-03T09:00:00", "kind", "MODIFIED", "title", "Họp dời",
                "startLocal", "2027-02-03T15:00:00", "endLocal", "2027-02-03T16:00:00")).getResponse().getStatus())
                .isEqualTo(201);
        MvcResult notOccurrence = post(user, EVENTS + "/" + id + "/exceptions", body("id",
                UUID.randomUUID().toString(), "originalStartLocal", "2027-02-04T10:00:00", "kind", "CANCELLED"));
        assertThat(notOccurrence.getResponse().getStatus()).isEqualTo(422);
        assertThat(errorCode(notOccurrence)).isEqualTo("CALENDAR_OCCURRENCE_NOT_FOUND");
        MvcResult duplicate = post(user, EVENTS + "/" + id + "/exceptions", body("id", UUID.randomUUID().toString(),
                "originalStartLocal", "2027-02-02T09:00:00", "kind", "CANCELLED"));
        assertThat(duplicate.getResponse().getStatus()).isEqualTo(409);

        List<Map<String, Object>> items = window(user, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 3));
        assertThat(items).hasSize(2);
        assertThat(items.get(1)).containsEntry("title", "Họp dời").containsEntry("exception", true)
                .containsEntry("startAt", "2027-02-03T08:00:00Z");

        long version = version(readJson(get(user, EVENTS + "/" + id)));
        UUID second = UUID.randomUUID();
        MvcResult split = post(user, EVENTS + "/" + id + "/split", body("version", version, "originalStartLocal",
                "2027-02-06T09:00:00", "newEventId", second.toString(), "title", "Họp mới"));
        assertThat(split.getResponse().getStatus()).as(split.getResponse().getContentAsString()).isEqualTo(201);
        assertThat(((Map<?, ?>) readJson(split).get("recurrence")).get("count")).isEqualTo(5);
        assertThat(((Map<?, ?>) readJson(get(user, EVENTS + "/" + id)).get("recurrence")).get("count")).isEqualTo(5);
        List<Map<String, Object>> afterSplit = window(user, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 28));
        assertThat(afterSplit).hasSize(9);
        assertThat(afterSplit).filteredOn(i -> "Họp mới".equals(i.get("title"))).hasSize(5);

        long secondVersion = version(readJson(get(user, EVENTS + "/" + second)));
        assertThat(delete(user, EVENTS + "/" + second + "?version=" + secondVersion
                + "&fromOriginalStart=2027-02-08T09:00:00").getResponse().getStatus()).isEqualTo(204);
        assertThat(window(user, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 28))).hasSize(6);

        long v = version(readJson(get(user, EVENTS + "/" + id)));
        assertThat(delete(user, EVENTS + "/" + id + "?version=" + v).getResponse().getStatus()).isEqualTo(204);
        for (UUID x : List.of(cancelled, moved)) {
            assertThat(jdbc.sql("SELECT deleted_at IS NOT NULL FROM event_exceptions WHERE id = :id").param("id", x)
                    .query(Boolean.class).single()).isTrue();
            assertThat(jdbc.sql("SELECT count(*) FROM change_logs WHERE entity_id = :id AND operation = 'DELETE'"
                    + " AND mutation_id IS NULL").param("id", x).query(Integer.class).single()).isEqualTo(1);
            assertThat(changeLogs(user.id(), "event_exception", x, "DELETE")).isEqualTo(1);
        }
    }

    @Test
    void Cal_04_eventTimezoneIsTheSourceOfTruthAndDstUsesD134() throws Exception {
        TestUser user = newUser();
        jdbc.sql("INSERT INTO user_settings (id, user_id, timezone) VALUES (:id, :u, 'Asia/Ho_Chi_Minh')")
                .param("id", UUID.randomUUID()).param("u", user.id()).update();
        UUID tokyo = event(user, "timezone", "Asia/Tokyo", "startLocal", "2027-05-10T09:00:00", "endLocal",
                "2027-05-10T10:00:00");
        Map<String, Object> e = readJson(get(user, EVENTS + "/" + tokyo));
        assertThat(e).containsEntry("startAt", "2027-05-10T00:00:00Z").containsEntry("endAt", "2027-05-10T01:00:00Z");
        List<Map<String, Object>> items = window(user, LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 10));
        assertThat(items).extracting(i -> i.get("id")).containsExactly(tokyo.toString());
        UUID ny = event(user, "timezone", "America/New_York", "startLocal", "2027-03-13T02:30:00", "endLocal",
                "2027-03-13T03:00:00", "recurrence", Map.of("freq", "DAILY", "count", 3));
        List<Map<String, Object>> dst = window(user, LocalDate.of(2027, 3, 13), LocalDate.of(2027, 3, 16));
        assertThat(dst).filteredOn(i -> i.get("id").equals(ny.toString())).extracting(i -> i.get("startAt"))
                .containsExactly("2027-03-13T07:30:00Z", "2027-03-14T07:30:00Z", "2027-03-15T06:30:00Z");
        MvcResult badZone = createEvent(user, UUID.randomUUID(), "timezone", "GMT+7", "startLocal",
                "2027-01-01T08:00:00", "endLocal", "2027-01-01T09:00:00");
        assertThat(badZone.getResponse().getStatus()).isEqualTo(422);
        assertThat(errorCode(badZone)).isEqualTo("CALENDAR_INVALID_TIMEZONE");
    }

    @Test
    void Cal_05_allDayAndTimedShapesAreValidatedAndEnforcedByTheDatabase() throws Exception {
        TestUser user = newUser();
        UUID allDay = event(user, "allDay", true, "startDate", "2027-06-01", "endDate", "2027-06-03");
        Map<String, Object> e = readJson(get(user, EVENTS + "/" + allDay));
        assertThat(e).containsEntry("startAt", "2027-05-31T17:00:00Z").containsEntry("endAt", "2027-06-03T17:00:00Z")
                .containsEntry("startLocal", null);
        assertThat(createEvent(user, UUID.randomUUID(), "allDay", true, "startDate", "2027-06-01", "endDate",
                "2027-06-03", "startLocal", "2027-06-01T08:00:00").getResponse().getStatus()).isEqualTo(400);
        assertThat(createEvent(user, UUID.randomUUID(), "startLocal", "2027-06-01T10:00:00", "endLocal",
                "2027-06-01T09:00:00").getResponse().getStatus()).isEqualTo(400);
        assertThat(createEvent(user, UUID.randomUUID(), "allDay", true, "startDate", "2027-06-03", "endDate",
                "2027-06-01").getResponse().getStatus()).isEqualTo(400);
        long version = version(e);
        Map<String, Object> switched = readJson(patch(user, EVENTS + "/" + allDay, body("version", version,
                "allDay", false, "startLocal", "2027-06-01T08:00:00", "endLocal", "2027-06-01T09:00:00")));
        assertThat(switched).containsEntry("allDay", false).containsEntry("startDate", null);
        assertThatThrownBy(() -> jdbc.sql("UPDATE calendar_events SET start_date = DATE '2027-06-01' WHERE id = :id")
                .param("id", allDay).update()).hasMessageContaining("ck_calendar_events_shape");
    }

    @Test
    void Cal_06_timeBlocksAreAtMost24HoursAndCascadeWithTheTask() throws Exception {
        TestUser user = newUser();
        UUID task = UUID.randomUUID();
        assertThat(post(user, "/api/v1/tasks", body("id", task.toString(), "title", "Viết báo cáo")).getResponse()
                .getStatus()).isEqualTo(201);
        Instant start = Instant.parse("2027-01-10T01:00:00Z");
        UUID block = UUID.randomUUID();
        assertThat(post(user, "/api/v1/time-blocks", body("id", block.toString(), "taskId", task.toString(),
                "startAt", start.toString(), "endAt", start.plusSeconds(7200).toString())).getResponse().getStatus())
                .isEqualTo(201);
        UUID overlap = UUID.randomUUID();
        assertThat(post(user, "/api/v1/time-blocks", body("id", overlap.toString(), "taskId", task.toString(),
                "startAt", start.plusSeconds(3600).toString(), "endAt", start.plusSeconds(9000).toString()))
                .getResponse().getStatus()).as("trùng giờ không bị chặn").isEqualTo(201);
        MvcResult tooLong = post(user, "/api/v1/time-blocks", body("id", UUID.randomUUID().toString(), "taskId",
                task.toString(), "startAt", start.toString(), "endAt", start.plusSeconds(86401).toString()));
        assertThat(tooLong.getResponse().getStatus()).isEqualTo(400);
        TestUser other = newUser();
        MvcResult foreign = post(other, "/api/v1/time-blocks", body("id", UUID.randomUUID().toString(), "taskId",
                task.toString(), "startAt", start.toString(), "endAt", start.plusSeconds(60).toString()));
        assertThat(foreign.getResponse().getStatus()).isEqualTo(422);
        assertThat(errorCode(foreign)).isEqualTo("TASK_NOT_FOUND");
        List<Map<String, Object>> items = window(user, LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 10));
        assertThat(items).filteredOn(i -> "TIME_BLOCK".equals(i.get("source"))).hasSize(2);

        assertThat(delete(user, "/api/v1/tasks/" + task + "?version=1").getResponse().getStatus()).isEqualTo(204);
        for (UUID b : List.of(block, overlap)) {
            assertThat(jdbc.sql("SELECT deleted_at IS NOT NULL FROM time_blocks WHERE id = :id").param("id", b)
                    .query(Boolean.class).single()).isTrue();
        }
        List<Map<String, Object>> rows = jdbc.sql("SELECT entity_type, entity_id, mutation_id FROM change_logs"
                + " WHERE user_id = :u AND operation = 'DELETE' ORDER BY seq").param("u", user.id()).query()
                .listOfRows();
        assertThat(rows).extracting(r -> r.get("entity_type")).containsExactly("time_block", "time_block", "task");
        assertThat(rows.subList(0, 2)).allSatisfy(r -> assertThat(r.get("mutation_id")).isNull());
        assertThat(LocalDate.ofInstant(start, ZoneId.of("UTC"))).isEqualTo(LocalDate.of(2027, 1, 10));
    }
}
