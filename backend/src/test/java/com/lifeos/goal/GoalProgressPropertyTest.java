package com.lifeos.goal;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.goal.domain.GoalProgress;
import com.lifeos.goal.domain.GoalStatus;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

/**
 * KNW-09 (PHASE_6 §9, D185): mô hình tham chiếu viết độc lập (số thập phân 50 chữ số rồi làm tròn HALF_UP 4 chữ số) so
 * với GoalProgress trên dữ liệu ngẫu nhiên: có/không milestone, milestone hoàn thành, total = 0, không có gì ⇒ null.
 * KNW-11: bảng chuyển trạng thái D188.
 */
class GoalProgressPropertyTest {

    private static final MathContext MC = new MathContext(50, RoundingMode.HALF_UP);

    @RepeatedTest(20)
    void Knw_09_progressAgreesWithTheReferenceModel(RepetitionInfo info) {
        Random random = new Random(20261107L + info.getCurrentRepetition());
        for (int round = 0; round < 500; round++) {
            List<GoalProgress.MilestoneInput> milestones = new ArrayList<>();
            int count = random.nextInt(3) == 0 ? 0 : 1 + random.nextInt(20);
            for (int i = 0; i < count; i++) {
                long total = random.nextInt(4) == 0 ? 0 : random.nextInt(30);
                long done = total == 0 ? 0 : random.nextInt((int) total + 1);
                milestones.add(new GoalProgress.MilestoneInput(random.nextInt(4) == 0, done, total));
            }
            long goalTotal = random.nextInt(3) == 0 ? 0 : random.nextInt(50);
            long goalDone = goalTotal == 0 ? 0 : random.nextInt((int) goalTotal + 1);

            GoalProgress.Result result = GoalProgress.compute(milestones, goalDone, goalTotal);
            String context = "lần " + info.getCurrentRepetition() + ", vòng " + round;
            assertThat(result.value()).as(context).isEqualTo(reference(milestones, goalDone, goalTotal));
            assertThat(result.method()).as(context).isEqualTo(!milestones.isEmpty() ? GoalProgress.BY_MILESTONES
                    : goalTotal > 0 ? GoalProgress.BY_TASKS : GoalProgress.NONE);
            if (result.value() != null) {
                assertThat(result.value()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
                assertThat(result.value().scale()).isEqualTo(4);
            }
        }
    }

    private static BigDecimal reference(List<GoalProgress.MilestoneInput> milestones, long done, long total) {
        if (!milestones.isEmpty()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (GoalProgress.MilestoneInput m : milestones) {
                BigDecimal p = m.completed() ? BigDecimal.ONE : m.totalTasks() == 0 ? BigDecimal.ZERO
                        : BigDecimal.valueOf(m.doneTasks()).divide(BigDecimal.valueOf(m.totalTasks()), MC);
                sum = sum.add(p, MC);
            }
            return sum.divide(BigDecimal.valueOf(milestones.size()), MC).setScale(4, RoundingMode.HALF_UP);
        }
        if (total > 0) {
            return BigDecimal.valueOf(done).divide(BigDecimal.valueOf(total), MC).setScale(4, RoundingMode.HALF_UP);
        }
        return null;
    }

    @Test
    void Knw_09_examplesFromTheSpec() {
        assertThat(GoalProgress.compute(List.of(), 0, 0).value()).as("không có gì ⇒ null, không phải 0").isNull();
        assertThat(GoalProgress.compute(List.of(), 1, 3).value()).isEqualByComparingTo("0.3333");
        assertThat(GoalProgress.compute(List.of(new GoalProgress.MilestoneInput(true, 0, 0),
                new GoalProgress.MilestoneInput(false, 1, 2), new GoalProgress.MilestoneInput(false, 0, 0)), 9, 9)
                .value()).as("(1 + 0.5 + 0) / 3; Task gắn thẳng bị bỏ qua khi có milestone").isEqualByComparingTo(
                        "0.5000");
        assertThat(GoalProgress.compute(List.of(new GoalProgress.MilestoneInput(false, 2, 3)), 0, 0).value())
                .isEqualByComparingTo("0.6667");
    }

    @Test
    void Knw_11_statusTransitionTable() {
        String[] all = {GoalStatus.ACTIVE, GoalStatus.PAUSED, GoalStatus.ACHIEVED, GoalStatus.ABANDONED};
        List<String> allowed = new ArrayList<>();
        for (String from : all) {
            for (String to : all) {
                if (!from.equals(to) && GoalStatus.allowed(from, to)) {
                    allowed.add(from + "→" + to);
                }
            }
        }
        assertThat(allowed).containsExactlyInAnyOrder("ACTIVE→PAUSED", "ACTIVE→ACHIEVED", "ACTIVE→ABANDONED",
                "PAUSED→ACTIVE", "ACHIEVED→ACTIVE", "ABANDONED→ACTIVE");
        for (String s : all) {
            assertThat(GoalStatus.allowed(s, s)).as("cùng trạng thái là không đổi").isTrue();
        }
    }
}
