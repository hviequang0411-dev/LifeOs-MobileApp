package com.lifeos.habit.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.common.util.Patch;
import com.lifeos.habit.domain.HabitFields;
import com.lifeos.habit.domain.HabitSchedule;
import com.lifeos.habit.dto.HabitResponse;
import com.lifeos.habit.entity.Habit;
import com.lifeos.habit.entity.HabitCheckin;
import com.lifeos.habit.mapper.HabitMapper;
import com.lifeos.habit.repository.HabitCheckinRepository;
import com.lifeos.habit.repository.HabitRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi Habit (FR-HAB-01/02/09, D230, D236, D237). Thứ tự kiểm D96: tồn tại → version → giá trị → giới hạn → áp
 * dụng. Mỗi thay đổi thật gọi ChangeRecorder đúng một lần, SAU mọi khóa/flush nghiệp vụ (D86).
 */
@Service
public class HabitCommandService {

    /** D237: tối đa 200 habit mỗi người dùng (kể cả lưu trữ, trừ đã xóa). */
    static final int MAX_HABITS = 200;
    private static final String PK_CONSTRAINT = "pk_habits";

    private final HabitRepository habits;
    private final HabitCheckinRepository checkins;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final UserCalendar calendar;
    private final HabitMapper mapper;
    private final Clock clock;
    private final org.springframework.context.ApplicationEventPublisher events;

    public HabitCommandService(HabitRepository habits, HabitCheckinRepository checkins, ChangeRecorder changeRecorder,
            VersionedWrites versionedWrites, UserCalendar calendar, HabitMapper mapper, Clock clock,
            org.springframework.context.ApplicationEventPublisher events) {
        this.habits = habits;
        this.checkins = checkins;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.calendar = calendar;
        this.mapper = mapper;
        this.clock = clock;
        this.events = events;
    }

