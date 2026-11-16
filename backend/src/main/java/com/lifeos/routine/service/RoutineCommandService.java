package com.lifeos.routine.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.persistence.SyncedEntity;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.routine.dto.RoutineResponse;
import com.lifeos.routine.dto.StepResponse;
import com.lifeos.routine.entity.Routine;
import com.lifeos.routine.entity.RoutineLog;
import com.lifeos.routine.entity.RoutineStep;
import com.lifeos.routine.entity.RoutineStepLog;
import com.lifeos.routine.mapper.RoutineMapper;
import com.lifeos.routine.repository.RoutineLogRepository;
import com.lifeos.routine.repository.RoutineRepository;
import com.lifeos.routine.repository.RoutineStepLogRepository;
import com.lifeos.routine.repository.RoutineStepRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi Routine và bước (FR-RTN-01/02/05, D240, D241, D246). Thứ tự kiểm D96; ChangeRecorder sau mọi khóa/flush
 * (D86). Ghi bước khóa Routine FOR UPDATE (tuần tự theo routine: giới hạn 30 bước, vị trí duy nhất, không bước mồ côi).
 */
@Service
public class RoutineCommandService {

    private static final String ROUTINE_PK = "pk_routines";
    private static final String STEP_PK = "pk_routine_steps";
    private static final String POSITION_UNIQUE = "ux_routine_steps_routine_position";

    private final RoutineRepository routines;
    private final RoutineStepRepository steps;
    private final RoutineLogRepository logs;
    private final RoutineStepLogRepository stepLogs;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final RoutineMapper mapper;
    private final Clock clock;
    private final org.springframework.context.ApplicationEventPublisher events;

    public RoutineCommandService(RoutineRepository routines, RoutineStepRepository steps, RoutineLogRepository logs,
            RoutineStepLogRepository stepLogs, ChangeRecorder changeRecorder, VersionedWrites versionedWrites,
            RoutineMapper mapper, Clock clock,
            org.springframework.context.ApplicationEventPublisher events) {
        this.routines = routines;
        this.steps = steps;
        this.logs = logs;
        this.stepLogs = stepLogs;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
        this.events = events;
    }

    // ---------- Routine

