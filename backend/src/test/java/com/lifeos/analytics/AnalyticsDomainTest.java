package com.lifeos.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.analytics.domain.FinanceMetrics;
import com.lifeos.analytics.domain.GoalRisk;
import com.lifeos.analytics.domain.Period;
import com.lifeos.analytics.domain.TaskMetrics;
import com.lifeos.task.api.TaskStatsApi.CompletedTask;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

/**
 * INT-02 (kỳ theo múi giờ, DST, weekStartDay), INT-03 (chỉ số Task D198, mô hình tham chiếu độc lập), INT-04 phần
 * công thức (D199), INT-05 (Goal AT_RISK/OVERDUE, D200).
 */
class AnalyticsDomainTest {

    private static final ZoneId HCM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final ZoneId LA = ZoneId.of("America/Los_Angeles");

    @Test
    void Int_02_periodBoundariesFollowTheUserTimezoneWeekStartAndDst() {
        LocalDate wed = LocalDate.of(2027, 3, 10);
        assertThat(Period.week(wed, DayOfWeek.MONDAY).start()).isEqualTo(LocalDate.of(2027, 3, 8));
        assertThat(Period.week(wed, DayOfWeek.SUNDAY).start()).isEqualTo(LocalDate.of(2027, 3, 7));
        assertThat(Period.week(LocalDate.of(2027, 3, 7), DayOfWeek.SUNDAY).end()).isEqualTo(LocalDate.of(2027, 3, 13));
        Period feb = Period.month(YearMonth.of(2028, 2));
        assertThat(feb.end()).isEqualTo(LocalDate.of(2028, 2, 29));
        assertThat(feb.previous()).isEqualTo(Period.month(YearMonth.of(2028, 1)));
        assertThat(Period.week(wed, DayOfWeek.MONDAY).previous().start()).isEqualTo(LocalDate.of(2027, 3, 1));

        Period vn = Period.day(LocalDate.of(2027, 1, 1));
        assertThat(vn.startInstant(HCM)).isEqualTo(Instant.parse("2026-12-31T17:00:00Z"));
        assertThat(vn.startInstant(LA)).isEqualTo(Instant.parse("2027-01-01T08:00:00Z"));
        Period springForward = Period.day(LocalDate.of(2027, 3, 14));
        assertThat(Duration.between(springForward.startInstant(LA), springForward.endInstant(LA)))
                .as("DST nhảy tiến: ngày 23 giờ vẫn là một ngày").isEqualTo(Duration.ofHours(23));
        Period fallBack = Period.day(LocalDate.of(2027, 11, 7));
        assertThat(Duration.between(fallBack.startInstant(LA), fallBack.endInstant(LA))).isEqualTo(Duration.ofHours(25));

        assertThat(Period.isPeriodStart(Period.WEEK, LocalDate.of(2027, 3, 7), DayOfWeek.SUNDAY)).isTrue();
        assertThat(Period.isPeriodStart(Period.WEEK, LocalDate.of(2027, 3, 7), DayOfWeek.MONDAY)).isFalse();
        assertThat(Period.isPeriodStart(Period.MONTH, LocalDate.of(2027, 3, 2), DayOfWeek.MONDAY)).isFalse();
        assertThat(Period.bucketStart(wed, "month", DayOfWeek.MONDAY)).isEqualTo(LocalDate.of(2027, 3, 1));
    }

