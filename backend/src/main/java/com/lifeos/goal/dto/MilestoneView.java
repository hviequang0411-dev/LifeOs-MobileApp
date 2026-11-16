package com.lifeos.goal.dto;

import java.math.BigDecimal;

/** Milestone kèm số Task liên kết (done/total) và tiến độ riêng (D185: hoàn thành = 1; total = 0 ⇒ 0). */
public record MilestoneView(MilestoneResponse milestone, long doneTasks, long totalTasks, BigDecimal progress) {
}
