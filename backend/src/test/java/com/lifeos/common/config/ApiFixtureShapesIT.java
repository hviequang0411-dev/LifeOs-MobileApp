package com.lifeos.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

/**
 * Hợp đồng backend ↔ Flutter bằng fixture JSON dùng chung cho entity của WP4–WP7 (D68, BACKEND_MASTER_PLAN §7): phản
 * hồi thật phải cùng hình dạng (tập trường, kiểu JSON từng trường) với {@code docs/api/fixtures/*.response.json}.
 * Chạy với {@code -Dfixtures.export=true} để ghi lại fixture từ phản hồi thật (sau khi hợp đồng đổi có chủ đích).
 */
class ApiFixtureShapesIT extends ApiTestBase {

    private static final Path FIXTURES = Path.of("..", "docs", "api", "fixtures");
    private static final boolean EXPORT = Boolean.getBoolean("fixtures.export");

    private static Object shape(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> fields = new TreeMap<>();
            node.properties().forEach(e -> fields.put(e.getKey(), shape(e.getValue())));
            return fields;
        }
        if (node.isArray()) {
            List<Object> items = new ArrayList<>();
            node.forEach(item -> items.add(shape(item)));
            return items;
        }
        return node.getNodeType().name();
    }

    private String ok(MvcResult r) throws Exception {
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isIn(200, 201);
        return r.getResponse().getContentAsString();
    }

    @Test
    void responsesOfNewEntitiesMatchTheSharedFixtures() throws Exception {
        TestUser user = newUser();
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
        UUID goal = UUID.randomUUID();
        UUID milestone = UUID.randomUUID();
        UUID note = UUID.randomUUID();
        UUID event = UUID.randomUUID();
        Map<String, String> responses = new LinkedHashMap<>();
        responses.put("goal.response.json", ok(post(user, "/api/v1/goals", body("id", goal.toString(), "title",
                "Chạy 10 km", "description", "Mô tả", "startOn", "2027-01-01", "targetOn", "2027-06-30"))));
        ok(post(user, "/api/v1/goals/" + goal + "/milestones", body("id", milestone.toString(), "title", "5 km",
                "dueOn", "2027-03-01")));
        responses.put("task.response.json", ok(post(user, "/api/v1/tasks", body("id", UUID.randomUUID().toString(),
                "title", "Chạy thử", "description", "d", "priority", "HIGH", "dueDate", "2027-02-01", "dueTime", "06:30",
                "milestoneId", milestone.toString()))));
        responses.put("goal-detail.response.json", ok(get(user, "/api/v1/goals/" + goal)));
        responses.put("note.response.json", ok(post(user, "/api/v1/notes", body("id", note.toString(), "title",
                "Hóa đơn", "content", "Tháng 11"))));
        responses.put("tag.response.json", ok(post(user, "/api/v1/tags", body("id", UUID.randomUUID().toString(),
                "name", "Việc", "color", "#112233"))));
        responses.put("review.response.json", ok(post(user, "/api/v1/reviews", body("id", UUID.randomUUID()
                .toString(), "periodType", "DAY", "periodKey", today.toString(), "reflection", "Ổn", "mood", 4))));
        responses.put("calendar-event.response.json", ok(post(user, "/api/v1/calendar/events", body("id",
                event.toString(), "title", "Họp", "location", "P1", "timezone", "Asia/Ho_Chi_Minh", "startLocal",
                "2027-03-01T09:00:00", "endLocal", "2027-03-01T10:00:00", "recurrence", Map.of("freq", "WEEKLY",
                        "byWeekday", List.of("MO"), "count", 4)))));
        responses.put("reminder.response.json", ok(post(user, "/api/v1/reminders", body("id", UUID.randomUUID()
                .toString(), "targetType", "EVENT", "targetId", event.toString(), "mode", "OFFSET", "offsetMinutes",
                10))));

        for (Map.Entry<String, String> e : responses.entrySet()) {
            Path file = FIXTURES.resolve(e.getKey());
            JsonNode actual = json.readTree(e.getValue());
            if (EXPORT) {
                Files.writeString(file, json.writerWithDefaultPrettyPrinter().writeValueAsString(actual) + "\n",
                        StandardCharsets.UTF_8);
                continue;
            }
            assertThat(Files.exists(file)).as("thiếu fixture " + e.getKey()).isTrue();
            JsonNode expected = json.readTree(Files.readString(file));
            assertThat(shape(actual)).as(e.getKey()).isEqualTo(shape(expected));
        }
    }
}
