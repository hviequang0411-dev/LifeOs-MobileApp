package com.lifeos.analytics.service;

import com.lifeos.analytics.domain.FinanceMetrics;
import com.lifeos.analytics.domain.GoalRisk;
import com.lifeos.calendar.api.CalendarStatsApi;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.finance.api.FinanceStatsApi;
import com.lifeos.goal.api.GoalStatsApi;
import com.lifeos.habit.api.HabitStatsApi;
import com.lifeos.task.api.TaskApi;
import com.lifeos.task.api.TaskDue;
import com.lifeos.task.api.TaskStatsApi;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Các thẻ dashboard FR-INT-03: việc hôm nay, habit hôm nay, ngân sách tháng, sự kiện sắp tới, Goal đang theo dõi. */
@Configuration
class DashboardCards {

    record TaskItem(UUID taskId, String title, String status, String dueTime) {
        @Override
        public String toString() {
            return "TaskItem[taskId=" + taskId + "]";
        }
    }

    record HabitItem(UUID habitId, String name, boolean scheduled, boolean done, int currentStreak) {
        @Override
        public String toString() {
            return "HabitItem[habitId=" + habitId + "]";
        }
    }

    record BudgetItem(UUID budgetId, UUID categoryId, String currency, String spent, String limit, BigDecimal ratio,
            BigDecimal pace) {
    }

    record GoalItem(UUID goalId, String title, BigDecimal progress, String risk) {
        @Override
        public String toString() {
            return "GoalItem[goalId=" + goalId + "]";
        }
    }

    private static DashboardCard card(String id, java.util.function.Function<UUID, Object> f) {
        return new DashboardCard() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Object compute(UUID userId) {
                return f.apply(userId);
            }
        };
    }

    @Bean
    DashboardCard tasksTodayCard(TaskApi tasks, TaskStatsApi stats, UserCalendar calendar) {
        return card("tasks_today", userId -> {
            LocalDate today = calendar.today(userId);
            List<TaskDue> due = tasks.dueBetween(userId, today, today, 20);
            return Map.of("dueToday", due.stream().map(t -> new TaskItem(t.taskId(), t.title(), t.status(),
                    t.dueTime() == null ? null : t.dueTime().toString())).toList(),
                    "overdueOpen", stats.overdueOpen(userId, today));
        });
    }

    @Bean
    DashboardCard habitsTodayCard(HabitStatsApi habits, UserCalendar calendar) {
        return card("habits_today", userId -> {
            LocalDate today = calendar.today(userId);
            return habits.stats(userId, today, today).stream().map(h -> new HabitItem(h.habitId(), h.name(),
                    h.scheduled() > 0, h.done() > 0, h.currentStreak())).toList();
        });
    }

    @Bean
    DashboardCard budgetsCard(FinanceStatsApi finance, UserCalendar calendar) {
        return card("budgets", userId -> {
            LocalDate today = calendar.today(userId);
            return finance.budgets(userId, YearMonth.from(today)).stream().map(b -> new BudgetItem(b.budgetId(),
                    b.categoryId(), b.currency(), b.spent(), b.limit(), b.ratio(),
                    FinanceMetrics.pace(new BigDecimal(b.spent()), new BigDecimal(b.limit()), today))).toList();
        });
    }

    @Bean
    DashboardCard upcomingEventsCard(CalendarStatsApi calendarStats, UserCalendar calendar, Clock clock) {
        return card("upcoming_events", userId -> calendarStats.upcoming(userId, clock.instant(),
                calendar.today(userId).plusDays(7), 5));
    }

    @Bean
    DashboardCard goalsCard(GoalStatsApi goals, UserCalendar calendar,
            @Value("${lifeos.analytics.at-risk-threshold:0.15}") BigDecimal threshold) {
        return card("goals", userId -> {
            LocalDate today = calendar.today(userId);
            return goals.goals(userId).stream().filter(g -> "ACTIVE".equals(g.status())).limit(10)
                    .map(g -> new GoalItem(g.goalId(), g.title(), g.progress(), GoalRisk.label(g.status(),
                            g.startOn(), g.targetOn(), g.progress(), today, threshold))).toList();
        });
    }
}
