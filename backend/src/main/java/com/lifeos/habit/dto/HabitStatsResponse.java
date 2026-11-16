package com.lifeos.habit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Thống kê một habit trên [from, to] (D234) kèm chuỗi (D233). completionRate null khi mẫu số 0. */
public record HabitStatsResponse(UUID habitId, LocalDate from, LocalDate to, int scheduled, int done, int skipped,
        int missed, BigDecimal completionRate, int currentStreak, int longestStreak) {
}
