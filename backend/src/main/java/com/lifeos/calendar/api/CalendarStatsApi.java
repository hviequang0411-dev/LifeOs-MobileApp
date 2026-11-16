package com.lifeos.calendar.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** API thống kê chỉ đọc của module calendar (PHASE_7 D197; FR-INT-07). Ngày theo múi giờ người dùng (D192). */
public interface CalendarStatsApi {

    /** Phút của sự kiện có giờ (đã mở rộng lặp, áp ngoại lệ) và Time Block giao với ngày; số sự kiện bắt đầu trong ngày. */
    record DayAllocation(LocalDate day, long eventMinutes, long blockMinutes, int events) {
    }

    record Upcoming(UUID eventId, String title, Instant startAt, Instant endAt, boolean allDay) {
        @Override
        public String toString() {
            return "Upcoming[eventId=" + eventId + "]";
        }
    }

    /** Mọi ngày trong [from, to] (≤ 366 ngày). */
    List<DayAllocation> allocation(UUID userId, LocalDate from, LocalDate to);

    /** Lần xuất hiện sự kiện bắt đầu từ {@code from} trong [from, to], tối đa {@code limit}. */
    List<Upcoming> upcoming(UUID userId, Instant from, LocalDate to, int limit);
}
