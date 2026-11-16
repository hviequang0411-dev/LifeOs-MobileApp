package com.lifeos.notification.service;

import com.lifeos.calendar.api.CalendarApi;
import com.lifeos.calendar.api.EventDeletionStarted;
import com.lifeos.calendar.api.EventScheduleChanged;
import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.util.Patch;
import com.lifeos.habit.api.HabitApi;
import com.lifeos.habit.api.HabitDeletionStarted;
import com.lifeos.identity.api.UserTimezoneChanged;
import com.lifeos.notification.dto.ReminderResponse;
import com.lifeos.notification.entity.Reminder;
import com.lifeos.notification.mapper.NotificationJson;
import com.lifeos.notification.mapper.ReminderMapper;
import com.lifeos.notification.repository.ReminderRepository;
import com.lifeos.routine.api.RoutineApi;
import com.lifeos.routine.api.RoutineDeletionStarted;
import com.lifeos.task.api.TaskApi;
import com.lifeos.task.api.TaskDeletionStarted;
import com.lifeos.task.api.TaskScheduleChanged;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh ghi reminder (FR-NTF-01/02, D129–D134, D146) và cascade từ module khác. targetType/targetId bất biến; mục tiêu
 * khóa FOR SHARE qua API công khai của module sở hữu (không mồ côi khi bị xóa đồng thời). nextFireAt/lastFiredAt/DONE
 * do server (mass assignment bị bỏ qua). Tối đa 10 reminder mỗi mục tiêu, 2000 reminder đang hoạt động mỗi người dùng.
 */
@Service
public class ReminderCommandService {

    static final int MAX_PER_TARGET = 10;
    static final int MAX_ACTIVE = 2000;
    static final int OFFSET_MAX = 525_600;
    private static final Set<String> TARGET_TYPES = Set.of("TASK", "EVENT", "HABIT", "ROUTINE", "NONE");
    private static final Set<String> MODES = Set.of("OFFSET", "LOCAL_AT");
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String PK = "pk_reminders";

    public record Fields(Patch<String> mode, Patch<Integer> offsetMinutes, Patch<String> localTime,
            Patch<String> localDate, Patch<Map<String, Object>> repeatRule, Patch<Boolean> urgent,
            Patch<Boolean> paused) {
        @Override
        public String toString() {
            return "ReminderFields[]";
        }
    }

    private final ReminderRepository reminders;
    private final ReminderSchedules schedules;
    private final TaskApi tasks;
    private final CalendarApi calendarApi;
    private final HabitApi habits;
    private final RoutineApi routines;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final ReminderMapper mapper;
    private final NotificationJson json;
    private final Clock clock;

    public ReminderCommandService(ReminderRepository reminders, ReminderSchedules schedules, TaskApi tasks,
            CalendarApi calendarApi, HabitApi habits, RoutineApi routines, ChangeRecorder changeRecorder,
            VersionedWrites versionedWrites, ReminderMapper mapper, NotificationJson json, Clock clock) {
        this.reminders = reminders;
        this.schedules = schedules;
        this.tasks = tasks;
        this.calendarApi = calendarApi;
        this.habits = habits;
        this.routines = routines;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.json = json;
        this.clock = clock;
    }

    public ReminderResponse create(UUID userId, UUID id, Patch<String> rawTargetType, Patch<UUID> rawTargetId,
            Fields f, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (reminders.existsById(id)) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String targetType = rawTargetType.value();
                if (targetType == null || !TARGET_TYPES.contains(targetType)) {
                    throw invalid("targetType", targetType == null ? "REQUIRED" : "INVALID_VALUE",
                            "targetType là TASK, EVENT, HABIT, ROUTINE hoặc NONE.");
                }
                UUID targetId = rawTargetId.value();
                if ("NONE".equals(targetType) != (targetId == null)) {
                    throw invalid("targetId", "INVALID_VALUE", "targetId bắt buộc trừ khi targetType là NONE.");
                }
                Reminder r = Reminder.create(id, userId, targetType, targetId);
                applyFields(r, f, true);
                lockTarget(userId, targetType, targetId);
                if (targetId != null && reminders.countLiveByTarget(userId, targetType, targetId) >= MAX_PER_TARGET) {
                    throw limitExceeded();
                }
                if (reminders.countActive(userId) >= MAX_ACTIVE) {
                    throw limitExceeded();
                }
                r.setNextFireAt(schedules.next(r, clock.instant()).orElse(null));
                Reminder saved = reminders.saveAndFlush(r);
                changeRecorder.record(userId, Reminder.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            for (Throwable t = e; t != null; t = t.getCause()) {
                if (t.getMessage() != null && t.getMessage().contains(PK)) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
            }
            throw e;
        }
    }

