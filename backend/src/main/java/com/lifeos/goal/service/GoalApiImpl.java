package com.lifeos.goal.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.goal.GoalLinks;
import com.lifeos.goal.api.GoalApi;
import com.lifeos.goal.entity.Milestone;
import com.lifeos.goal.repository.GoalRepository;
import com.lifeos.goal.repository.MilestoneRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài đặt {@link GoalApi} và {@link GoalLinks} (D186). Khóa FOR SHARE theo thứ tự goal → milestone, cùng thứ tự với
 * ghi milestone/xóa goal (goal FOR UPDATE trước) nên không deadlock.
 */
@Service
class GoalApiImpl implements GoalApi, GoalLinks {

    private final GoalRepository goals;
    private final MilestoneRepository milestones;

    GoalApiImpl(GoalRepository goals, MilestoneRepository milestones) {
        this.goals = goals;
        this.milestones = milestones;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockForShare(UUID userId, UUID goalId) {
        return goals.findActiveForShare(goalId, userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID userId, UUID goalId) {
        return goals.findActive(goalId, userId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean goalExists(UUID userId, UUID goalId) {
        return exists(userId, goalId);
    }

    /**
     * milestoneId có mặt ⇒ goal hiệu lực = goal của milestone; goalId gửi kèm phải khớp (GOAL_LINK_MISMATCH). Goal khóa
     * trước milestone: đọc goal của milestone không khóa, khóa goal, rồi khóa milestone và kiểm lại.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Link resolve(UUID userId, UUID goalIdOrNull, UUID milestoneIdOrNull) {
        if (milestoneIdOrNull == null) {
            if (goalIdOrNull == null) {
                return new Link(null, null);
            }
            if (goals.findActiveForShare(goalIdOrNull, userId).isEmpty()) {
                throw GoalValues.reference(ErrorCode.GOAL_NOT_FOUND);
            }
            return new Link(goalIdOrNull, null);
        }
        Milestone peek = milestones.findActive(milestoneIdOrNull, userId)
                .orElseThrow(() -> GoalValues.reference(ErrorCode.MILESTONE_NOT_FOUND));
        if (goalIdOrNull != null && !goalIdOrNull.equals(peek.getGoalId())) {
            if (goals.findActive(goalIdOrNull, userId).isEmpty()) {
                throw GoalValues.reference(ErrorCode.GOAL_NOT_FOUND);
            }
            throw GoalValues.reference(ErrorCode.GOAL_LINK_MISMATCH);
        }
        if (goals.findActiveForShare(peek.getGoalId(), userId).isEmpty()) {
            throw GoalValues.reference(ErrorCode.MILESTONE_NOT_FOUND);
        }
        if (milestones.findActiveForShare(milestoneIdOrNull, userId).isEmpty()) {
            throw GoalValues.reference(ErrorCode.MILESTONE_NOT_FOUND);
        }
        return new Link(peek.getGoalId(), milestoneIdOrNull);
    }
}
