package com.lifeos.task.api;

import java.util.UUID;

/** Số Task còn sống (total) và đã DONE (done) theo (goal, milestone) — tiến độ Goal D185. milestoneId null = gắn thẳng. */
public record GoalTaskCount(UUID goalId, UUID milestoneId, long done, long total) {
}
