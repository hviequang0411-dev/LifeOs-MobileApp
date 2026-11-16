package com.lifeos.analytics.domain;

import com.lifeos.task.api.TaskStatsApi.CompletedTask;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Định nghĩa chỉ số Task D198 (nguồn duy nhất, vector kiểm thử D203). Đầu vào: Task còn sống hoàn thành trong kỳ.
 * <ul>
 *   <li>onTimeRate = hoàn thành có completedAt ≤ hạn / hoàn thành có hạn; hạn = dueDate + dueTime theo giờ địa phương,
 *       không có dueTime ⇒ hết ngày dueDate (00:00 ngày sau). null khi không có Task có hạn.</li>
 *   <li>medianCycleSeconds = trung vị (completedAt − createdAt); số chẵn ⇒ trung bình hai giá trị giữa, làm tròn xuống
 *       [PROPOSED]; null khi mẫu &lt; 5.</li>
 * </ul>
 */
public final class TaskMetrics {

    public static final int MIN_SAMPLE = 5;

    public record Share(UUID id, long count) {
    }

    public record Result(long completed, long completedWithDue, long completedOnTime, BigDecimal onTimeRate,
            Long medianCycleSeconds, List<Share> byProject, List<Share> byCategory) {
    }

    private TaskMetrics() {
    }

    public static Result compute(List<CompletedTask> rows, ZoneId zone) {
        long withDue = 0;
        long onTime = 0;
        List<Long> cycles = new ArrayList<>(rows.size());
        for (CompletedTask t : rows) {
            cycles.add(Duration.between(t.createdAt(), t.completedAt()).getSeconds());
            if (t.dueDate() != null) {
                withDue++;
                if (!t.completedAt().isAfter(dueEnd(t, zone))) {
                    onTime++;
                }
            }
        }
        BigDecimal rate = withDue == 0 ? null
                : BigDecimal.valueOf(onTime).divide(BigDecimal.valueOf(withDue), 4, RoundingMode.HALF_UP);
        return new Result(rows.size(), withDue, onTime, rate, median(cycles), top(rows, CompletedTask::projectId),
                top(rows, CompletedTask::categoryId));
    }

    static Instant dueEnd(CompletedTask t, ZoneId zone) {
        if (t.dueTime() == null) {
            return t.dueDate().plusDays(1).atStartOfDay(zone).toInstant();
        }
        return ZonedDateTime.ofLocal(t.dueDate().atTime(t.dueTime()), zone, null).toInstant();
    }

    static Long median(List<Long> values) {
        if (values.size() < MIN_SAMPLE) {
            return null;
        }
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : Math.floorDiv(sorted.get(n / 2 - 1) + sorted.get(n / 2), 2);
    }

    /** Top 10 theo số Task (id null = không có Project/Category), hòa thì theo id. */
    static List<Share> top(List<CompletedTask> rows, Function<CompletedTask, UUID> key) {
        Map<UUID, Long> counts = new HashMap<>();
        for (CompletedTask t : rows) {
            counts.merge(key.apply(t), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(e -> Objects.toString(e.getKey(), "")))
                .limit(10).map(e -> new Share(e.getKey(), e.getValue())).toList();
    }
}
