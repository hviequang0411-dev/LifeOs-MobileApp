package com.lifeos.goal.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** API thống kê chỉ đọc của module goal (PHASE_7 D197, D200): tiến độ D185 của Phase 6 là nguồn duy nhất. */
public interface GoalStatsApi {

    /** progress null = chưa có tiến độ (D185). */
    record GoalSnapshot(UUID goalId, String title, String status, LocalDate startOn, LocalDate targetOn,
            BigDecimal progress) {
        @Override
        public String toString() {
            return "GoalSnapshot[goalId=" + goalId + "]";
        }
    }

    /** Goal còn sống của user (tối đa 500, mới nhất trước). */
    List<GoalSnapshot> goals(UUID userId);
}
