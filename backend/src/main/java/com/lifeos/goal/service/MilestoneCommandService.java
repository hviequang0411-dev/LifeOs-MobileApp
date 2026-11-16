package com.lifeos.goal.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.sync.PendingChanges;
import com.lifeos.goal.dto.MilestoneResponse;
import com.lifeos.goal.entity.Milestone;
import com.lifeos.goal.mapper.GoalMapper;
import com.lifeos.goal.repository.GoalRepository;
import com.lifeos.goal.repository.MilestoneRepository;
import com.lifeos.task.api.TaskApi;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi Milestone (FR-GOAL-02, D186, D187). Mọi ghi khóa Goal FOR UPDATE (tuần tự theo goal: ≤ 20, position duy
 * nhất, không milestone mồ côi). Xóa milestone gỡ milestone_id của Task (giữ goal_id), change_logs Task trước.
 */
@Service
public class MilestoneCommandService {

    private static final String PK = "pk_milestones";
    private static final String POSITION_UNIQUE = "ux_milestones_goal_position";

    private final GoalRepository goals;
    private final MilestoneRepository milestones;
    private final TaskApi taskApi;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final GoalMapper mapper;
    private final Clock clock;

    public MilestoneCommandService(GoalRepository goals, MilestoneRepository milestones, TaskApi taskApi,
            ChangeRecorder changeRecorder, VersionedWrites versionedWrites, GoalMapper mapper, Clock clock) {
        this.goals = goals;
        this.milestones = milestones;
        this.taskApi = taskApi;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Goal đích: REST theo URL (404) hoặc tham chiếu trong payload sync (422 GOAL_NOT_FOUND). */
    public MilestoneResponse create(UUID userId, GoalCommands.CreateMilestone cmd, boolean goalFromUrl,
            UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (milestones.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                UUID goalId = GoalValues.requiredId(cmd.goalId().value(), "goalId");
                goals.findActiveForUpdate(goalId, userId).orElseThrow(() -> goalFromUrl ? GoalCommandService.notFound()
                        : GoalValues.reference(ErrorCode.GOAL_NOT_FOUND));
                String title = GoalValues.title(cmd.title().value());
                LocalDate dueOn = GoalValues.date(cmd.dueOn().value(), "dueOn");
                Long requested = cmd.position().value();
                long position = requested != null ? requested : milestones.maxPosition(goalId) + GoalValues.POSITION_GAP;
                requirePositionFree(goalId, position, cmd.id());
                if (milestones.countLive(goalId) >= GoalValues.MAX_MILESTONES) {
                    throw new ValidationException(ErrorCode.MILESTONE_LIMIT_EXCEEDED,
                            ErrorCode.MILESTONE_LIMIT_EXCEEDED.title());
                }
                Milestone m = Milestone.create(cmd.id(), userId, goalId);
                m.setTitle(title);
                m.setDueOn(dueOn);
                m.setPosition(position);
                if (Boolean.TRUE.equals(cmd.completed().value())) {
                    m.setCompletedAt(clock.instant());
                }
                Milestone saved = milestones.saveAndFlush(m);
                changeRecorder.record(userId, Milestone.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    public MilestoneResponse update(UUID userId, UUID milestoneId, GoalCommands.UpdateMilestone cmd,
            UUID mutationId) {
        return write(userId, milestoneId, cmd.expectedVersion(), mutationId, m -> {
            GoalValues.unchanged(cmd.goalId().present(), cmd.goalId().value(), m.getGoalId(), "goalId");
            String title = cmd.title().present() ? GoalValues.title(cmd.title().value()) : m.getTitle();
            LocalDate dueOn = cmd.dueOn().present() ? GoalValues.date(cmd.dueOn().value(), "dueOn") : m.getDueOn();
            long position = m.getPosition();
            if (cmd.position().present()) {
                if (cmd.position().value() == null) {
                    throw GoalValues.invalid("position", "REQUIRED", "position là bắt buộc.");
                }
                position = cmd.position().value();
                requirePositionFree(m.getGoalId(), position, milestoneId);
            }
            boolean changed = m.setTitle(title);
            changed |= m.setDueOn(dueOn);
            changed |= m.setPosition(position);
            if (cmd.completed().present()) {
                changed |= complete(m, Boolean.TRUE.equals(cmd.completed().value()));
            }
            return changed;
        });
    }

    public MilestoneResponse complete(UUID userId, UUID milestoneId, long expectedVersion) {
        return write(userId, milestoneId, expectedVersion, null, m -> complete(m, true));
    }

    public MilestoneResponse reopen(UUID userId, UUID milestoneId, long expectedVersion) {
        return write(userId, milestoneId, expectedVersion, null, m -> complete(m, false));
    }

    private boolean complete(Milestone m, boolean done) {
        if (done == (m.getCompletedAt() != null)) {
            return false;
        }
        return m.setCompletedAt(done ? clock.instant() : null);
    }

    public long delete(UUID userId, UUID milestoneId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            Milestone m = milestones.findActive(milestoneId, userId).orElseThrow(MilestoneCommandService::notFound);
            goals.findActiveForUpdate(m.getGoalId(), userId);
            VersionedWrites.requireVersion(m, expectedVersion);
            m.softDelete(clock.instant());
            Milestone saved = milestones.saveAndFlush(m);
            PendingChanges cascade = new PendingChanges();
            taskApi.detachFromMilestone(userId, milestoneId, cascade);
            cascade.recordAll(changeRecorder, userId);
            changeRecorder.record(userId, Milestone.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationId);
            return saved.getVersion();
        }, () -> milestones.findVersion(milestoneId, userId).orElseThrow(MilestoneCommandService::notFound));
    }

    private MilestoneResponse write(UUID userId, UUID milestoneId, long expectedVersion, UUID mutationId,
            Function<Milestone, Boolean> change) {
        try {
            return versionedWrites.write(() -> {
                Milestone m = milestones.findActive(milestoneId, userId)
                        .orElseThrow(MilestoneCommandService::notFound);
                VersionedWrites.requireVersion(m, expectedVersion);
                goals.findActiveForUpdate(m.getGoalId(), userId);
                if (!change.apply(m)) {
                    return mapper.toResponse(m);
                }
                Milestone saved = milestones.saveAndFlush(m);
                changeRecorder.record(userId, Milestone.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> milestones.findVersion(milestoneId, userId).orElseThrow(MilestoneCommandService::notFound));
        } catch (DataIntegrityViolationException e) {
            throw translate(e);
        }
    }

    private void requirePositionFree(UUID goalId, long position, UUID milestoneId) {
        if (milestones.positionTaken(goalId, position, milestoneId)) {
            throw GoalValues.invalid("position", "INVALID_VALUE", "Vị trí đã có cột mốc khác.");
        }
    }

    private static RuntimeException translate(DataIntegrityViolationException e) {
        if (GoalCommandService.mentions(e, PK)) {
            return new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
        }
        if (GoalCommandService.mentions(e, POSITION_UNIQUE)) {
            return GoalValues.invalid("position", "INVALID_VALUE", "Vị trí đã có cột mốc khác.");
        }
        return e;
    }

    static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.MILESTONE_NOT_FOUND);
    }
}
