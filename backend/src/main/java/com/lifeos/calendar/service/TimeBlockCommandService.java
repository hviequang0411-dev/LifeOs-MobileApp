package com.lifeos.calendar.service;

import com.lifeos.calendar.dto.TimeBlockResponse;
import com.lifeos.calendar.entity.TimeBlock;
import com.lifeos.calendar.mapper.CalendarMapper;
import com.lifeos.calendar.repository.TimeBlockRepository;
import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.task.api.TaskApi;
import com.lifeos.task.api.TaskDeletionStarted;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khối thời gian của Task (FR-CAL-03, D128): ≤ 24 giờ, end sau start, không chặn trùng. Task tham chiếu khóa FOR SHARE
 * (TaskApi) nên không mồ côi khi Task bị xóa đồng thời; xóa Task cascade xóa mềm các block (listener cùng transaction).
 */
@Service
public class TimeBlockCommandService {

    static final Duration MAX = Duration.ofHours(24);
    private static final String PK = "pk_time_blocks";

    private final TimeBlockRepository blocks;
    private final TaskApi tasks;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final CalendarMapper mapper;
    private final Clock clock;

    public TimeBlockCommandService(TimeBlockRepository blocks, TaskApi tasks, ChangeRecorder changeRecorder,
            VersionedWrites versionedWrites, CalendarMapper mapper, Clock clock) {
        this.blocks = blocks;
        this.tasks = tasks;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
    }

    public TimeBlockResponse create(UUID userId, CalendarCommands.CreateTimeBlock cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (blocks.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                UUID taskId = cmd.taskId().value();
                if (taskId == null) {
                    throw CalendarValues.invalid("taskId", "REQUIRED", "taskId là bắt buộc.");
                }
                Instant start = CalendarValues.instant(cmd.startAt().value(), "startAt");
                Instant end = CalendarValues.instant(cmd.endAt().value(), "endAt");
                range(start, end);
                String note = CalendarValues.optionalText(cmd.note().value(), "note", CalendarValues.NOTE_MAX);
                tasks.lockForShare(userId, taskId)
                        .orElseThrow(() -> CalendarValues.badReference(ErrorCode.TASK_NOT_FOUND));
                TimeBlock block = TimeBlock.create(cmd.id(), userId, taskId);
                block.setStartAt(start);
                block.setEndAt(end);
                block.setNote(note);
                TimeBlock saved = blocks.saveAndFlush(block);
                changeRecorder.record(userId, TimeBlock.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (EventCommandService.mentions(e, PK)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    public TimeBlockResponse update(UUID userId, UUID blockId, CalendarCommands.UpdateTimeBlock cmd,
            UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            TimeBlock block = blocks.findActive(blockId, userId)
                    .orElseThrow(() -> CalendarValues.notFound(ErrorCode.TIME_BLOCK_NOT_FOUND));
            VersionedWrites.requireVersion(block, cmd.expectedVersion());
            if (cmd.taskId().present() && !block.getTaskId().equals(cmd.taskId().value())) {
                throw CalendarValues.invalid("taskId", "IMMUTABLE", "Không thể thay đổi trường này sau khi tạo.");
            }
            Instant start = cmd.startAt().present() ? CalendarValues.instant(cmd.startAt().value(), "startAt")
                    : block.getStartAt();
            Instant end = cmd.endAt().present() ? CalendarValues.instant(cmd.endAt().value(), "endAt")
                    : block.getEndAt();
            range(start, end);
            String note = cmd.note().present()
                    ? CalendarValues.optionalText(cmd.note().value(), "note", CalendarValues.NOTE_MAX) : block.getNote();
            boolean changed = block.setStartAt(start);
            changed |= block.setEndAt(end);
            changed |= block.setNote(note);
            if (!changed) {
                return mapper.toResponse(block);
            }
            TimeBlock saved = blocks.saveAndFlush(block);
            changeRecorder.record(userId, TimeBlock.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, blockId));
    }

    public long delete(UUID userId, UUID blockId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            TimeBlock block = blocks.findActive(blockId, userId)
                    .orElseThrow(() -> CalendarValues.notFound(ErrorCode.TIME_BLOCK_NOT_FOUND));
            VersionedWrites.requireVersion(block, expectedVersion);
            block.softDelete(clock.instant());
            TimeBlock saved = blocks.saveAndFlush(block);
            changeRecorder.record(userId, TimeBlock.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, blockId));
    }

    /**
     * Cascade khi xóa Task (D128): chạy ĐỒNG BỘ trong transaction xóa Task (Task đã xóa mềm + flush), tải block theo id,
     * xóa mềm + flush, rồi gom change_logs vào sự kiện (TaskCommandService ghi sau mọi listener, D86).
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onTaskDeletion(TaskDeletionStarted event) {
        Instant now = clock.instant();
        List<TimeBlock> children = blocks.findLiveByTask(event.userId(), event.taskId());
        if (children.isEmpty()) {
            return;
        }
        children.forEach(b -> b.softDelete(now));
        for (TimeBlock b : blocks.saveAllAndFlush(children)) {
            event.changes().add(TimeBlock.ENTITY_TYPE, b.getId(), ChangeOperation.DELETE, b.getVersion());
        }
    }

    private static void range(Instant start, Instant end) {
        if (!end.isAfter(start) || Duration.between(start, end).compareTo(MAX) > 0) {
            throw CalendarValues.invalid("endAt", "INVALID_VALUE", "endAt sau startAt và tối đa 24 giờ.");
        }
    }

    private long currentVersion(UUID userId, UUID blockId) {
        return blocks.findVersion(blockId, userId)
                .orElseThrow(() -> CalendarValues.notFound(ErrorCode.TIME_BLOCK_NOT_FOUND));
    }
}