    public HabitResponse create(UUID userId, HabitCommands.Create cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (habits.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                HabitFields fields = fields(cmd.name().value(), cmd.description().value(), cmd.color().value(),
                        cmd.icon().value(), cmd.scheduleType().value(), cmd.weekdays().value(),
                        cmd.timesPerWeek().value(), cmd.intervalDays().value(),
                        startOrToday(userId, cmd.startOn().value()), cmd.endOn().value(),
                        cmd.reminderTime().value());
                if (habits.countLive(userId) >= MAX_HABITS) {
                    throw limitExceeded();
                }
                Habit saved = habits.saveAndFlush(Habit.create(cmd.id(), userId, fields));
                changeRecorder.record(userId, Habit.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (mentions(e, PK_CONSTRAINT)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    /** JSON Merge Patch; lịch là một bộ (D254, xem HabitCommands.Update). */
    public HabitResponse update(UUID userId, UUID habitId, HabitCommands.Update cmd, UUID mutationIdOrNull) {
        return writeExisting(userId, habitId, cmd.expectedVersion(), mutationIdOrNull, habit -> {
            HabitFields current = habit.fields();
            HabitSchedule s = current.schedule();
            boolean scheduleReplaced = cmd.scheduleType().present();
            HabitFields merged = fields(
                    pick(cmd.name(), current.name()),
                    pick(cmd.description(), current.description()),
                    pick(cmd.color(), current.color()),
                    pick(cmd.icon(), current.icon()),
                    pick(cmd.scheduleType(), s.type()),
                    scheduleReplaced ? cmd.weekdays().value() : pick(cmd.weekdays(), s.weekdays()),
                    scheduleReplaced ? cmd.timesPerWeek().value() : pick(cmd.timesPerWeek(), s.timesPerWeek()),
                    scheduleReplaced ? cmd.intervalDays().value() : pick(cmd.intervalDays(), s.intervalDays()),
                    cmd.startOn().present() ? requiredDate(cmd.startOn().value(), "startOn") : s.startOn(),
                    cmd.endOn().present() ? cmd.endOn().value() : format(s.endOn()),
                    cmd.reminderTime().present() ? cmd.reminderTime().value() : formatTime(current));
            return habit.apply(merged);
        });
    }

    public HabitResponse archive(UUID userId, UUID habitId, long expectedVersion) {
        return writeExisting(userId, habitId, expectedVersion, null, habit -> habit.archive(clock.instant()));
    }

    public HabitResponse unarchive(UUID userId, UUID habitId, long expectedVersion) {
        return writeExisting(userId, habitId, expectedVersion, null, Habit::unarchive);
    }

    /**
     * Xóa Habit (D236): khóa FOR UPDATE → kiểm version → xóa mềm + flush → xóa mềm MỌI check-in còn sống (tải hết theo
     * id, flush) → change_logs DELETE từng check-in (mutation_id NULL) → change_logs DELETE của Habit, sau cùng (D86).
     *
     * @return version tombstone của Habit
     */
    public long delete(UUID userId, UUID habitId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            Habit habit = habits.findActiveForUpdate(habitId, userId).orElseThrow(HabitCommandService::notFound);
            VersionedWrites.requireVersion(habit, expectedVersion);
            Instant now = clock.instant();
            habit.softDelete(now);
            Habit saved = habits.saveAndFlush(habit);
            List<HabitCheckin> children = checkins.findLiveByHabit(userId, habitId);
            children.forEach(c -> c.softDelete(now));
            List<HabitCheckin> deleted = checkins.saveAllAndFlush(children);
            // PHASE_4 D129: reminder của Habit bị xóa mềm trong cùng transaction (trước mọi change_logs).
            events.publishEvent(new com.lifeos.habit.api.HabitDeletionStarted(userId, habitId));
            for (HabitCheckin checkin : deleted) {
                changeRecorder.record(userId, HabitCheckin.ENTITY_TYPE, checkin.getId(), ChangeOperation.DELETE,
                        checkin.getVersion(), null);
            }
            changeRecorder.record(userId, Habit.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, habitId));
    }

    private HabitResponse writeExisting(UUID userId, UUID habitId, long expectedVersion, UUID mutationIdOrNull,
            Function<Habit, Boolean> change) {
        return versionedWrites.write(() -> {
            Habit habit = habits.findActive(habitId, userId).orElseThrow(HabitCommandService::notFound);
            VersionedWrites.requireVersion(habit, expectedVersion);
            if (!change.apply(habit)) {
                return mapper.toResponse(habit);
            }
            Habit saved = habits.saveAndFlush(habit);
            changeRecorder.record(userId, Habit.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, habitId));
    }

    private HabitFields fields(String name, String description, String color, String icon, String scheduleType,
            Integer weekdays, Integer timesPerWeek, Integer intervalDays, java.time.LocalDate startOn,
            String endOn, String reminderTime) {
        HabitSchedule schedule = HabitValues.schedule(scheduleType, weekdays, timesPerWeek, intervalDays, startOn,
                HabitValues.date(endOn, "endOn"));
        return new HabitFields(HabitValues.name(name),
                HabitValues.optionalText(description, "description", HabitValues.DESCRIPTION_MAX),
                HabitValues.color(color),
                HabitValues.optionalText(icon, "icon", HabitValues.ICON_MAX),
                schedule, HabitValues.time(reminderTime, "reminderTime"));
    }

    private java.time.LocalDate startOrToday(UUID userId, String raw) {
        java.time.LocalDate start = HabitValues.date(raw, "startOn");
        return start != null ? start : calendar.today(userId);
    }

    private static java.time.LocalDate requiredDate(String raw, String field) {
        java.time.LocalDate date = HabitValues.date(raw, field);
        if (date == null) {
            throw HabitValues.invalid(field, "REQUIRED", "Trường này không được để trống.");
        }
        return date;
    }

    private static <T> T pick(Patch<T> patch, T current) {
        return patch.present() ? patch.value() : current;
    }

    private static String format(java.time.LocalDate date) {
        return date == null ? null : date.toString();
    }

    private static String formatTime(HabitFields fields) {
        return fields.reminderTime() == null ? null
                : fields.reminderTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
    }

    private long currentVersion(UUID userId, UUID habitId) {
        return habits.findVersion(habitId, userId).orElseThrow(HabitCommandService::notFound);
    }

    static ValidationException limitExceeded() {
        return new ValidationException(ErrorCode.LIMIT_EXCEEDED, ErrorCode.LIMIT_EXCEEDED.title());
    }

    static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.HABIT_NOT_FOUND);
    }

    static boolean mentions(Throwable e, String constraint) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }
}
