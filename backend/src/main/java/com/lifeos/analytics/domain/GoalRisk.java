package com.lifeos.analytics.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Goal có nguy cơ trễ D200 (quy tắc tất định): chỉ Goal ACTIVE. Quá targetOn ⇒ OVERDUE. Có startOn, targetOn
 * (targetOn &gt; startOn) và tiến độ p: p &lt; elapsedRatio − ngưỡng ⇒ AT_RISK; elapsedRatio = (hôm nay − startOn) /
 * (targetOn − startOn), kẹp [0, 1]. Không tính được ⇒ không nhãn (null). Ngưỡng mặc định 0,15 (§4 kế hoạch).
 */
public final class GoalRisk {

    public static final String AT_RISK = "AT_RISK";
    public static final String OVERDUE = "OVERDUE";

    private GoalRisk() {
    }

    public static BigDecimal elapsedRatio(LocalDate startOn, LocalDate targetOn, LocalDate today) {
        if (startOn == null || targetOn == null || !targetOn.isAfter(startOn)) {
            return null;
        }
        long total = ChronoUnit.DAYS.between(startOn, targetOn);
        long elapsed = Math.max(0, Math.min(total, ChronoUnit.DAYS.between(startOn, today)));
        return BigDecimal.valueOf(elapsed).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    public static String label(String status, LocalDate startOn, LocalDate targetOn, BigDecimal progress,
            LocalDate today, BigDecimal threshold) {
        if (!"ACTIVE".equals(status)) {
            return null;
        }
        if (targetOn != null && today.isAfter(targetOn)) {
            return OVERDUE;
        }
        BigDecimal elapsed = elapsedRatio(startOn, targetOn, today);
        if (elapsed == null || progress == null) {
            return null;
        }
        return progress.compareTo(elapsed.subtract(threshold)) < 0 ? AT_RISK : null;
    }
}
