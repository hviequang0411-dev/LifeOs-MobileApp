package com.lifeos.habit;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.habit.domain.HabitSchedule;
import com.lifeos.habit.domain.HabitStatistics;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;

/**
 * HAB-06 (PHASE_2 §9): mô hình tham chiếu viết ĐỘC LẬP (đưa mọi lịch về một dãy "đơn vị" — ngày đến lịch hoặc tuần —
 * mỗi đơn vị một trạng thái ĐẠT / TRUNG TÍNH / ĐỨT, rồi đếm), so với HabitStatistics sau mỗi thao tác trong 500 thao tác
 * ngẫu nhiên (check-in DONE/SKIPPED, hoàn tác, đổi "hôm nay"). Seed cố định theo lần lặp để tái lập được.
 */
class HabitStatisticsPropertyTest {

    private enum Unit { ACHIEVED, NEUTRAL, BROKEN }

    @RepeatedTest(10)
    void Hab_06_referenceModelAgreesOver500RandomOperations(RepetitionInfo info) {
        Random random = new Random(20261107L + info.getCurrentRepetition());
        LocalDate startOn = LocalDate.of(2026, 1, 1).plusDays(random.nextInt(60));
        HabitSchedule schedule = randomSchedule(random, startOn);
        DayOfWeek weekStart = random.nextBoolean() ? DayOfWeek.MONDAY : DayOfWeek.SUNDAY;
        Map<LocalDate, String> checkins = new HashMap<>();
        LocalDate today = startOn.plusDays(random.nextInt(20));
        for (int op = 0; op < 500; op++) {
            LocalDate day = startOn.plusDays(random.nextInt(Math.max(1, (int) ChronoUnit.DAYS.between(startOn, today))
                    + 2));
            switch (random.nextInt(5)) {
                case 0, 1 -> checkins.put(day, "DONE");
                case 2 -> checkins.put(day, "SKIPPED");
                case 3 -> checkins.remove(day);
                default -> today = today.plusDays(random.nextInt(3));
            }
            HabitStatistics calc = new HabitStatistics(schedule, checkins, today, weekStart);
            HabitStatistics.Streaks streaks = calc.streaks();
            List<Unit> units = units(schedule, checkins, today, weekStart);
            String context = "lần " + info.getCurrentRepetition() + ", thao tác " + op + ", lịch " + schedule.type();
            assertThat(streaks.current()).as(context + " current").isEqualTo(referenceCurrent(units));
            assertThat(streaks.longest()).as(context + " longest").isEqualTo(referenceLongest(units));
            LocalDate from = startOn.plusDays(random.nextInt(10) - 5);
            LocalDate to = today.plusDays(random.nextInt(5));
            assertThat(calc.stats(from, to)).as(context + " stats")
                    .isEqualTo(referenceStats(schedule, checkins, today, weekStart, from, to));
        }
    }

    private static HabitSchedule randomSchedule(Random random, LocalDate startOn) {
        LocalDate endOn = random.nextInt(4) == 0 ? startOn.plusDays(10 + random.nextInt(40)) : null;
        return switch (random.nextInt(4)) {
            case 0 -> new HabitSchedule("DAILY", null, null, null, startOn, endOn);
            case 1 -> new HabitSchedule("WEEKDAYS", 1 + random.nextInt(127), null, null, startOn, endOn);
            case 2 -> new HabitSchedule("TIMES_PER_WEEK", null, 1 + random.nextInt(7), null, startOn, endOn);
            default -> new HabitSchedule("EVERY_N_DAYS", null, null, 2 + random.nextInt(29), startOn, endOn);
        };
    }

    // ---------- Mô hình tham chiếu

    private static boolean scheduledDay(HabitSchedule s, LocalDate day) {
        if (day.isBefore(s.startOn()) || (s.endOn() != null && day.isAfter(s.endOn()))) {
            return false;
        }
        return switch (s.type()) {
            case "DAILY" -> true;
            case "WEEKDAYS" -> ((s.weekdays() >> (day.getDayOfWeek().getValue() - 1)) & 1) == 1;
            case "EVERY_N_DAYS" -> Math.floorMod(ChronoUnit.DAYS.between(s.startOn(), day), s.intervalDays()) == 0;
            default -> false;
        };
    }

    private static LocalDate last(HabitSchedule s, LocalDate today) {
        return s.endOn() != null && s.endOn().isBefore(today) ? s.endOn() : today;
    }

