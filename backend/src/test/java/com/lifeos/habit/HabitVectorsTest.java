package com.lifeos.habit;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.habit.domain.HabitSchedule;
import com.lifeos.habit.domain.HabitStatistics;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * HAB-05 (PHASE_2 D233, D234, D251): vector dùng chung Java–Dart {@code docs/api/fixtures/habit-vectors.json}. Phía
 * Dart chạy cùng tệp; hai bên phải cho cùng kết quả.
 */
class HabitVectorsTest {

    private static final Path VECTORS = Path.of("..", "docs", "api", "fixtures", "habit-vectors.json");

    private static Integer intOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asInt();
    }

    private static LocalDate dateOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : LocalDate.parse(node.asString());
    }

    @Test
    void Hab_05_everyVectorMatchesTheCalculator() throws Exception {
        JsonNode root = JsonMapper.builder().build().readTree(Files.readString(VECTORS));
        int checked = 0;
        for (JsonNode c : root.get("cases")) {
            JsonNode s = c.get("schedule");
            HabitSchedule schedule = new HabitSchedule(s.get("type").asString(), intOrNull(s.get("weekdays")),
                    intOrNull(s.get("timesPerWeek")), intOrNull(s.get("intervalDays")),
                    LocalDate.parse(s.get("startOn").asString()), dateOrNull(s.get("endOn")));
            Map<LocalDate, String> checkins = new HashMap<>();
            c.get("checkins").properties().forEach(e -> checkins.put(LocalDate.parse(e.getKey()),
                    e.getValue().asString()));
            HabitStatistics calc = new HabitStatistics(schedule, checkins, LocalDate.parse(c.get("today").asString()),
                    DayOfWeek.of(c.get("weekStart").asInt()));
            JsonNode expected = c.get("expected");
            String name = c.get("name").asString();

            HabitStatistics.Streaks streaks = calc.streaks();
            assertThat(streaks.current()).as(name + " currentStreak").isEqualTo(expected.get("currentStreak").asInt());
            assertThat(streaks.longest()).as(name + " longestStreak").isEqualTo(expected.get("longestStreak").asInt());

            JsonNode es = expected.get("stats");
            HabitStatistics.Stats stats = calc.stats(LocalDate.parse(es.get("from").asString()),
                    LocalDate.parse(es.get("to").asString()));
            assertThat(stats.scheduled()).as(name + " scheduled").isEqualTo(es.get("scheduled").asInt());
            assertThat(stats.done()).as(name + " done").isEqualTo(es.get("done").asInt());
            assertThat(stats.skipped()).as(name + " skipped").isEqualTo(es.get("skipped").asInt());
            assertThat(stats.missed()).as(name + " missed").isEqualTo(es.get("missed").asInt());
            JsonNode rate = es.get("completionRate");
            assertThat(stats.completionRate()).as(name + " completionRate")
                    .isEqualTo(rate.isNull() ? null : new BigDecimal(rate.asString()));
            checked++;
        }
        assertThat(checked).isGreaterThanOrEqualTo(8);
    }
}
