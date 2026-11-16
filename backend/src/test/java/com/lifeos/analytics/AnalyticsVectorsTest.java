package com.lifeos.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.analytics.domain.FinanceMetrics;
import com.lifeos.analytics.domain.GoalRisk;
import com.lifeos.analytics.domain.Period;
import com.lifeos.analytics.domain.TaskMetrics;
import com.lifeos.task.api.TaskStatsApi.CompletedTask;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** INT-10 (phần Java, D203): vector dùng chung với Dart ({@code docs/api/fixtures/analytics-vectors.json}). */
class AnalyticsVectorsTest {

    @Test
    @SuppressWarnings("unchecked")
    void Int_10_sharedVectorsProduceTheExpectedResults() throws Exception {
        Map<String, Object> v = JsonMapper.builder().build().readValue(
                Files.readString(Path.of("..", "docs", "api", "fixtures", "analytics-vectors.json")), Map.class);

        for (Map<String, Object> p : (List<Map<String, Object>>) v.get("periods")) {
            LocalDate date = LocalDate.parse((String) p.get("date"));
            DayOfWeek weekStart = DayOfWeek.of(((Number) p.get("weekStartDay")).intValue());
            Period period = switch ((String) p.get("type")) {
                case "DAY" -> Period.day(date);
                case "WEEK" -> Period.week(date, weekStart);
                default -> Period.month(YearMonth.from(date));
            };
            assertThat(period.start()).as(p.toString()).isEqualTo(LocalDate.parse((String) p.get("start")));
            assertThat(period.end()).as(p.toString()).isEqualTo(LocalDate.parse((String) p.get("end")));
            if (p.get("timezone") != null) {
                ZoneId zone = ZoneId.of((String) p.get("timezone"));
                assertThat(period.startInstant(zone)).isEqualTo(Instant.parse((String) p.get("startAt")));
                assertThat(period.endInstant(zone)).isEqualTo(Instant.parse((String) p.get("endAt")));
            }
        }

        for (Map<String, Object> t : (List<Map<String, Object>>) v.get("tasks")) {
            List<CompletedTask> rows = ((List<Map<String, Object>>) t.get("rows")).stream().map(r -> new CompletedTask(
                    Instant.parse((String) r.get("createdAt")), Instant.parse((String) r.get("completedAt")),
                    r.get("dueDate") == null ? null : LocalDate.parse((String) r.get("dueDate")),
                    r.get("dueTime") == null ? null : LocalTime.parse((String) r.get("dueTime")), null, null)).toList();
            TaskMetrics.Result r = TaskMetrics.compute(rows, ZoneId.of((String) t.get("timezone")));
            Map<String, Object> e = (Map<String, Object>) t.get("expected");
            assertThat(r.completed()).isEqualTo(((Number) e.get("completed")).longValue());
            assertThat(r.completedWithDue()).isEqualTo(((Number) e.get("completedWithDue")).longValue());
            assertThat(r.completedOnTime()).isEqualTo(((Number) e.get("completedOnTime")).longValue());
            assertThat(r.onTimeRate()).isEqualTo(e.get("onTimeRate") == null ? null
                    : new BigDecimal((String) e.get("onTimeRate")));
            assertThat(r.medianCycleSeconds()).isEqualTo(e.get("medianCycleSeconds") == null ? null
                    : ((Number) e.get("medianCycleSeconds")).longValue());
        }

        for (Map<String, Object> f : (List<Map<String, Object>>) v.get("finance")) {
            if (f.containsKey("savingsRate")) {
                BigDecimal rate = FinanceMetrics.savingsRate(new BigDecimal((String) f.get("income")),
                        new BigDecimal((String) f.get("expense")));
                assertThat(rate).isEqualTo(f.get("savingsRate") == null ? null
                        : new BigDecimal((String) f.get("savingsRate")));
            } else {
                assertThat(FinanceMetrics.pace(new BigDecimal((String) f.get("spent")),
                        new BigDecimal((String) f.get("limit")), LocalDate.parse((String) f.get("today"))))
                        .isEqualTo(new BigDecimal((String) f.get("pace")));
            }
        }

        for (Map<String, Object> g : (List<Map<String, Object>>) v.get("goals")) {
            assertThat(GoalRisk.label((String) g.get("status"), LocalDate.parse((String) g.get("startOn")),
                    LocalDate.parse((String) g.get("targetOn")),
                    g.get("progress") == null ? null : new BigDecimal((String) g.get("progress")),
                    LocalDate.parse((String) g.get("today")), new BigDecimal("0.15"))).as(g.toString())
                    .isEqualTo(g.get("risk"));
        }
    }
}
