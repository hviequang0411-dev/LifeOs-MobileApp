package com.lifeos.goal.dto;

import java.math.BigDecimal;

/**
 * Tiến độ Goal D185 kèm "con số thật" và cách tính (FR-GOAL-04): method ∈ {MILESTONES, TASKS, NONE}; value 0..1 (4 chữ
 * số) hoặc null khi chưa có gì để tính. allMilestonesCompleted chỉ là gợi ý ACHIEVED (D188, không tự đổi).
 */
public record ProgressResponse(String method, BigDecimal value, int completedMilestones, int totalMilestones,
        long doneTasks, long totalTasks, boolean allMilestonesCompleted) {
}
