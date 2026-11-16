package com.lifeos.analytics.service;

import com.lifeos.analytics.cache.AggregateCache;
import com.lifeos.analytics.domain.FinanceMetrics;
import com.lifeos.analytics.domain.GoalRisk;
import com.lifeos.analytics.domain.Period;
import com.lifeos.analytics.domain.TaskMetrics;
import com.lifeos.analytics.dto.FinanceAnalytics;
import com.lifeos.analytics.dto.GoalAnalytics;
import com.lifeos.analytics.dto.HabitAnalytics;
import com.lifeos.analytics.dto.TaskAnalytics;
import com.lifeos.analytics.dto.TimeAnalytics;
import com.lifeos.calendar.api.CalendarStatsApi;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.finance.api.FinanceStatsApi;
import com.lifeos.goal.api.GoalStatsApi;
import com.lifeos.habit.api.HabitStatsApi;
import com.lifeos.task.api.TaskStatsApi;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Analytics theo miền (FR-INT-04..08). Giới hạn D208; ngày theo múi giờ người dùng (D192). */
@Service
public class AnalyticsService {

    private final TaskStatsApi taskStats;
    private final HabitStatsApi habitStats;
    private final FinanceStatsApi financeStats;
    private final CalendarStatsApi calendarStats;
    private final GoalStatsApi goalStats;
    private final UserCalendar calendar;
    private final AggregateCache cache;
    private final Clock clock;
    private final BigDecimal riskThreshold;

    public AnalyticsService(TaskStatsApi taskStats, HabitStatsApi habitStats, FinanceStatsApi financeStats,
            CalendarStatsApi calendarStats, GoalStatsApi goalStats, UserCalendar calendar, AggregateCache cache,
            Clock clock, @Value("${lifeos.analytics.at-risk-threshold:0.15}") BigDecimal riskThreshold) {
        this.taskStats = taskStats;
        this.habitStats = habitStats;
        this.financeStats = financeStats;
        this.calendarStats = calendarStats;
        this.goalStats = goalStats;
        this.calendar = calendar;
        this.cache = cache;
        this.clock = clock;
        this.riskThreshold = riskThreshold;
    }

    public TaskAnalytics tasks(UUID userId, String rawFrom, String rawTo, String rawGranularity) {
        LocalDate from = AnalyticsValues.required(rawFrom, "from");
        LocalDate to = AnalyticsValues.required(rawTo, "to");
        String granularity = AnalyticsValues.granularity(rawGranularity);
        AnalyticsValues.range(from, to, granularity, false);
        return cache.get(userId, "analytics:tasks:" + granularity, from + ".." + to, () -> {
            ZoneId zone = calendar.zone(userId);
            DayOfWeek weekStart = calendar.weekStart(userId);
            List<TaskStatsApi.CompletedTask> rows = taskStats.completedBetween(userId,
                    from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant());
            TaskMetrics.Result m = TaskMetrics.compute(rows, zone);
            Map<LocalDate, long[]> buckets = buckets(from, to, granularity, weekStart, 1);
            for (TaskStatsApi.CompletedTask t : rows) {
                long[] b = buckets.get(Period.bucketStart(LocalDate.ofInstant(t.completedAt(), zone), granularity,
                        weekStart));
                if (b != null) {
                    b[0]++;
                }
            }
            List<TaskAnalytics.Bucket> out = new ArrayList<>();
            buckets.forEach((d, v) -> out.add(new TaskAnalytics.Bucket(d, v[0])));
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            LocalDate prevTo = from.minusDays(1);
            LocalDate prevFrom = prevTo.minusDays(days - 1);
            TaskMetrics.Result prev = TaskMetrics.compute(taskStats.completedBetween(userId,
                    prevFrom.atStartOfDay(zone).toInstant(), from.atStartOfDay(zone).toInstant()), zone);
            return new TaskAnalytics(from, to, granularity, clock.instant(), m.completed(), m.completedWithDue(),
                    m.completedOnTime(), m.onTimeRate(), m.medianCycleSeconds(),
                    taskStats.overdueOpen(userId, calendar.today(userId)), out, m.byProject(), m.byCategory(),
                    new TaskAnalytics.Previous(prevFrom, prevTo, prev.completed(), prev.onTimeRate()));
        });
    }

