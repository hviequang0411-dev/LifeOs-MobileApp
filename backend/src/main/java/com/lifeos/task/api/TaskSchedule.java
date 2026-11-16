package com.lifeos.task.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Hạn của một Task cho module khác (reminder, time block). dueTime null = cả ngày. Không chứa tiêu đề/mô tả. */
public record TaskSchedule(UUID taskId, LocalDate dueDate, LocalTime dueTime) {
}
