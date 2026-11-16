package com.lifeos.analytics.service;

import com.lifeos.analytics.cache.AggregateCache;
import com.lifeos.analytics.domain.Period;
import com.lifeos.analytics.domain.TaskMetrics;
import com.lifeos.analytics.dto.ReviewSummary;
import com.lifeos.calendar.api.CalendarStatsApi;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.finance.api.FinanceStatsApi;
import com.lifeos.habit.api.HabitStatsApi;
import com.lifeos.task.api.TaskStatsApi;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Daily/Weekly/Monthly Review (FR-INT-01/02, D190, D192): số liệu tính khi đọc từ API thống kê của từng module (D197),
 * kỳ theo múi giờ và weekStartDay hiện hành của người dùng. Weekly/Monthly so sánh với kỳ liền trước.
 */
@Service
public class ReviewSummaryService {

    private final TaskStatsApi taskStats;
    private final HabitStatsApi habitStats;
    private final FinanceStatsApi financeStats;
    private final CalendarStatsApi calendarStats;
    private final ReviewQueryService reviews;
    private final UserCalendar calendar;
    private final AggregateCache cache;
    private final Clock clock;

    public ReviewSummaryService(TaskStatsApi taskStats, HabitStatsApi habitStats, FinanceStatsApi financeStats,
            CalendarStatsApi calendarStats, ReviewQueryService reviews, UserCalendar calendar, AggregateCache cache,
            Clock clock) {
        this.taskStats = taskStats;
        this.habitStats = habitStats;
        this.financeStats = financeStats;
        this.calendarStats = calendarStats;
        this.reviews = reviews;
        this.calendar = calendar;
        this.cache = cache;
        this.clock = clock;
    }

    public ReviewSummary daily(UUID userId, String rawDate) {
        LocalDate date = AnalyticsValues.date(rawDate, "date");
        return summary(userId, Period.day(date == null ? calendar.today(userId) : date), false);
    }

    public ReviewSummary weekly(UUID userId, String rawDate) {
        LocalDate date = AnalyticsValues.date(rawDate, "date");
        return summary(userId, Period.week(date == null ? calendar.today(userId) : date, calendar.weekStart(userId)),
                true);
    }

    public ReviewSummary monthly(UUID userId, String rawMonth) {
        YearMonth month = AnalyticsValues.month(rawMonth);
        return summary(userId, Period.month(month == null ? YearMonth.from(calendar.today(userId)) : month), true);
    }

    private ReviewSummary summary(UUID userId, Period p, boolean compare) {
        return cache.get(userId, "review:" + p.type(), p.key(), () -> compute(userId, p, compare));
    }

    private ReviewSummary compute(UUID userId, Period p, boolean compare) {
        ZoneId zone = calendar.zone(userId);
        LocalDate today = calendar.today(userId);
        TaskMetrics.Result t = TaskMetrics.compute(taskStats.completedBetween(userId, p.startInstant(zone),
                p.endInstant(zone)), zone);
        ReviewSummary.Tasks tasks = new ReviewSummary.Tasks(t.completed(), t.completedWithDue(), t.completedOnTime(),
                t.onTimeRate(), taskStats.overdueOpen(userId, today), t.medianCycleSeconds());
        List<HabitStatsApi.HabitPeriod> habitList = habitStats.stats(userId, p.start(), p.end());
        ReviewSummary.Habits habits = habits(habitList);
        int events = 0;
        long eventMinutes = 0;
        long blockMinutes = 0;
        for (CalendarStatsApi.DayAllocation d : calendarStats.allocation(userId, p.start(), p.end())) {
            events += d.events();
            eventMinutes += d.eventMinutes();
            blockMinutes += d.blockMinutes();
        }
        ReviewSummary.Comparison previous = null;
        if (compare) {
            Period prev = p.previous();
            long prevCompleted = taskStats.completedBetween(userId, prev.startInstant(zone), prev.endInstant(zone))
                    .size();
            int prevDone = habitStats.stats(userId, prev.start(), prev.end()).stream()
                    .mapToInt(HabitStatsApi.HabitPeriod::done).sum();
            previous = new ReviewSummary.Comparison(prev.key(), prevCompleted, prevDone,
                    financeStats.totals(userId, prev.start(), prev.end()));
        }
        return new ReviewSummary(p.type(), p.key(), p.start(), p.end(), clock.instant(), tasks, habits,
                financeStats.totals(userId, p.start(), p.end()), new ReviewSummary.Time(events, eventMinutes,
                        blockMinutes), reviews.forPeriod(userId, p), previous);
    }

    static ReviewSummary.Habits habits(List<HabitStatsApi.HabitPeriod> list) {
        int scheduled = 0;
        int done = 0;
        int skipped = 0;
        int missed = 0;
        for (HabitStatsApi.HabitPeriod h : list) {
            scheduled += h.scheduled();
            done += h.done();
            skipped += h.skipped();
            missed += h.missed();
        }
        int denominator = scheduled - skipped;
        BigDecimal rate = denominator <= 0 ? null
                : BigDecimal.valueOf(Math.min(done, denominator)).divide(BigDecimal.valueOf(denominator), 4,
                        RoundingMode.HALF_UP);
        List<ReviewSummary.HabitItem> top = list.stream()
                .sorted(Comparator.comparingInt(HabitStatsApi.HabitPeriod::done).reversed()
                        .thenComparing(h -> h.habitId().toString()))
                .limit(10).map(h -> new ReviewSummary.HabitItem(h.habitId(), h.name(), h.scheduled(), h.done(),
                        h.completionRate())).toList();
        return new ReviewSummary.Habits(scheduled, done, skipped, missed, rate, top);
    }
}