    public HabitAnalytics habits(UUID userId, String rawFrom, String rawTo) {
        LocalDate from = AnalyticsValues.required(rawFrom, "from");
        LocalDate to = AnalyticsValues.required(rawTo, "to");
        AnalyticsValues.range(from, to, "day", true);
        return cache.get(userId, "analytics:habits", from + ".." + to, () -> {
            List<HabitStatsApi.HabitPeriod> list = habitStats.stats(userId, from, to);
            var totals = ReviewSummaryService.habits(list);
            return new HabitAnalytics(from, to, clock.instant(), totals.scheduled(), totals.done(),
                    totals.completionRate(), list);
        });
    }

    public FinanceAnalytics finance(UUID userId, String rawFrom, String rawTo, String rawGranularity,
            String currency) {
        LocalDate from = AnalyticsValues.required(rawFrom, "from");
        LocalDate to = AnalyticsValues.required(rawTo, "to");
        String granularity = rawGranularity == null ? "month" : AnalyticsValues.granularity(rawGranularity);
        AnalyticsValues.range(from, to, granularity, false);
        return cache.get(userId, "analytics:finance:" + granularity + ":" + currency, from + ".." + to, () -> {
            FinanceStatsApi.Flow flow = financeStats.flow(userId, currency, from, to, granularity);
            return new FinanceAnalytics(clock.instant(), granularity, flow,
                    FinanceMetrics.savingsRate(new BigDecimal(flow.income()), new BigDecimal(flow.expense())));
        });
    }

    public TimeAnalytics time(UUID userId, String rawFrom, String rawTo, String rawGranularity) {
        LocalDate from = AnalyticsValues.required(rawFrom, "from");
        LocalDate to = AnalyticsValues.required(rawTo, "to");
        String granularity = AnalyticsValues.granularity(rawGranularity);
        AnalyticsValues.range(from, to, granularity, true);
        return cache.get(userId, "analytics:time:" + granularity, from + ".." + to, () -> {
            DayOfWeek weekStart = calendar.weekStart(userId);
            Map<LocalDate, long[]> buckets = buckets(from, to, granularity, weekStart, 3);
            long eventMinutes = 0;
            long blockMinutes = 0;
            int events = 0;
            for (CalendarStatsApi.DayAllocation d : calendarStats.allocation(userId, from, to)) {
                long[] b = buckets.get(Period.bucketStart(d.day(), granularity, weekStart));
                b[0] += d.eventMinutes();
                b[1] += d.blockMinutes();
                b[2] += d.events();
                eventMinutes += d.eventMinutes();
                blockMinutes += d.blockMinutes();
                events += d.events();
            }
            List<TimeAnalytics.Bucket> out = new ArrayList<>();
            buckets.forEach((d, v) -> out.add(new TimeAnalytics.Bucket(d, v[0], v[1], (int) v[2])));
            return new TimeAnalytics(from, to, granularity, clock.instant(), eventMinutes, blockMinutes, events, out);
        });
    }

    public GoalAnalytics goals(UUID userId) {
        return cache.get(userId, "analytics:goals", "all", () -> {
            LocalDate today = calendar.today(userId);
            List<GoalAnalytics.Item> items = goalStats.goals(userId).stream().map(g -> new GoalAnalytics.Item(
                    g.goalId(), g.title(), g.status(), g.startOn(), g.targetOn(), g.progress(),
                    GoalRisk.elapsedRatio(g.startOn(), g.targetOn(), today),
                    GoalRisk.label(g.status(), g.startOn(), g.targetOn(), g.progress(), today, riskThreshold)))
                    .toList();
            return new GoalAnalytics(clock.instant(), riskThreshold.setScale(2, RoundingMode.HALF_UP), items);
        });
    }

    BigDecimal riskThreshold() {
        return riskThreshold;
    }

    private static Map<LocalDate, long[]> buckets(LocalDate from, LocalDate to, String granularity,
            DayOfWeek weekStart, int width) {
        Map<LocalDate, long[]> buckets = new TreeMap<>();
        for (LocalDate d = Period.bucketStart(from, granularity, weekStart); !d.isAfter(to);
                d = Period.nextBucket(d, granularity)) {
            buckets.put(d, new long[width]);
        }
        return buckets;
    }
}
