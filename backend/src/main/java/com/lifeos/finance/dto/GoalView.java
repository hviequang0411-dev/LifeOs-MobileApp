package com.lifeos.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** GET /saving-goals: mục tiêu kèm tiến độ {saved, target, ratio, remaining} tính khi đọc. */
public record GoalView(UUID id, String name, String currency, String targetAmount, LocalDate targetOn,
        String status, long version, Instant createdAt, Instant updatedAt, String saved, String target,
        BigDecimal ratio, String remaining) {

    public static GoalView of(GoalResponse g, String saved, BigDecimal ratio, String remaining) {
        return new GoalView(g.id(), g.name(), g.currency(), g.targetAmount(), g.targetOn(), g.status(), g.version(),
                g.createdAt(), g.updatedAt(), saved, g.targetAmount(), ratio, remaining);
    }

    @Override
    public String toString() {
        return "GoalView[id=" + id + ", version=" + version + "]";
    }
}
