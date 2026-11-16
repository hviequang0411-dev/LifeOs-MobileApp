package com.lifeos.task.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * API thống kê chỉ đọc của module task cho analytics (PHASE_7 D197). Trả dữ liệu thô tối thiểu; định nghĩa chỉ số
 * (D198) nằm ở module analytics để có một nguồn duy nhất (vector kiểm thử D203).
 */
public interface TaskStatsApi {

    /** Task còn sống đã hoàn thành (dueTime theo giờ địa phương của người dùng). */
    record CompletedTask(Instant createdAt, Instant completedAt, LocalDate dueDate, LocalTime dueTime, UUID projectId,
            UUID categoryId) {
    }

    /**
     * Task còn sống có completed_at trong [from, to). Nhiều hơn {@link #MAX_ROWS} dòng thì KHÔNG cắt bớt mà ném
     * {@code ValidationException(ANALYTICS_RANGE_TOO_LARGE)} (422) — người gọi thu hẹp kỳ (đợt sửa F7).
     */
    List<CompletedTask> completedBetween(UUID userId, Instant from, Instant to);

    /** Task chưa xong (OPEN) có hạn trước {@code today} (giờ địa phương). */
    long overdueOpen(UUID userId, LocalDate today);

    int MAX_ROWS = 200_000;
}
