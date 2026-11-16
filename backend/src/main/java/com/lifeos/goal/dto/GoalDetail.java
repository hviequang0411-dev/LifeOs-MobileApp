package com.lifeos.goal.dto;

import java.util.List;

/** GET /goals, /goals/{id}: Goal + tiến độ tính + milestone theo thứ tự position. */
public record GoalDetail(GoalResponse goal, ProgressResponse progress, List<MilestoneView> milestones) {
}
