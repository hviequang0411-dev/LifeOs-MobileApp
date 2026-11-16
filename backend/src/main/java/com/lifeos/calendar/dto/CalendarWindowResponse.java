package com.lifeos.calendar.dto;

import java.time.LocalDate;
import java.util.List;

/** GET /calendar?from=&to= (D145): mục sắp theo thời điểm bắt đầu. */
public record CalendarWindowResponse(java.time.LocalDate from, java.time.LocalDate to, List<CalendarItem> items) {
}
