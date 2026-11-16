package com.lifeos.habit.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Lịch habit (Phase 2 D230): DAILY, WEEKDAYS (bitmask 7 bit, Thứ Hai = bit 0 … Chủ Nhật = bit 6), TIMES_PER_WEEK (n/tuần,
 * không có ngày cố định), EVERY_N_DAYS (các ngày startOn + k·n). Thuần (không I/O) để kiểm bằng vector dùng chung
 * Java–Dart (docs/api/fixtures/habit-vectors.json).
 */
public record HabitSchedule(String type, Integer weekdays, Integer timesPerWeek, Integer intervalDays,
        LocalDate startOn, LocalDate endOn) {

    public static final String DAILY = "DAILY";
    public static final String WEEKDAYS = "WEEKDAYS";
    public static final String TIMES_PER_WEEK = "TIMES_PER_WEEK";
    public static final String EVERY_N_DAYS = "EVERY_N_DAYS";

    public HabitSchedule {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(startOn, "startOn");
    }

    public boolean isWeekly() {
        return TIMES_PER_WEEK.equals(type);
    }

    /** Ngày nằm trong vòng đời habit: ≥ startOn và ≤ endOn (nếu có). */
    public boolean inRange(LocalDate day) {
        return !day.isBefore(startOn) && (endOn == null || !day.isAfter(endOn));
    }

    /** Ngày "đến lịch" (D230). TIMES_PER_WEEK không có ngày cố định nên luôn false ở đây. */
    public boolean isScheduled(LocalDate day) {
        if (!inRange(day)) {
            return false;
        }
        return switch (type) {
            case DAILY -> true;
            case WEEKDAYS -> (weekdays & bit(day.getDayOfWeek())) != 0;
            case EVERY_N_DAYS -> ChronoUnit.DAYS.between(startOn, day) % intervalDays == 0;
            default -> false;
        };
    }

    /** Bit của một thứ trong bitmask weekdays (Thứ Hai = 1, …, Chủ Nhật = 64). */
    public static int bit(DayOfWeek day) {
        return 1 << (day.getValue() - 1);
    }
}
