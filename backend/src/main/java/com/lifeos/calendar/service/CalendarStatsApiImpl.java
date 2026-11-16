package com.lifeos.calendar.service;

import com.lifeos.calendar.api.CalendarStatsApi;
import com.lifeos.calendar.dto.CalendarItem;
import com.lifeos.common.time.UserCalendar;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài đặt {@link CalendarStatsApi} dùng lại cửa sổ lịch hợp nhất (D145) theo từng đoạn ≤ 31 ngày (giới hạn 1000 lần
 * xuất hiện áp cho mỗi đoạn). Sự kiện cả ngày: đếm vào {@code events}, không tính phút [PROPOSED].
 */
@Service
class CalendarStatsApiImpl implements CalendarStatsApi {

    private static final int CHUNK_DAYS = 31;

    private final CalendarQueryService queries;
    private final UserCalendar calendar;

    CalendarStatsApiImpl(CalendarQueryService queries, UserCalendar calendar) {
        this.queries = queries;
        this.calendar = calendar;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DayAllocation> allocation(UUID userId, LocalDate from, LocalDate to) {
        ZoneId zone = calendar.zone(userId);
        Map<LocalDate, long[]> days = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.put(d, new long[3]);
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (LocalDate start = from; !start.isAfter(to); start = start.plusDays(CHUNK_DAYS)) {
            LocalDate end = start.plusDays(CHUNK_DAYS - 1).isAfter(to) ? to : start.plusDays(CHUNK_DAYS - 1);
            for (CalendarItem item : queries.window(userId, start.toString(), end.toString()).items()) {
                if ("TASK".equals(item.source()) || !seen.add(item.source() + item.id() + item.startAt())) {
                    continue;
                }
                boolean event = "EVENT".equals(item.source());
                if (event) {
                    LocalDate startDay = item.allDay() && item.startDate() != null ? item.startDate()
                            : LocalDate.ofInstant(item.startAt(), zone);
                    long[] slot = days.get(startDay);
                    if (slot != null) {
                        slot[2]++;
                    }
                }
                if (item.allDay()) {
                    continue;
                }
                spread(days, zone, item.startAt(), item.endAt(), event ? 0 : 1);
            }
        }
        List<DayAllocation> out = new ArrayList<>();
        days.forEach((day, v) -> out.add(new DayAllocation(day, v[0], v[1], (int) v[2])));
        return out;
    }

    /** Chia phút của [start, end) cho từng ngày địa phương giao nhau. */
    private static void spread(Map<LocalDate, long[]> days, ZoneId zone, Instant start, Instant end, int index) {
        LocalDate day = LocalDate.ofInstant(start, zone);
        while (start.isBefore(end)) {
            Instant dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant();
            Instant segmentEnd = end.isBefore(dayEnd) ? end : dayEnd;
            long[] slot = days.get(day);
            if (slot != null) {
                slot[index] += Duration.between(start, segmentEnd).toMinutes();
            }
            start = segmentEnd;
            day = day.plusDays(1);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Upcoming> upcoming(UUID userId, Instant from, LocalDate to, int limit) {
        LocalDate fromDay = LocalDate.ofInstant(from, calendar.zone(userId));
        return queries.window(userId, fromDay.toString(), to.toString()).items().stream()
                .filter(i -> "EVENT".equals(i.source()) && !i.endAt().isBefore(from))
                .limit(limit)
                .map(i -> new Upcoming(i.eventId(), i.title(), i.startAt(), i.endAt(), i.allDay()))
                .toList();
    }
}
