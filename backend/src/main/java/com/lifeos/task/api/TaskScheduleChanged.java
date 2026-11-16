package com.lifeos.task.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Sự kiện miền: hạn (dueDate/dueTime) của Task đổi; notification tính lại reminder (cùng transaction, trước record). */
public record TaskScheduleChanged(UUID userId, UUID taskId, LocalDate dueDate, LocalTime dueTime) {
}
