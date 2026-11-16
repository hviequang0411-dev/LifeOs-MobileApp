package com.lifeos.calendar.service;

import com.lifeos.calendar.domain.EventRecurrence;
import com.lifeos.calendar.entity.CalendarEvent;
import com.lifeos.calendar.entity.EventException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mở rộng một sự kiện thành các lần xuất hiện trong một cửa sổ UTC (D125, D127, D134): tính theo giờ địa phương của sự
 * kiện, đổi sang UTC theo quy tắc DST của {@link CalendarValues#toInstant}; CANCELLED bị bỏ, MODIFIED thay trường/giờ.
 */
final class EventTimeline {

    /** Một lần xuất hiện đã áp ngoại lệ. */
    record Occurrence(CalendarEvent event, LocalDateTime originalStartLocal, String title, String location,
            Instant startAt, Instant endAt, LocalDate startDate, LocalDate endDate, boolean exception) {
    }

    private EventTimeline() {
    }

    static LocalDateTime seriesStart(CalendarEvent e) {
        return e.isAllDay() ? e.getStartDate().atStartOfDay() : e.getStartLocal();
    }

    static Duration localDuration(CalendarEvent e) {
        return e.isAllDay() ? Duration.ofDays(ChronoUnit.DAYS.between(e.getStartDate(), e.getEndDate()) + 1)
                : Duration.between(e.getStartLocal(), e.getEndLocal());
    }

    /**
     * @param limit số lần xuất hiện tối đa (đếm cả lần bị CANCELLED để chặn vòng lặp vô hạn)
     */
    static List<Occurrence> expand(CalendarEvent e, EventRecurrence rule, Collection<EventException> exceptions,
            Instant from, Instant to, int limit) {
        ZoneId zone = ZoneId.of(e.getTimezone());
        Duration duration = localDuration(e);
        Map<LocalDateTime, EventException> byOriginal = new HashMap<>();
        for (EventException x : exceptions) {
            byOriginal.put(x.getOriginalStartLocal(), x);
        }
        List<LocalDateTime> starts;
        if (rule == null) {
            starts = List.of(seriesStart(e));
        } else {
            // Cửa sổ địa phương nới thêm thời lượng (lần bắt đầu trước 'from' nhưng còn kéo dài) và 1 ngày cho lệch múi giờ.
            LocalDateTime localFrom = LocalDateTime.ofInstant(from, zone).minus(duration).minusDays(1);
            LocalDateTime localTo = LocalDateTime.ofInstant(to, zone).plusDays(1);
            starts = rule.between(seriesStart(e), localFrom, localTo, limit);
        }
        List<Occurrence> out = new ArrayList<>();
        for (LocalDateTime original : starts) {
            EventException x = byOriginal.get(original);
            if (x != null && "CANCELLED".equals(x.getKind())) {
                continue;
            }
            LocalDateTime startLocal = original;
            LocalDateTime endLocal = original.plus(duration);
            String title = e.getTitle();
            String location = e.getLocation();
            if (x != null) {
                if (x.getStartLocal() != null) {
                    startLocal = x.getStartLocal();
                    endLocal = x.getEndLocal() != null ? x.getEndLocal() : startLocal.plus(duration);
                }
                title = x.getTitle() != null ? x.getTitle() : title;
                location = x.getLocation() != null ? x.getLocation() : location;
            }
            Instant startAt = CalendarValues.toInstant(startLocal, zone);
            Instant endAt = CalendarValues.toInstant(endLocal, zone);
            if (!startAt.isBefore(to) || !endAt.isAfter(from)) {
                continue;
            }
            LocalDate startDate = e.isAllDay() ? startLocal.toLocalDate() : null;
            LocalDate endDate = e.isAllDay() ? endLocal.toLocalDate().minusDays(1) : null;
            out.add(new Occurrence(e, rule == null ? null : original, title, location, startAt, endAt, startDate,
                    endDate, x != null));
        }
        return out;
    }
}