    private static List<Unit> units(HabitSchedule s, Map<LocalDate, String> checkins, LocalDate today,
            DayOfWeek weekStart) {
        List<Unit> units = new ArrayList<>();
        LocalDate end = last(s, today);
        if (!s.type().equals("TIMES_PER_WEEK")) {
            for (LocalDate d = s.startOn(); !d.isAfter(end); d = d.plusDays(1)) {
                if (!scheduledDay(s, d)) {
                    continue;
                }
                String st = checkins.get(d);
                units.add("DONE".equals(st) ? Unit.ACHIEVED
                        : "SKIPPED".equals(st) || d.equals(today) ? Unit.NEUTRAL : Unit.BROKEN);
            }
            return units;
        }
        LocalDate week = s.startOn();
        while (week.getDayOfWeek() != weekStart) {
            week = week.minusDays(1);
        }
        for (; !week.isAfter(end); week = week.plusDays(7)) {
            List<LocalDate> days = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                LocalDate d = week.plusDays(i);
                if (!d.isBefore(s.startOn()) && (s.endOn() == null || !d.isAfter(s.endOn()))) {
                    days.add(d);
                }
            }
            long done = days.stream().filter(d -> !d.isAfter(today) && "DONE".equals(checkins.get(d))).count();
            long skipped = days.stream().filter(d -> !d.isAfter(today) && "SKIPPED".equals(checkins.get(d))).count();
            long open = days.stream().filter(d -> d.isAfter(today) || (d.equals(today) && checkins.get(d) == null))
                    .count();
            long required = Math.max(Math.min(s.timesPerWeek(), days.size()) - skipped, 0);
            boolean ongoing = !today.isBefore(week) && today.isBefore(week.plusDays(7));
            if (required == 0) {
                units.add(Unit.NEUTRAL);
            } else if (done >= required) {
                units.add(Unit.ACHIEVED);
            } else {
                units.add(ongoing && done + open >= required ? Unit.NEUTRAL : Unit.BROKEN);
            }
        }
        return units;
    }

    private static int referenceCurrent(List<Unit> units) {
        int count = 0;
        for (int i = units.size() - 1; i >= 0 && units.get(i) != Unit.BROKEN; i--) {
            if (units.get(i) == Unit.ACHIEVED) {
                count++;
            }
        }
        return count;
    }

    private static int referenceLongest(List<Unit> units) {
        int best = 0;
        int run = 0;
        for (Unit u : units) {
            run = u == Unit.BROKEN ? 0 : u == Unit.ACHIEVED ? run + 1 : run;
            best = Math.max(best, run);
        }
        return best;
    }

    private static HabitStatistics.Stats referenceStats(HabitSchedule s, Map<LocalDate, String> checkins,
            LocalDate today, DayOfWeek weekStart, LocalDate from, LocalDate to) {
        LocalDate lo = from.isBefore(s.startOn()) ? s.startOn() : from;
        LocalDate end = last(s, today);
        LocalDate hi = to.isAfter(end) ? end : to;
        int scheduled = 0;
        int done = 0;
        int skipped = 0;
        if (!hi.isBefore(lo)) {
            if (s.type().equals("TIMES_PER_WEEK")) {
                Map<LocalDate, int[]> weeks = new java.util.TreeMap<>();
                for (LocalDate d = lo; !d.isAfter(hi); d = d.plusDays(1)) {
                    LocalDate w = d;
                    while (w.getDayOfWeek() != weekStart) {
                        w = w.minusDays(1);
                    }
                    int[] c = weeks.computeIfAbsent(w, k -> new int[3]);
                    c[0]++;
                    c[1] += "DONE".equals(checkins.get(d)) ? 1 : 0;
                    c[2] += "SKIPPED".equals(checkins.get(d)) ? 1 : 0;
                }
                for (Map.Entry<LocalDate, int[]> e : weeks.entrySet()) {
                    int cap = Math.min(s.timesPerWeek(), e.getValue()[0]);
                    int d = Math.min(e.getValue()[1], cap);
                    int k = Math.min(e.getValue()[2], cap - d);
                    boolean ongoing = !today.isBefore(e.getKey()) && today.isBefore(e.getKey().plusDays(7));
                    scheduled += ongoing ? d + k : cap;
                    done += d;
                    skipped += k;
                }
            } else {
                for (LocalDate d = lo; !d.isAfter(hi); d = d.plusDays(1)) {
                    String st = checkins.get(d);
                    if (scheduledDay(s, d) && !(d.equals(today) && st == null)) {
                        scheduled++;
                        done += "DONE".equals(st) ? 1 : 0;
                        skipped += "SKIPPED".equals(st) ? 1 : 0;
                    }
                }
            }
        }
        int missed = scheduled - done - skipped;
        BigDecimal rate = scheduled - skipped == 0 ? null
                : new BigDecimal(done).divide(new BigDecimal(scheduled - skipped), 4, RoundingMode.HALF_UP);
        return new HabitStatistics.Stats(scheduled, done, skipped, missed, rate);
    }
}