    public RoutineResponse create(UUID userId, RoutineCommands.Create cmd, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (routines.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String name = RoutineValues.text(cmd.name().value(), "name");
                String kind = cmd.kind().value();
                Integer weekdays = cmd.weekdays().value();
                RoutineValues.schedule(kind, weekdays);
                var reminder = RoutineValues.time(cmd.reminderTime().value());
                if (routines.countLive(userId) >= RoutineValues.MAX_ROUTINES) {
                    throw RoutineValues.limitExceeded();
                }
                Routine saved = routines.saveAndFlush(Routine.create(cmd.id(), userId, name, kind, weekdays,
                        reminder));
                changeRecorder.record(userId, Routine.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw translate(e, ROUTINE_PK);
        }
    }

    public RoutineResponse update(UUID userId, UUID routineId, RoutineCommands.Update cmd, UUID mutationId) {
        return writeRoutine(userId, routineId, cmd.expectedVersion(), mutationId, routine -> {
            String name = cmd.name().present() ? RoutineValues.text(cmd.name().value(), "name") : routine.getName();
            boolean replaced = cmd.kind().present();
            String kind = replaced ? cmd.kind().value() : routine.getKind();
            Integer weekdays = replaced ? cmd.weekdays().value()
                    : cmd.weekdays().present() ? cmd.weekdays().value() : routine.getWeekdays();
            RoutineValues.schedule(kind, weekdays);
            var reminder = cmd.reminderTime().present() ? RoutineValues.time(cmd.reminderTime().value())
                    : routine.getReminderTime();
            return routine.apply(name, kind, weekdays, reminder);
        });
    }

    public RoutineResponse archive(UUID userId, UUID routineId, long expectedVersion) {
        return writeRoutine(userId, routineId, expectedVersion, null, r -> r.archive(clock.instant()));
    }

    public RoutineResponse unarchive(UUID userId, UUID routineId, long expectedVersion) {
        return writeRoutine(userId, routineId, expectedVersion, null, Routine::unarchive);
    }

    /**
     * Xóa Routine (D246): khóa → kiểm version → xóa mềm + flush → xóa mềm hết bước, lượt, kết quả bước (tải hết theo id,
     * flush) → change_logs từng bản ghi con (mutation_id NULL: kết quả bước, lượt, bước) → change_logs của Routine.
     */
    public long delete(UUID userId, UUID routineId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            Routine routine = routines.findActiveForUpdate(routineId, userId).orElseThrow(RoutineCommandService::notFound);
            VersionedWrites.requireVersion(routine, expectedVersion);
            Instant now = clock.instant();
            routine.softDelete(now);
            Routine saved = routines.saveAndFlush(routine);
            List<RoutineLog> runs = logs.findLiveByRoutine(userId, routineId);
            List<RoutineStepLog> results = runs.isEmpty() ? List.of()
                    : stepLogs.findLiveByLogs(userId, runs.stream().map(RoutineLog::getId).toList());
            List<RoutineStep> children = steps.findLiveByRoutine(userId, routineId);
            results.forEach(r -> r.softDelete(now));
            runs.forEach(r -> r.softDelete(now));
            children.forEach(s -> s.softDelete(now));
            List<RoutineStepLog> deletedResults = stepLogs.saveAllAndFlush(results);
            List<RoutineLog> deletedRuns = logs.saveAllAndFlush(runs);
            List<RoutineStep> deletedSteps = steps.saveAllAndFlush(children);
            // PHASE_4 D129: reminder của Routine bị xóa mềm trong cùng transaction (trước mọi change_logs).
            events.publishEvent(new com.lifeos.routine.api.RoutineDeletionStarted(userId, routineId));
            recordDeletes(userId, RoutineStepLog.ENTITY_TYPE, deletedResults);
            recordDeletes(userId, RoutineLog.ENTITY_TYPE, deletedRuns);
            recordDeletes(userId, RoutineStep.ENTITY_TYPE, deletedSteps);
            changeRecorder.record(userId, Routine.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationId);
            return saved.getVersion();
        }, () -> routines.findVersion(routineId, userId).orElseThrow(RoutineCommandService::notFound));
    }

    private RoutineResponse writeRoutine(UUID userId, UUID routineId, long expectedVersion, UUID mutationId,
            Function<Routine, Boolean> change) {
        return versionedWrites.write(() -> {
            Routine routine = routines.findActive(routineId, userId).orElseThrow(RoutineCommandService::notFound);
            VersionedWrites.requireVersion(routine, expectedVersion);
            if (!change.apply(routine)) {
                return mapper.toResponse(routine);
            }
            Routine saved = routines.saveAndFlush(routine);
            changeRecorder.record(userId, Routine.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationId);
            return mapper.toResponse(saved);
        }, () -> routines.findVersion(routineId, userId).orElseThrow(RoutineCommandService::notFound));
    }

    // ---------- Bước

    /**
     * Thêm bước (D241). Routine đích: REST theo URL (404) hoặc tham chiếu trong payload sync (422). Vị trí vắng = max +
     * 1000; vị trí đã có bước hoạt động khác → VALIDATION_FAILED position [PROPOSED]; ≤ 30 bước → LIMIT_EXCEEDED.
     */
    public StepResponse createStep(UUID userId, RoutineCommands.CreateStep cmd, boolean routineFromUrl,
            UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (steps.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                UUID routineId = RoutineValues.requiredId(cmd.routineId().value(), "routineId");
                routines.findActiveForUpdate(routineId, userId).orElseThrow(() -> routineFromUrl ? notFound()
                        : RoutineValues.reference(ErrorCode.ROUTINE_NOT_FOUND));
                String title = RoutineValues.text(cmd.title().value(), "title");
                Integer duration = RoutineValues.duration(cmd.durationMinutes().value());
                Long requested = cmd.position().value();
                long position = requested != null ? requested : steps.maxPosition(routineId) + RoutineValues.POSITION_GAP;
                requirePositionFree(routineId, position, cmd.id());
                if (steps.countLive(routineId) >= RoutineValues.MAX_STEPS) {
                    throw RoutineValues.limitExceeded();
                }
                RoutineStep saved = steps.saveAndFlush(RoutineStep.create(cmd.id(), userId, routineId, title, position,
                        duration));
                changeRecorder.record(userId, RoutineStep.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw translate(e, STEP_PK);
        }
    }

    public StepResponse updateStep(UUID userId, UUID stepId, RoutineCommands.UpdateStep cmd, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                RoutineStep step = steps.findActive(stepId, userId).orElseThrow(RoutineCommandService::stepNotFound);
                VersionedWrites.requireVersion(step, cmd.expectedVersion());
                routines.findActiveForUpdate(step.getRoutineId(), userId);
                RoutineValues.unchanged(cmd.routineId().present(), cmd.routineId().value(), step.getRoutineId(),
                        "routineId");
                String title = cmd.title().present() ? RoutineValues.text(cmd.title().value(), "title")
                        : step.getTitle();
                Integer duration = cmd.durationMinutes().present()
                        ? RoutineValues.duration(cmd.durationMinutes().value()) : step.getDurationMinutes();
                long position = step.getPosition();
                if (cmd.position().present()) {
                    if (cmd.position().value() == null) {
                        throw RoutineValues.invalid("position", "REQUIRED", "position là bắt buộc.");
                    }
                    position = cmd.position().value();
                    requirePositionFree(step.getRoutineId(), position, stepId);
                }
                if (!step.apply(title, position, duration)) {
                    return mapper.toResponse(step);
                }
                RoutineStep saved = steps.saveAndFlush(step);
                changeRecorder.record(userId, RoutineStep.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> steps.findVersion(stepId, userId).orElseThrow(RoutineCommandService::stepNotFound));
        } catch (DataIntegrityViolationException e) {
            throw translate(e, STEP_PK);
        }
    }

    /** Xóa bước (D246): tombstone bước, giữ kết quả bước cũ; lượt đã chạy không đổi (D249). */
    public long deleteStep(UUID userId, UUID stepId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            RoutineStep step = steps.findActive(stepId, userId).orElseThrow(RoutineCommandService::stepNotFound);
            VersionedWrites.requireVersion(step, expectedVersion);
            routines.findActiveForUpdate(step.getRoutineId(), userId);
            step.softDelete(clock.instant());
            RoutineStep saved = steps.saveAndFlush(step);
            changeRecorder.record(userId, RoutineStep.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationId);
            return saved.getVersion();
        }, () -> steps.findVersion(stepId, userId).orElseThrow(RoutineCommandService::stepNotFound));
    }

    private void requirePositionFree(UUID routineId, long position, UUID stepId) {
        if (steps.positionTaken(routineId, position, stepId)) {
            throw RoutineValues.invalid("position", "INVALID_VALUE", "Vị trí đã có bước khác.");
        }
    }

    private <T extends SyncedEntity> void recordDeletes(UUID userId, String entityType, List<T> deleted) {
        for (T entity : deleted) {
            changeRecorder.record(userId, entityType, entity.getId(), ChangeOperation.DELETE, entity.getVersion(),
                    null);
        }
    }

    private static RuntimeException translate(DataIntegrityViolationException e, String pk) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.contains(pk)) {
                return new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            if (message != null && message.contains(POSITION_UNIQUE)) {
                return RoutineValues.invalid("position", "INVALID_VALUE", "Vị trí đã có bước khác.");
            }
        }
        return e;
    }

    static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.ROUTINE_NOT_FOUND);
    }

    static NotFoundException stepNotFound() {
        return new NotFoundException(ErrorCode.ROUTINE_STEP_NOT_FOUND);
    }

    static String hhmm(java.time.LocalTime time) {
        return time == null ? null : time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
