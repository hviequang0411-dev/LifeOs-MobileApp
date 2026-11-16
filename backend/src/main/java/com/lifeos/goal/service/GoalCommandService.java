package com.lifeos.goal.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.sync.PendingChanges;
import com.lifeos.goal.api.GoalDeletionStarted;
import com.lifeos.goal.domain.GoalStatus;
import com.lifeos.goal.dto.GoalResponse;
import com.lifeos.goal.entity.Goal;
import com.lifeos.goal.entity.Milestone;
import com.lifeos.goal.mapper.GoalMapper;
import com.lifeos.goal.repository.GoalRepository;
import com.lifeos.goal.repository.MilestoneRepository;
import com.lifeos.task.api.TaskApi;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi Goal (FR-GOAL-01/05, D186, D188). Thứ tự kiểm D96; ChangeRecorder sau mọi ghi nghiệp vụ (D86). Xóa Goal:
 * khóa Goal FOR UPDATE → xóa mềm Goal, milestone → gỡ liên kết Task (TaskApi) → GoalDeletionStarted (note_links) →
 * change_logs cascade → milestone → Goal.
 */
@Service
public class GoalCommandService {

    private static final String PK = "pk_goals";

    private final GoalRepository goals;
    private final MilestoneRepository milestones;
    private final TaskApi taskApi;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final GoalMapper mapper;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public GoalCommandService(GoalRepository goals, MilestoneRepository milestones, TaskApi taskApi,
            ChangeRecorder changeRecorder, VersionedWrites versionedWrites, GoalMapper mapper, Clock clock,
            ApplicationEventPublisher events) {
        this.goals = goals;
        this.milestones = milestones;
        this.taskApi = taskApi;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
        this.events = events;
    }

    public GoalResponse create(UUID userId, GoalCommands.Create cmd, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (goals.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String title = GoalValues.title(cmd.title().value());
                String description = GoalValues.description(cmd.description().value());
                LocalDate startOn = GoalValues.date(cmd.startOn().value(), "startOn");
                LocalDate targetOn = GoalValues.date(cmd.targetOn().value(), "targetOn");
                GoalValues.range(startOn, targetOn);
                String status = cmd.status().present() ? GoalValues.status(cmd.status().value()) : GoalStatus.ACTIVE;
                Goal goal = new Goal(cmd.id(), userId);
                goal.setTitle(title);
                goal.setDescription(description);
                goal.setStartOn(startOn);
                goal.setTargetOn(targetOn);
                goal.setStatus(status);
                if (!GoalStatus.ACTIVE.equals(status)) {
                    goal.setStatusChangedAt(clock.instant());
                }
                Goal saved = goals.saveAndFlush(goal);
                changeRecorder.record(userId, Goal.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (mentions(e, PK)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    public GoalResponse update(UUID userId, UUID goalId, GoalCommands.Update cmd, UUID mutationId) {
        return write(userId, goalId, cmd.expectedVersion(), mutationId, goal -> {
            String title = cmd.title().present() ? GoalValues.title(cmd.title().value()) : goal.getTitle();
            String description = cmd.description().present() ? GoalValues.description(cmd.description().value())
                    : goal.getDescription();
            LocalDate startOn = cmd.startOn().present() ? GoalValues.date(cmd.startOn().value(), "startOn")
                    : goal.getStartOn();
            LocalDate targetOn = cmd.targetOn().present() ? GoalValues.date(cmd.targetOn().value(), "targetOn")
                    : goal.getTargetOn();
            GoalValues.range(startOn, targetOn);
            String status = cmd.status().present() ? GoalValues.status(cmd.status().value()) : goal.getStatus();
            GoalValues.transition(goal.getStatus(), status);
            boolean changed = goal.setTitle(title);
            changed |= goal.setDescription(description);
            changed |= goal.setStartOn(startOn);
            changed |= goal.setTargetOn(targetOn);
            changed |= changeStatus(goal, status);
            return changed;
        });
    }

    /** POST /goals/{id}/status (D188): cùng trạng thái → không đổi; chuyển sai → 422. */
    public GoalResponse changeStatus(UUID userId, UUID goalId, long expectedVersion, String rawStatus) {
        return write(userId, goalId, expectedVersion, null, goal -> {
            String status = GoalValues.status(rawStatus);
            GoalValues.transition(goal.getStatus(), status);
            return changeStatus(goal, status);
        });
    }

    private boolean changeStatus(Goal goal, String status) {
        if (!goal.setStatus(status)) {
            return false;
        }
        goal.setStatusChangedAt(clock.instant());
        return true;
    }

    public long delete(UUID userId, UUID goalId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            Goal goal = goals.findActiveForUpdate(goalId, userId).orElseThrow(GoalCommandService::notFound);
            VersionedWrites.requireVersion(goal, expectedVersion);
            Instant now = clock.instant();
            goal.softDelete(now);
            Goal saved = goals.saveAndFlush(goal);
            List<Milestone> children = milestones.findLiveByGoal(userId, goalId);
            children.forEach(m -> m.softDelete(now));
            List<Milestone> deleted = milestones.saveAllAndFlush(children);
            PendingChanges cascade = new PendingChanges();
            taskApi.detachFromGoal(userId, goalId, cascade);
            events.publishEvent(new GoalDeletionStarted(userId, goalId, cascade));
            cascade.recordAll(changeRecorder, userId);
            for (Milestone m : deleted) {
                changeRecorder.record(userId, Milestone.ENTITY_TYPE, m.getId(), ChangeOperation.DELETE,
                        m.getVersion(), null);
            }
            changeRecorder.record(userId, Goal.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationId);
            return saved.getVersion();
        }, () -> goals.findVersion(goalId, userId).orElseThrow(GoalCommandService::notFound));
    }

    private GoalResponse write(UUID userId, UUID goalId, long expectedVersion, UUID mutationId,
            Function<Goal, Boolean> change) {
        return versionedWrites.write(() -> {
            Goal goal = goals.findActive(goalId, userId).orElseThrow(GoalCommandService::notFound);
            VersionedWrites.requireVersion(goal, expectedVersion);
            if (!change.apply(goal)) {
                return mapper.toResponse(goal);
            }
            Goal saved = goals.saveAndFlush(goal);
            changeRecorder.record(userId, Goal.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationId);
            return mapper.toResponse(saved);
        }, () -> goals.findVersion(goalId, userId).orElseThrow(GoalCommandService::notFound));
    }

    static boolean mentions(DataIntegrityViolationException e, String constraint) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }

    static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.GOAL_NOT_FOUND);
    }
}
