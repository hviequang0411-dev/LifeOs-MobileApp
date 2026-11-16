package com.lifeos.task.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.task.api.TaskStatsApi;
import com.lifeos.task.repository.TaskRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt {@link TaskStatsApi}: JPQL (Hibernate đọc due_time nhất quán với cách đã ghi), chỉ đọc. */
@Service
class TaskStatsApiImpl implements TaskStatsApi {

    private final TaskRepository tasks;

    TaskStatsApiImpl(TaskRepository tasks) {
        this.tasks = tasks;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompletedTask> completedBetween(UUID userId, Instant from, Instant to) {
        // Đợt sửa F7: đọc MAX_ROWS + 1 để PHÁT HIỆN vượt trần thay vì cắt im lặng (kết quả sai mà không báo).
        List<Object[]> rows = tasks.completedBetween(userId, from, to, Limit.of(MAX_ROWS + 1));
        if (rows.size() > MAX_ROWS) {
            throw new ValidationException(ErrorCode.ANALYTICS_RANGE_TOO_LARGE,
                    ErrorCode.ANALYTICS_RANGE_TOO_LARGE.title());
        }
        return rows.stream()
                .map(r -> new CompletedTask((Instant) r[0], (Instant) r[1], (LocalDate) r[2], (LocalTime) r[3],
                        (UUID) r[4], (UUID) r[5]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long overdueOpen(UUID userId, LocalDate today) {
        return tasks.overdueOpen(userId, today);
    }
}
