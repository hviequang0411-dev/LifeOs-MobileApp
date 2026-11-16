package com.lifeos.habit.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** API thống kê chỉ đọc của module habit (PHASE_7 D197, D201): định nghĩa chuỗi/tỷ lệ của Phase 2 là nguồn duy nhất. */
public interface HabitStatsApi {

    /** Thống kê một habit chưa lưu trữ trên [from, to] (D234) và chuỗi (D233). Tên là dữ liệu của chính người dùng. */
    record HabitPeriod(UUID habitId, String name, int scheduled, int done, int skipped, int missed,
            BigDecimal completionRate, int currentStreak, int longestStreak) {
        @Override
        public String toString() {
            return "HabitPeriod[habitId=" + habitId + "]";
        }
    }

    /** Mọi habit chưa lưu trữ; khoảng ≤ 366 ngày. */
    List<HabitPeriod> stats(UUID userId, LocalDate from, LocalDate to);
}
