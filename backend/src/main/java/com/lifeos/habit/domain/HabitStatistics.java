package com.lifeos.habit.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

/**
 * Chuỗi và thống kê habit, TÍNH từ dữ liệu gốc, không lưu (Phase 2 D233, D234). Thuần (không I/O); là định nghĩa duy
 * nhất dùng lại ở Phase 7 (D201) và được kiểm bằng vector Java–Dart (habit-vectors.json) cùng mô hình tham chiếu.
 * <p>
 * Quy ước chung: chỉ check-in chưa xóa, ngày trong [startOn, endOn], ngày ≤ hôm nay theo múi giờ người dùng. Check-in
 * ngày không đến lịch (D232) không cộng, không đứt chuỗi và không vào thống kê. Chi tiết TIMES_PER_WEEK cho tuần đầu/cuối
 * bị cắt bởi startOn/endOn và tuần hiện tại: ghi trong BACKEND_PROGRESS.md (quyết định phát sinh WP2).
 */
public final class HabitStatistics {

    public static final String DONE = "DONE";
    public static final String SKIPPED = "SKIPPED";

    /** Kết quả thống kê một khoảng ngày (D234). completionRate null khi mẫu số = 0; làm tròn 4 chữ số. */
    public record Stats(int scheduled, int done, int skipped, int missed, BigDecimal completionRate) {
    }

    /** Chuỗi hiện tại và dài nhất. */
    public record Streaks(int current, int longest) {
    }

    private enum WeekState { ACHIEVED, NEUTRAL, BROKEN }

    private final HabitSchedule schedule;
    private final Map<LocalDate, String> statusByDate;
    private final LocalDate today;
    private final DayOfWeek weekStart;

    /**
     * @param statusByDate trạng thái (DONE/SKIPPED) của các check-in CHƯA xóa theo ngày
     * @param today        hôm nay theo múi giờ hiện hành của người dùng (D244)
     * @param weekStart    ngày đầu tuần (user_settings.week_start_day)
     */
    public HabitStatistics(HabitSchedule schedule, Map<LocalDate, String> statusByDate, LocalDate today,
            DayOfWeek weekStart) {
        this.schedule = schedule;
        this.statusByDate = statusByDate;
        this.today = today;
        this.weekStart = weekStart;
    }

    /** Ngày cuối cùng được xét: min(hôm nay, endOn). */
    private LocalDate lastDay() {
        LocalDate end = schedule.endOn();
        return end != null && end.isBefore(today) ? end : today;
    }

    public Streaks streaks() {
        return schedule.isWeekly() ? weeklyStreaks() : dailyStreaks();
    }

    // ---------- DAILY, WEEKDAYS, EVERY_N_DAYS

    private Streaks dailyStreaks() {
        LocalDate last = lastDay();
        if (last.isBefore(schedule.startOn())) {
            return new Streaks(0, 0);
        }
        int current = 0;
        for (LocalDate day = last; !day.isBefore(schedule.startOn()); day = day.minusDays(1)) {
            if (!schedule.isScheduled(day)) {
                continue;
            }
            String status = statusByDate.get(day);
            if (status == null) {
                if (day.equals(today)) {
                    continue; // hôm nay chưa xong không làm đứt
                }
                break;
            }
            if (DONE.equals(status)) {
                current++;
            }
        }
        int run = 0;
        int longest = 0;
        for (LocalDate day = schedule.startOn(); !day.isAfter(last); day = day.plusDays(1)) {
            if (!schedule.isScheduled(day)) {
                continue;
            }
            String status = statusByDate.get(day);
            if (DONE.equals(status)) {
                run++;
                longest = Math.max(longest, run);
            } else if (status == null && !day.equals(today)) {
                run = 0;
            }
        }
        return new Streaks(current, longest);
    }

    // ---------- TIMES_PER_WEEK

