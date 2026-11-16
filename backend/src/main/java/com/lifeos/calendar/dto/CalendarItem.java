package com.lifeos.calendar.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Một mục của cửa sổ lịch hợp nhất (D145). source: EVENT | TIME_BLOCK | TASK. originalStartLocal định danh lần xuất hiện của sự kiện lặp. Task chỉ có dueDate/dueTime (giờ địa phương người dùng). */
public record CalendarItem(String source, UUID id, UUID eventId, LocalDateTime originalStartLocal, String title,
        String location, boolean allDay, Instant startAt, Instant endAt, LocalDate startDate, LocalDate endDate,
        boolean exception, UUID taskId, LocalDate dueDate, String dueTime) {

    @Override
    public String toString() {
        return "CalendarItem[source=" + source + ", id=" + id + "]";
    }
}