    public ReminderResponse update(UUID userId, UUID reminderId, long expectedVersion, Patch<String> targetType,
            Patch<UUID> targetId, Fields f, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            Reminder r = reminders.findActiveForUpdate(reminderId, userId).orElseThrow(ReminderCommandService::notFound);
            VersionedWrites.requireVersion(r, expectedVersion);
            if (targetType.present() && !Objects.equals(targetType.value(), r.getTargetType())) {
                throw invalid("targetType", "IMMUTABLE", "Không thể thay đổi trường này sau khi tạo.");
            }
            if (targetId.present() && !Objects.equals(targetId.value(), r.getTargetId())) {
                throw invalid("targetId", "IMMUTABLE", "Không thể thay đổi trường này sau khi tạo.");
            }
            String oldStatus = r.getStatus();
            boolean changed = applyFields(r, f, false);
            if ("ACTIVE".equals(r.getStatus()) && !"ACTIVE".equals(oldStatus)
                    && reminders.countActive(userId) >= MAX_ACTIVE) {
                throw limitExceeded();
            }
            changed |= r.setNextFireAt(schedules.next(r, clock.instant()).orElse(null));
            if (!changed) {
                return mapper.toResponse(r);
            }
            Reminder saved = reminders.saveAndFlush(r);
            changeRecorder.record(userId, Reminder.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, reminderId));
    }

    public ReminderResponse pause(UUID userId, UUID reminderId, long expectedVersion) {
        return update(userId, reminderId, expectedVersion, Patch.absent(), Patch.absent(), pausedOnly(true), null);
    }

    public ReminderResponse resume(UUID userId, UUID reminderId, long expectedVersion) {
        return update(userId, reminderId, expectedVersion, Patch.absent(), Patch.absent(), pausedOnly(false), null);
    }

    public long delete(UUID userId, UUID reminderId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            Reminder r = reminders.findActiveForUpdate(reminderId, userId).orElseThrow(ReminderCommandService::notFound);
            VersionedWrites.requireVersion(r, expectedVersion);
            r.softDelete(clock.instant());
            Reminder saved = reminders.saveAndFlush(r);
            changeRecorder.record(userId, Reminder.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, reminderId));
    }

    // ---------------------------------------------------------------- cascade (cùng transaction với module nguồn)

    /** Xóa Task (D129): nhiều listener → chỉ GOM change_logs, TaskCommandService ghi sau cùng (D86). */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onTaskDeletion(TaskDeletionStarted event) {
        for (Reminder r : softDeleteTarget(event.userId(), "TASK", event.taskId())) {
            event.changes().add(Reminder.ENTITY_TYPE, r.getId(), ChangeOperation.DELETE, r.getVersion());
        }
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onEventDeletion(EventDeletionStarted event) {
        recordDeletes(event.userId(), softDeleteTarget(event.userId(), "EVENT", event.eventId()));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onHabitDeletion(HabitDeletionStarted event) {
        recordDeletes(event.userId(), softDeleteTarget(event.userId(), "HABIT", event.habitId()));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onRoutineDeletion(RoutineDeletionStarted event) {
        recordDeletes(event.userId(), softDeleteTarget(event.userId(), "ROUTINE", event.routineId()));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onTaskScheduleChanged(TaskScheduleChanged event) {
        reschedule(event.userId(), reminders.findLiveByTarget(event.userId(), "TASK", event.taskId()));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onEventScheduleChanged(EventScheduleChanged event) {
        reschedule(event.userId(), reminders.findLiveByTarget(event.userId(), "EVENT", event.eventId()));
    }

    /** D134: đổi múi giờ → tính lại reminder theo giờ địa phương (LOCAL_AT) và theo hạn Task. */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onTimezoneChanged(UserTimezoneChanged event) {
        List<Reminder> affected = new java.util.ArrayList<>(reminders.findLocalAtActive(event.userId()));
        reschedule(event.userId(), affected);
    }

    private List<Reminder> softDeleteTarget(UUID userId, String type, UUID targetId) {
        Instant now = clock.instant();
        List<Reminder> live = reminders.findLiveByTarget(userId, type, targetId);
        if (live.isEmpty()) {
            return List.of();
        }
        live.forEach(r -> r.softDelete(now));
        return reminders.saveAllAndFlush(live);
    }

    private void recordDeletes(UUID userId, List<Reminder> deleted) {
        for (Reminder r : deleted) {
            changeRecorder.record(userId, Reminder.ENTITY_TYPE, r.getId(), ChangeOperation.DELETE, r.getVersion(),
                    null);
        }
    }

    /** Tính lại next_fire_at; chỉ dòng đổi mới tăng version và ghi change_logs (mutation_id NULL), sau flush (D86). */
    private void reschedule(UUID userId, List<Reminder> candidates) {
        Instant now = clock.instant();
        List<Reminder> changed = new java.util.ArrayList<>();
        for (Reminder r : candidates) {
            if ("ACTIVE".equals(r.getStatus()) && r.setNextFireAt(schedules.next(r, now).orElse(null))) {
                changed.add(r);
            }
        }
        if (changed.isEmpty()) {
            return;
        }
        for (Reminder r : reminders.saveAllAndFlush(changed)) {
            changeRecorder.record(userId, Reminder.ENTITY_TYPE, r.getId(), ChangeOperation.UPDATE, r.getVersion(),
                    null);
        }
    }

    // ---------------------------------------------------------------- kiểm giá trị

    /** @return true nếu có giá trị đổi */
    private boolean applyFields(Reminder r, Fields f, boolean creating) {
        String mode = f.mode().present() ? f.mode().value() : r.getMode();
        if (mode == null || !MODES.contains(mode)) {
            throw invalid("mode", mode == null ? "REQUIRED" : "INVALID_VALUE", "mode là OFFSET hoặc LOCAL_AT.");
        }
        boolean modeChanged = !creating && !mode.equals(r.getMode());
        Integer offset = f.offsetMinutes().present() ? f.offsetMinutes().value()
                : modeChanged ? null : r.getOffsetMinutes();
        LocalTime time = f.localTime().present() ? time(f.localTime().value())
                : modeChanged ? null : r.getLocalTime();
        LocalDate date = f.localDate().present() ? date(f.localDate().value()) : modeChanged ? null : r.getLocalDate();
        Map<String, Object> rule = f.repeatRule().present() ? f.repeatRule().value()
                : modeChanged ? null : json.toMap(r.getRepeatRule());
        if ("OFFSET".equals(mode)) {
            if (!"TASK".equals(r.getTargetType()) && !"EVENT".equals(r.getTargetType())) {
                throw invalid("mode", "INVALID_VALUE", "OFFSET chỉ dùng cho Task hoặc Event.");
            }
            if (offset == null || offset < 0 || offset > OFFSET_MAX) {
                throw invalid("offsetMinutes", offset == null ? "REQUIRED" : "OUT_OF_RANGE",
                        "offsetMinutes từ 0 đến " + OFFSET_MAX + ".");
            }
            if (time != null || date != null || rule != null) {
                throw invalid("localTime", "INVALID_VALUE", "OFFSET không dùng localTime/localDate/repeatRule.");
            }
        } else {
            if (time == null) {
                throw invalid("localTime", "REQUIRED", "LOCAL_AT cần localTime (HH:mm).");
            }
            if (offset != null) {
                throw invalid("offsetMinutes", "INVALID_VALUE", "LOCAL_AT không dùng offsetMinutes.");
            }
            if (rule != null) {
                if (date == null) {
                    throw invalid("localDate", "REQUIRED", "repeatRule cần localDate (ngày bắt đầu).");
                }
                calendarApi.validateRule(rule);
            }
        }
        boolean urgent = f.urgent().present() ? Boolean.TRUE.equals(f.urgent().value()) : r.isUrgent();
        String status = r.getStatus();
        if (f.paused().present() && f.paused().value() != null) {
            if (f.paused().value()) {
                status = "PAUSED";
            } else if ("PAUSED".equals(status)) {
                status = "ACTIVE";
            }
        }
        if (!creating && "DONE".equals(r.getStatus()) && (f.mode().present() || f.offsetMinutes().present()
                || f.localTime().present() || f.localDate().present() || f.repeatRule().present())) {
            status = "ACTIVE";
        }
        boolean changed = r.setMode(mode);
        changed |= r.setOffsetMinutes(offset);
        changed |= r.setLocalTime(time);
        changed |= r.setLocalDate(date);
        String ruleJson = rule == null ? null : json.toJson(rule);
        changed |= !Objects.equals(json.toMap(r.getRepeatRule()), rule) && r.setRepeatRule(ruleJson);
        changed |= r.setUrgent(urgent);
        changed |= r.setStatus(status);
        return changed;
    }

    private void lockTarget(UUID userId, String type, UUID targetId) {
        boolean found = switch (type) {
            case "TASK" -> tasks.lockForShare(userId, targetId).isPresent();
            case "EVENT" -> calendarApi.lockForShare(userId, targetId);
            case "HABIT" -> habits.lockForShare(userId, targetId);
            case "ROUTINE" -> routines.lockForShare(userId, targetId);
            default -> true;
        };
        if (!found) {
            throw new ValidationException(ErrorCode.REMINDER_TARGET_NOT_FOUND,
                    ErrorCode.REMINDER_TARGET_NOT_FOUND.title());
        }
    }

    private static Fields pausedOnly(boolean paused) {
        return new Fields(Patch.absent(), Patch.absent(), Patch.absent(), Patch.absent(), Patch.absent(),
                Patch.absent(), Patch.of(paused));
    }

    private static LocalTime time(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalTime.parse(raw, HH_MM);
        } catch (DateTimeParseException e) {
            throw invalid("localTime", "PATTERN", "Giờ theo dạng HH:mm.");
        }
    }

    private static LocalDate date(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw invalid("localDate", "PATTERN", "Ngày theo dạng yyyy-MM-dd.");
        }
    }

    private static ValidationException invalid(String field, String code, String message) {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, code, message);
    }

    private static ValidationException limitExceeded() {
        return new ValidationException(ErrorCode.LIMIT_EXCEEDED, ErrorCode.LIMIT_EXCEEDED.title());
    }

    static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.REMINDER_NOT_FOUND);
    }

    private long currentVersion(UUID userId, UUID reminderId) {
        return reminders.findVersion(reminderId, userId).orElseThrow(ReminderCommandService::notFound);
    }
}