    private LocalDate weekOf(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(weekStart));
    }

    private WeekState weekState(LocalDate weekFirst) {
        int available = 0;
        int done = 0;
        int skipped = 0;
        int remaining = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekFirst.plusDays(i);
            if (!schedule.inRange(day)) {
                continue;
            }
            available++;
            if (day.isAfter(today)) {
                remaining++;
                continue;
            }
            String status = statusByDate.get(day);
            if (DONE.equals(status)) {
                done++;
            } else if (SKIPPED.equals(status)) {
                skipped++;
            } else if (day.equals(today)) {
                remaining++; // hôm nay chưa xong vẫn còn cơ hội
            }
        }
        int required = Math.max(Math.min(schedule.timesPerWeek(), available) - skipped, 0);
        if (required == 0) {
            return WeekState.NEUTRAL;
        }
        if (done >= required) {
            return WeekState.ACHIEVED;
        }
        boolean current = !today.isBefore(weekFirst) && !today.isAfter(weekFirst.plusDays(6));
        return current && done + remaining >= required ? WeekState.NEUTRAL : WeekState.BROKEN;
    }

    private Streaks weeklyStreaks() {
        LocalDate last = lastDay();
        if (last.isBefore(schedule.startOn())) {
            return new Streaks(0, 0);
        }
        LocalDate firstWeek = weekOf(schedule.startOn());
        LocalDate lastWeek = weekOf(last);
        int current = 0;
        for (LocalDate week = lastWeek; !week.isBefore(firstWeek); week = week.minusWeeks(1)) {
            WeekState state = weekState(week);
            if (state == WeekState.BROKEN) {
                break;
            }
            if (state == WeekState.ACHIEVED) {
                current++;
            }
        }
        int run = 0;
        int longest = 0;
        for (LocalDate week = firstWeek; !week.isAfter(lastWeek); week = week.plusWeeks(1)) {
            WeekState state = weekState(week);
            if (state == WeekState.ACHIEVED) {
                run++;
                longest = Math.max(longest, run);
            } else if (state == WeekState.BROKEN) {
                run = 0;
            }
        }
        return new Streaks(current, longest);
    }

    // ---------- Thống kê (D234)

    public Stats stats(LocalDate from, LocalDate to) {
        LocalDate lo = from.isBefore(schedule.startOn()) ? schedule.startOn() : from;
        LocalDate hi = to.isAfter(lastDay()) ? lastDay() : to;
        int scheduled = 0;
        int done = 0;
        int skipped = 0;
        int missed = 0;
        if (!hi.isBefore(lo)) {
            if (schedule.isWeekly()) {
                for (LocalDate week = weekOf(lo); !week.isAfter(hi); week = week.plusWeeks(1)) {
                    int days = 0;
                    int weekDone = 0;
                    int weekSkipped = 0;
                    for (int i = 0; i < 7; i++) {
                        LocalDate day = week.plusDays(i);
                        if (day.isBefore(lo) || day.isAfter(hi)) {
                            continue;
                        }
                        days++;
                        String status = statusByDate.get(day);
                        if (DONE.equals(status)) {
                            weekDone++;
                        } else if (SKIPPED.equals(status)) {
                            weekSkipped++;
                        }
                    }
                    int weekScheduled = Math.min(schedule.timesPerWeek(), days);
                    int countedDone = Math.min(weekDone, weekScheduled);
                    int countedSkipped = Math.min(weekSkipped, weekScheduled - countedDone);
                    boolean current = !today.isBefore(week) && !today.isAfter(week.plusDays(6));
                    if (current) {
                        weekScheduled = countedDone + countedSkipped; // tuần đang chạy: chưa tính bỏ lỡ
                    }
                    scheduled += weekScheduled;
                    done += countedDone;
                    skipped += countedSkipped;
                    missed += weekScheduled - countedDone - countedSkipped;
                }
            } else {
                for (LocalDate day = lo; !day.isAfter(hi); day = day.plusDays(1)) {
                    if (!schedule.isScheduled(day)) {
                        continue;
                    }
                    String status = statusByDate.get(day);
                    if (status == null && day.equals(today)) {
                        continue; // hôm nay chưa xong: không tính
                    }
                    scheduled++;
                    if (DONE.equals(status)) {
                        done++;
                    } else if (SKIPPED.equals(status)) {
                        skipped++;
                    } else {
                        missed++;
                    }
                }
            }
        }
        int denominator = scheduled - skipped;
        BigDecimal rate = denominator == 0 ? null
                : BigDecimal.valueOf(done).divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
        return new Stats(scheduled, done, skipped, missed, rate);
    }
}
