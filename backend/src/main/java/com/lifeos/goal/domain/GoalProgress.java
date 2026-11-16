package com.lifeos.goal.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

/**
 * Tiến độ Goal D185 — tính, không lưu. Có milestone: trung bình của tiến độ từng milestone (hoàn thành = 1, ngược lại
 * done/total của Task liên kết, total = 0 ⇒ 0); không milestone mà có Task gắn thẳng: done/total; không có gì ⇒ null.
 * Tính bằng phân số chính xác rồi làm tròn HALF_UP 4 chữ số (giá trị 0..1) [PROPOSED: thang 0..1, 4 chữ số].
 */
public final class GoalProgress {

    public static final String BY_MILESTONES = "MILESTONES";
    public static final String BY_TASKS = "TASKS";
    public static final String NONE = "NONE";

    /** Một milestone: đã hoàn thành chưa và số Task liên kết (còn sống) done/total. */
    public record MilestoneInput(boolean completed, long doneTasks, long totalTasks) {
    }

    public record Result(String method, BigDecimal value) {
    }

    private GoalProgress() {
    }

    public static Result compute(List<MilestoneInput> milestones, long goalDoneTasks, long goalTotalTasks) {
        if (!milestones.isEmpty()) {
            BigInteger num = BigInteger.ZERO;
            BigInteger den = BigInteger.ONE;
            for (MilestoneInput m : milestones) {
                long n = m.completed() ? 1 : (m.totalTasks() == 0 ? 0 : m.doneTasks());
                long d = m.completed() ? 1 : (m.totalTasks() == 0 ? 1 : m.totalTasks());
                // num/den + n/d
                num = num.multiply(BigInteger.valueOf(d)).add(BigInteger.valueOf(n).multiply(den));
                den = den.multiply(BigInteger.valueOf(d));
            }
            den = den.multiply(BigInteger.valueOf(milestones.size()));
            return new Result(BY_MILESTONES, ratio(num, den));
        }
        if (goalTotalTasks > 0) {
            return new Result(BY_TASKS, ratio(BigInteger.valueOf(goalDoneTasks), BigInteger.valueOf(goalTotalTasks)));
        }
        return new Result(NONE, null);
    }

    public static BigDecimal ratio(BigInteger num, BigInteger den) {
        return new BigDecimal(num).divide(new BigDecimal(den), 4, RoundingMode.HALF_UP);
    }
}
