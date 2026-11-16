package com.lifeos.routine.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NaturalKeyConflictException;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.routine.dto.LogResponse;
import com.lifeos.routine.dto.StepLogResponse;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lượt chạy routine và kết quả bước (FR-RTN-03, D242, D243, D249). Mọi thao tác khóa Routine FOR UPDATE trước, rồi
 * lượt (thứ tự khóa Routine → lượt → kết quả bước, cùng thứ tự với cascade xóa — D86), ghi, flush, và cuối cùng
 * ChangeRecorder: kết quả bước trước, lượt sau (khi completedAt đổi).
 */
@Service
public class RoutineLogCommandService {

    private final RoutineRepository routines;
    private final RoutineStepRepository steps;
    private final RoutineLogRepository logs;
    private final RoutineStepLogRepository stepLogs;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final RoutineMapper mapper;
    private final Clock clock;

    public RoutineLogCommandService(RoutineRepository routines, RoutineStepRepository steps, RoutineLogRepository logs,
            RoutineStepLogRepository stepLogs, ChangeRecorder changeRecorder, VersionedWrites versionedWrites,
            RoutineMapper mapper, Clock clock) {
        this.routines = routines;
        this.steps = steps;
        this.logs = logs;
        this.stepLogs = stepLogs;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Kết quả upsert: created = true nếu vừa tạo (201). */
    public record Started(LogResponse log, boolean created) {
    }

    /** Kết quả upsert bước. */
    public record Recorded(StepLogResponse stepLog, boolean created) {
    }

    // ---------- Lượt chạy

    /** PUT /routines/{id}/logs/{date}: Start idempotent — đã có lượt ngày đó thì trả lượt đó (totalSteps giữ nguyên). */
    public Started start(UUID userId, UUID routineId, String rawDate, Integer rawTotal, UUID clientId) {
        return versionedWrites.write(() -> {
            routines.findActiveForUpdate(routineId, userId).orElseThrow(RoutineCommandService::notFound);
            LocalDate date = RoutineValues.date(rawDate, "localDate");
            int total = RoutineValues.totalSteps(rawTotal);
            Optional<RoutineLog> existing = logs.findActiveOn(routineId, date, userId);
            if (existing.isPresent()) {
                return new Started(mapper.toResponse(existing.get()), false);
            }
            UUID id = clientId != null ? clientId : UUID.randomUUID();
            if (logs.existsById(id)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            RoutineLog saved = logs.saveAndFlush(RoutineLog.start(id, userId, routineId, date, total,
                    clock.instant()));
            changeRecorder.record(userId, RoutineLog.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                    saved.getVersion(), null);
            return new Started(mapper.toResponse(saved), true);
        }, () -> 0L);
    }

    /** CREATE lượt qua sync: id đã dùng → ENTITY_ALREADY_EXISTS; routine tham chiếu 422; trùng ngày → ALREADY_EXISTS. */
    public LogResponse startForSync(UUID userId, RoutineCommands.StartLog cmd, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (logs.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                UUID routineId = RoutineValues.requiredId(cmd.routineId().value(), "routineId");
                routines.findActiveForUpdate(routineId, userId)
                        .orElseThrow(() -> RoutineValues.reference(ErrorCode.ROUTINE_NOT_FOUND));
                LocalDate date = RoutineValues.date(cmd.localDate().value(), "localDate");
                int total = RoutineValues.totalSteps(cmd.totalSteps().value());
                Optional<RoutineLog> existing = logs.findActiveOn(routineId, date, userId);
                if (existing.isPresent()) {
                    throw new NaturalKeyConflictException(existing.get().getId());
                }
                RoutineLog saved = logs.saveAndFlush(RoutineLog.start(cmd.id(), userId, routineId, date, total,
                        clock.instant()));
                changeRecorder.record(userId, RoutineLog.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
        }
    }

    /** UPDATE lượt qua sync: mọi trường client gửi đều bất biến (routineId, localDate, totalSteps) → không đổi gì. */
    public LogResponse updateLogForSync(UUID userId, UUID logId, RoutineCommands.UpdateLog cmd) {
        return versionedWrites.write(() -> {
            RoutineLog log = logs.findActive(logId, userId).orElseThrow(RoutineLogCommandService::logNotFound);
            VersionedWrites.requireVersion(log, cmd.expectedVersion());
            RoutineValues.unchanged(cmd.routineId().present(), cmd.routineId().value(), log.getRoutineId(),
                    "routineId");
            if (cmd.localDate().present()) {
                RoutineValues.unchanged(true, RoutineValues.date(cmd.localDate().value(), "localDate"),
                        log.getLocalDate(), "localDate");
            }
            RoutineValues.unchanged(cmd.totalSteps().present(), cmd.totalSteps().value(), log.getTotalSteps(),
                    "totalSteps");
            return mapper.toResponse(log);
        }, () -> logs.findVersion(logId, userId).orElseThrow(RoutineLogCommandService::logNotFound));
    }

    /** DELETE lượt qua sync: tombstone lượt + kết quả bước (mutation_id NULL cho con). */
    public long deleteLogForSync(UUID userId, UUID logId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            RoutineLog log = logs.findActive(logId, userId).orElseThrow(RoutineLogCommandService::logNotFound);
            VersionedWrites.requireVersion(log, expectedVersion);
            routines.findActiveForUpdate(log.getRoutineId(), userId);
            Instant now = clock.instant();
            log.softDelete(now);
            RoutineLog saved = logs.saveAndFlush(log);
            List<RoutineStepLog> results = stepLogs.findLiveByLogs(userId, List.of(logId));
            results.forEach(r -> r.softDelete(now));
            for (RoutineStepLog r : stepLogs.saveAllAndFlush(results)) {
                changeRecorder.record(userId, RoutineStepLog.ENTITY_TYPE, r.getId(), ChangeOperation.DELETE,
                        r.getVersion(), null);
            }
            changeRecorder.record(userId, RoutineLog.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationId);
            return saved.getVersion();
        }, () -> logs.findVersion(logId, userId).orElseThrow(RoutineLogCommandService::logNotFound));
    }

    // ---------- Kết quả bước

    /** PUT /routine-logs/{logId}/steps/{stepId}: hoàn thành/bỏ qua (upsert); bước phải thuộc routine của lượt. */
    public Recorded record(UUID userId, UUID logId, UUID stepId, String rawStatus, UUID clientId) {
        return versionedWrites.write(() -> {
            RoutineLog log = lockLog(userId, logId, true);
            RoutineStep step = steps.findOwned(stepId, userId).filter(s -> s.getRoutineId().equals(log.getRoutineId()))
                    .orElseThrow(RoutineCommandService::stepNotFound);
            String status = RoutineValues.status(rawStatus);
            Optional<RoutineStepLog> existing = stepLogs.findActiveFor(logId, step.getId(), userId);
            RoutineStepLog saved;
            boolean created = existing.isEmpty();
            if (existing.isPresent()) {
                RoutineStepLog current = existing.get();
                if (!current.changeStatus(status)) {
                    return new Recorded(mapper.toResponse(current), false);
                }
                saved = stepLogs.saveAndFlush(current);
            } else {
                UUID id = clientId != null ? clientId : UUID.randomUUID();
                if (stepLogs.existsById(id)) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                saved = stepLogs.saveAndFlush(RoutineStepLog.create(id, userId, logId, step.getId(), status));
            }
            settleAndRecord(userId, log, saved, created ? ChangeOperation.CREATE : ChangeOperation.UPDATE, null);
            return new Recorded(mapper.toResponse(saved), created);
        }, () -> 0L);
    }

    /** DELETE /routine-logs/{logId}/steps/{stepId}: hoàn tác (idempotent). */
    public void undo(UUID userId, UUID logId, UUID stepId) {
        versionedWrites.write(() -> {
            RoutineLog log = lockLog(userId, logId, true);
            stepLogs.findActiveFor(logId, stepId, userId).ifPresent(result -> {
                result.softDelete(clock.instant());
                RoutineStepLog saved = stepLogs.saveAndFlush(result);
                settleAndRecord(userId, log, saved, ChangeOperation.DELETE, null);
            });
            return null;
        }, () -> 0L);
    }

    /** CREATE kết quả bước qua sync: tham chiếu lượt/bước 422; trùng (lượt, bước) → ALREADY_EXISTS (D235). */
    public StepLogResponse createStepLogForSync(UUID userId, RoutineCommands.CreateStepLog cmd, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (stepLogs.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                UUID logId = RoutineValues.requiredId(cmd.routineLogId().value(), "routineLogId");
                RoutineLog log = lockLog(userId, logId, false);
                UUID stepId = RoutineValues.requiredId(cmd.stepId().value(), "stepId");
                steps.findOwned(stepId, userId).filter(s -> s.getRoutineId().equals(log.getRoutineId()))
                        .orElseThrow(() -> RoutineValues.reference(ErrorCode.ROUTINE_STEP_NOT_FOUND));
                String status = RoutineValues.status(cmd.status().value());
                Optional<RoutineStepLog> existing = stepLogs.findActiveFor(logId, stepId, userId);
                if (existing.isPresent()) {
                    throw new NaturalKeyConflictException(existing.get().getId());
                }
                RoutineStepLog saved = stepLogs.saveAndFlush(RoutineStepLog.create(cmd.id(), userId, logId, stepId,
                        status));
                settleAndRecord(userId, log, saved, ChangeOperation.CREATE, mutationId);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
        }
    }

    /** UPDATE kết quả bước qua sync: chỉ status; routineLogId/stepId bất biến. */
    public StepLogResponse updateStepLogForSync(UUID userId, UUID stepLogId, RoutineCommands.UpdateStepLog cmd,
            UUID mutationId) {
        return versionedWrites.write(() -> {
            RoutineStepLog result = stepLogs.findActive(stepLogId, userId)
                    .orElseThrow(RoutineLogCommandService::stepLogNotFound);
            VersionedWrites.requireVersion(result, cmd.expectedVersion());
            RoutineLog log = lockLog(userId, result.getRoutineLogId(), false);
            RoutineValues.unchanged(cmd.routineLogId().present(), cmd.routineLogId().value(),
                    result.getRoutineLogId(), "routineLogId");
            RoutineValues.unchanged(cmd.stepId().present(), cmd.stepId().value(), result.getStepId(), "stepId");
            String status = cmd.status().present() ? RoutineValues.status(cmd.status().value()) : result.getStatus();
            if (!result.changeStatus(status)) {
                return mapper.toResponse(result);
            }
            RoutineStepLog saved = stepLogs.saveAndFlush(result);
            settleAndRecord(userId, log, saved, ChangeOperation.UPDATE, mutationId);
            return mapper.toResponse(saved);
        }, () -> stepLogs.findVersion(stepLogId, userId).orElseThrow(RoutineLogCommandService::stepLogNotFound));
    }

    /** DELETE kết quả bước qua sync (hoàn tác). */
    public long deleteStepLogForSync(UUID userId, UUID stepLogId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            RoutineStepLog result = stepLogs.findActive(stepLogId, userId)
                    .orElseThrow(RoutineLogCommandService::stepLogNotFound);
            VersionedWrites.requireVersion(result, expectedVersion);
            RoutineLog log = lockLog(userId, result.getRoutineLogId(), false);
            result.softDelete(clock.instant());
            RoutineStepLog saved = stepLogs.saveAndFlush(result);
            settleAndRecord(userId, log, saved, ChangeOperation.DELETE, mutationId);
            return saved.getVersion();
        }, () -> stepLogs.findVersion(stepLogId, userId).orElseThrow(RoutineLogCommandService::stepLogNotFound));
    }

    /**
     * Khóa Routine rồi lượt (FOR UPDATE). Lượt là entity đích của URL (REST) → 404; là tham chiếu (sync) → 422.
     */
    private RoutineLog lockLog(UUID userId, UUID logId, boolean fromUrl) {
        RoutineLog unlocked = logs.findActive(logId, userId).orElseThrow(() -> fromUrl ? logNotFound()
                : RoutineValues.reference(ErrorCode.ROUTINE_LOG_NOT_FOUND));
        Optional<Routine> routine = routines.findActiveForUpdate(unlocked.getRoutineId(), userId);
        if (routine.isEmpty()) {
            throw fromUrl ? logNotFound() : RoutineValues.reference(ErrorCode.ROUTINE_LOG_NOT_FOUND);
        }
        return logs.findActiveForUpdate(logId, userId).orElseThrow(() -> fromUrl ? logNotFound()
                : RoutineValues.reference(ErrorCode.ROUTINE_LOG_NOT_FOUND));
    }

    /**
     * Tính lại completedAt của lượt (D242: done + skipped còn sống ≥ totalSteps) sau khi kết quả bước đã flush; ghi
     * change_logs kết quả bước rồi (nếu completedAt đổi) của lượt — sau mọi flush (D86).
     */
    private void settleAndRecord(UUID userId, RoutineLog log, RoutineStepLog changed, ChangeOperation operation,
            UUID mutationId) {
        boolean logChanged = log.settle((int) stepLogs.countLive(log.getId()), clock.instant());
        RoutineLog savedLog = logChanged ? logs.saveAndFlush(log) : log;
        changeRecorder.record(userId, RoutineStepLog.ENTITY_TYPE, changed.getId(), operation, changed.getVersion(),
                mutationId);
        if (logChanged) {
            changeRecorder.record(userId, RoutineLog.ENTITY_TYPE, savedLog.getId(), ChangeOperation.UPDATE,
                    savedLog.getVersion(), null);
        }
    }

    static NotFoundException logNotFound() {
        return new NotFoundException(ErrorCode.ROUTINE_LOG_NOT_FOUND);
    }

    static NotFoundException stepLogNotFound() {
        return new NotFoundException(ErrorCode.ROUTINE_STEP_NOT_FOUND);
    }
}