    @RepeatedTest(20)
    void Int_03_taskMetricsAgreeWithAnIndependentReference(RepetitionInfo info) {
        Random random = new Random(7_000 + info.getCurrentRepetition());
        ZoneId zone = random.nextBoolean() ? HCM : LA;
        for (int round = 0; round < 100; round++) {
            List<CompletedTask> rows = new ArrayList<>();
            int n = random.nextInt(12);
            for (int i = 0; i < n; i++) {
                Instant created = Instant.parse("2027-03-01T00:00:00Z").plusSeconds(random.nextInt(86_400 * 20));
                Instant completed = created.plusSeconds(random.nextInt(86_400 * 5));
                LocalDate due = random.nextInt(3) == 0 ? null
                        : LocalDate.ofInstant(completed, zone).plusDays(random.nextInt(3) - 1);
                LocalTime dueTime = due == null || random.nextBoolean() ? null : LocalTime.of(random.nextInt(24),
                        random.nextInt(4) * 15);
                rows.add(new CompletedTask(created, completed, due, dueTime, null, null));
            }
            TaskMetrics.Result r = TaskMetrics.compute(rows, zone);
            long withDue = 0;
            long onTime = 0;
            for (CompletedTask t : rows) {
                if (t.dueDate() == null) {
                    continue;
                }
                withDue++;
                LocalDateTime deadline = t.dueTime() == null ? t.dueDate().plusDays(1).atStartOfDay()
                        : t.dueDate().atTime(t.dueTime());
                LocalDateTime doneLocal = LocalDateTime.ofInstant(t.completedAt(), zone);
                boolean gapSafe = !zone.getRules().getValidOffsets(deadline).isEmpty();
                if (gapSafe ? !doneLocal.isAfter(deadline) : !t.completedAt().isAfter(
                        deadline.atZone(zone).toInstant())) {
                    onTime++;
                }
            }
            assertThat(r.completed()).isEqualTo(n);
            assertThat(r.completedWithDue()).isEqualTo(withDue);
            assertThat(r.completedOnTime()).as("vòng " + round).isEqualTo(onTime);
            assertThat(r.onTimeRate()).isEqualTo(withDue == 0 ? null
                    : BigDecimal.valueOf(onTime).divide(BigDecimal.valueOf(withDue), 4, RoundingMode.HALF_UP));
            List<Long> cycles = rows.stream().map(t -> Duration.between(t.createdAt(), t.completedAt())
                    .getSeconds()).sorted().toList();
            Long median = cycles.size() < 5 ? null : cycles.size() % 2 == 1 ? cycles.get(cycles.size() / 2)
                    : Math.floorDiv(cycles.get(cycles.size() / 2 - 1) + cycles.get(cycles.size() / 2), 2);
            assertThat(r.medianCycleSeconds()).as("mẫu < 5 ⇒ null").isEqualTo(median);
        }
    }

    @Test
    void Int_04_savingsRateAndBudgetPace() {
        assertThat(FinanceMetrics.savingsRate(new BigDecimal("10000000"), new BigDecimal("7500000")))
                .isEqualByComparingTo("0.25");
        assertThat(FinanceMetrics.savingsRate(BigDecimal.ZERO, new BigDecimal("1"))).as("thu = 0 ⇒ null").isNull();
        assertThat(FinanceMetrics.savingsRate(new BigDecimal("100"), new BigDecimal("150")))
                .isEqualByComparingTo("-0.5");
        assertThat(FinanceMetrics.pace(new BigDecimal("1500000"), new BigDecimal("3000000"),
                LocalDate.of(2027, 4, 15))).as("đúng nhịp giữa tháng 30 ngày").isEqualByComparingTo("1");
        assertThat(FinanceMetrics.pace(new BigDecimal("2000000"), new BigDecimal("3000000"),
                LocalDate.of(2027, 4, 10))).isEqualByComparingTo("2");
        assertThat(FinanceMetrics.pace(BigDecimal.ONE, BigDecimal.ZERO, LocalDate.of(2027, 4, 10))).isNull();
    }

    @Test
    void Int_05_goalRiskEdgeCases() {
        BigDecimal t = new BigDecimal("0.15");
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate target = start.plusDays(100);
        LocalDate mid = start.plusDays(50);
        assertThat(GoalRisk.label("ACTIVE", start, target, new BigDecimal("0.34"), mid, t)).as("0.34 < 0.5 − 0.15")
                .isEqualTo(GoalRisk.AT_RISK);
        assertThat(GoalRisk.label("ACTIVE", start, target, new BigDecimal("0.35"), mid, t)).as("biên: không nhỏ hơn")
                .isNull();
        assertThat(GoalRisk.label("ACTIVE", start, target, null, mid, t)).as("chưa có tiến độ").isNull();
        assertThat(GoalRisk.label("ACTIVE", null, target, BigDecimal.ZERO, mid, t)).as("thiếu startOn").isNull();
        assertThat(GoalRisk.label("ACTIVE", start, start, BigDecimal.ZERO, start, t)).as("targetOn = startOn").isNull();
        assertThat(GoalRisk.label("ACTIVE", start, target, BigDecimal.ONE, target.plusDays(1), t))
                .isEqualTo(GoalRisk.OVERDUE);
        assertThat(GoalRisk.label("ACTIVE", null, target, null, target.plusDays(1), t)).isEqualTo(GoalRisk.OVERDUE);
        assertThat(GoalRisk.label("PAUSED", start, target, BigDecimal.ZERO, target.plusDays(1), t)).isNull();
        assertThat(GoalRisk.label("ACHIEVED", start, target, BigDecimal.ZERO, mid, t)).isNull();
        assertThat(GoalRisk.elapsedRatio(start, target, start.minusDays(5))).isEqualByComparingTo("0");
        assertThat(GoalRisk.elapsedRatio(start, target, target.plusDays(5))).isEqualByComparingTo("1");
    }
}
