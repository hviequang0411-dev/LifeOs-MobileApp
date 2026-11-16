package com.lifeos.task.api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Task có hạn cho cửa sổ lịch hợp nhất (D145). Tiêu đề là dữ liệu của chính người dùng (trả qua REST của họ). */
public record TaskDue(UUID taskId, String title, LocalDate dueDate, LocalTime dueTime, String status) {

    @Override
    public String toString() {
        return "TaskDue[taskId=" + taskId + "]";
    }
}
