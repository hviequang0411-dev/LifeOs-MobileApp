package com.lifeos.analytics.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Chỉ số Finance D199 (một tiền tệ, số tiền BigDecimal chính xác): savingsRate = (thu − chi) / thu khi thu &gt; 0, trả 4
 * chữ số (làm tròn 1 chữ số % ở lớp hiển thị); pace = spent / (limit × ngày đã qua / số ngày của tháng), ≥ 1 = vượt
 * nhịp; "ngày đã qua" tính cả hôm nay [PROPOSED].
 */
public final class FinanceMetrics {

    private FinanceMetrics() {
    }

    public static BigDecimal savingsRate(BigDecimal income, BigDecimal expense) {
        if (income.signum() <= 0) {
            return null;
        }
        return income.subtract(expense).divide(income, 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal pace(BigDecimal spent, BigDecimal limit, LocalDate today) {
        if (limit.signum() <= 0) {
            return null;
        }
        BigDecimal expected = limit.multiply(BigDecimal.valueOf(today.getDayOfMonth()))
                .divide(BigDecimal.valueOf(today.lengthOfMonth()), 10, RoundingMode.HALF_UP);
        return spent.divide(expected, 4, RoundingMode.HALF_UP);
    }
}
