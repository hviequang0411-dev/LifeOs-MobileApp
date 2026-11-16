package com.lifeos.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.calendar.domain.EventRecurrence;
import com.lifeos.calendar.service.CalendarValues;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** CAL-01: vector mở rộng lặp dùng chung với Dart ({@code docs/api/fixtures/recurrence-vectors.json}). */
class EventRecurrenceVectorsTest {

    @Test
    @SuppressWarnings("unchecked")
    void Cal_01_sharedRecurrenceVectorsExpandIdentically() throws Exception {
        Map<String, Object> file = JsonMapper.builder().build().readValue(
                Files.readString(Path.of("..", "docs", "api", "fixtures", "recurrence-vectors.json")), Map.class);
        List<Map<String, Object>> cases = (List<Map<String, Object>>) file.get("cases");
        assertThat(cases).hasSizeGreaterThanOrEqualTo(10);
        for (Map<String, Object> c : cases) {
            String name = (String) c.get("name");
            EventRecurrence rule = EventRecurrence.parse((Map<String, Object>) c.get("rule"));
            LocalDateTime start = LocalDateTime.parse((String) c.get("start"));
            List<LocalDateTime> got = rule.between(start, LocalDateTime.parse((String) c.get("from")),
                    LocalDateTime.parse((String) c.get("to")), 1000);
            List<LocalDateTime> expected = ((List<String>) c.get("expected")).stream().map(LocalDateTime::parse)
                    .toList();
            assertThat(got).as(name).isEqualTo(expected);
            if (c.containsKey("expectedUtc")) {
                ZoneId zone = ZoneId.of((String) c.get("timezone"));
                assertThat(got.stream().map(l -> CalendarValues.toInstant(l, zone).toString()).toList()).as(name)
                        .isEqualTo(c.get("expectedUtc"));
            }
        }
    }

    @Test
    void Cal_01_invalidRulesAreRejectedWithTheOffendingField() {
        assertThatThrownBy(() -> EventRecurrence.parse(Map.of("freq", "HOURLY")))
                .isInstanceOf(EventRecurrence.Invalid.class).hasMessage("freq");
        assertThatThrownBy(() -> EventRecurrence.parse(Map.of("freq", "DAILY", "interval", 0)))
                .hasMessage("interval");
        assertThatThrownBy(() -> EventRecurrence.parse(Map.of("freq", "DAILY", "byWeekday", List.of("MO"))))
                .hasMessage("byWeekday");
        assertThatThrownBy(() -> EventRecurrence.parse(Map.of("freq", "MONTHLY", "byMonthDay", 32)))
                .hasMessage("byMonthDay");
        assertThatThrownBy(() -> EventRecurrence.parse(Map.of("freq", "DAILY", "count", 1001))).hasMessage("count");
        assertThatThrownBy(() -> EventRecurrence.parse(Map.of("freq", "DAILY", "count", 3, "until", "2027-01-01")))
                .hasMessage("count");
        assertThatThrownBy(() -> EventRecurrence.parse(Map.of("freq", "DAILY", "bySetPos", 1)))
                .hasMessage("bySetPos");
        EventRecurrence rule = EventRecurrence.parse(Map.of("freq", "WEEKLY", "byWeekday", List.of("FR", "MO")));
        assertThat(rule.toMap()).containsEntry("byWeekday", List.of("MO", "FR")).containsEntry("interval", 1);
    }
}
