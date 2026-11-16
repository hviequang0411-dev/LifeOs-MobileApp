package com.lifeos.habit.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NaturalKeyConflictException;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.time.UserCalendar;
import com.lifeos.common.util.Patch;
import com.lifeos.habit.dto.CheckinResponse;
import com.lifeos.habit.entity.Habit;
import com.lifeos.habit.entity.HabitCheckin;
import com.lifeos.habit.mapper.HabitMapper;
import com.lifeos.habit.repository.HabitCheckinRepository;
import com.lifeos.habit.repository.HabitRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi check-in (FR-HAB-03..05, D231, D232, D235). Mọi thao tác khóa dòng Habit FOR UPDATE trước (các check-in của
 * một habit tuần tự: upsert theo ngày không đua nhau, không có check-in mồ côi khi habit bị xóa đồng thời), rồi mới
 * ghi check-in, flush, và cuối cùng ChangeRecorder (D86).
 */
@Service
public class CheckinCommandService {

    private static final String PK_CONSTRAINT = "pk_habit_checkins";

    private final HabitRepository habits;
    private final HabitCheckinRepository checkins;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final UserCalendar calendar;
    private final HabitMapper mapper;
    private final Clock clock;

    public CheckinCommandService(HabitRepository habits, HabitCheckinRepository checkins,
            ChangeRecorder changeRecorder, VersionedWrites versionedWrites, UserCalendar calendar, HabitMapper mapper,
            Clock clock) {
        this.habits = habits;
        this.checkins = checkins;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.calendar = calendar;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Kết quả upsert: created = true nếu vừa tạo dòng mới (201), false nếu cập nhật/không đổi (200). */
    public record Upserted(CheckinResponse checkin, boolean created) {
    }

    /**
     * PUT /habits/{id}/checkins/{date}: upsert idempotent theo (habit, ngày). Habit đích không có → 404; ngày ngoài
     * khoảng (D231) → 422. clientId dùng khi tạo mới; đã bị bản ghi khác dùng → 409 ENTITY_ALREADY_EXISTS.
     */
    public Upserted upsert(UUID userId, UUID habitId, String rawDate, String rawStatus, String rawNote,
            UUID clientId) {
        return versionedWrites.write(() -> {
            Habit habit = habits.findActiveForUpdate(habitId, userId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.HABIT_NOT_FOUND));
            LocalDate date = checkDate(userId, habit, requiredDate(rawDate));
            String status = HabitValues.status(rawStatus);
            String note = HabitValues.optionalText(rawNote, "note", HabitValues.NOTE_MAX);
            Optional<HabitCheckin> existing = checkins.findActiveOn(habitId, date, userId);
            if (existing.isPresent()) {
                HabitCheckin checkin = existing.get();
                if (!checkin.update(status, note)) {
                    return new Upserted(mapper.toResponse(checkin), false);
                }
                HabitCheckin saved = checkins.saveAndFlush(checkin);
                changeRecorder.record(userId, HabitCheckin.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                        saved.getVersion(), null);
                return new Upserted(mapper.toResponse(saved), false);
            }
            UUID id = clientId != null ? clientId : UUID.randomUUID();
            if (checkins.existsById(id)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            HabitCheckin saved = checkins.saveAndFlush(
                    HabitCheckin.create(id, userId, habitId, date, status, note));
            changeRecorder.record(userId, HabitCheckin.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                    saved.getVersion(), null);
            return new Upserted(mapper.toResponse(saved), true);
        }, () -> 0L);
    }

    /** DELETE /habits/{id}/checkins/{date}: hoàn tác (xóa mềm); không có check-in ngày đó → không làm gì (204). */
    public void undo(UUID userId, UUID habitId, String rawDate) {
        versionedWrites.write(() -> {
            habits.findActiveForUpdate(habitId, userId)
                    .orElseThrow(() -> new NotFoundException(ErrorCode.HABIT_NOT_FOUND));
            LocalDate date = requiredDate(rawDate);
            checkins.findActiveOn(habitId, date, userId).ifPresent(checkin -> softDelete(userId, checkin, null));
            return null;
        }, () -> 0L);
    }

    /**
     * CREATE qua sync. Thứ tự: id đã dùng → 409/REJECTED ENTITY_ALREADY_EXISTS; Habit tham chiếu không có → 422
     * HABIT_NOT_FOUND (tham chiếu, không phải entity đích); giá trị; ngày (D231); đã có check-in hoạt động cùng
     * (habit, ngày) → CONFLICT ALREADY_EXISTS kèm bản đang có (D235).
     */
    public CheckinResponse createForSync(UUID userId, HabitCommands.CreateCheckin cmd, UUID mutationId) {
        try {
            return versionedWrites.write(() -> {
                if (checkins.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                UUID habitId = cmd.habitId().value();
                if (habitId == null) {
                    throw HabitValues.invalid("habitId", "REQUIRED", "habitId là bắt buộc.");
                }
                Habit habit = habits.findActiveForUpdate(habitId, userId).orElseThrow(
                        () -> new ValidationException(ErrorCode.HABIT_NOT_FOUND, ErrorCode.HABIT_NOT_FOUND.title()));
                LocalDate date = checkDate(userId, habit, requiredDate(cmd.localDate().value()));
                String status = HabitValues.status(cmd.status().value());
                String note = HabitValues.optionalText(cmd.note().value(), "note", HabitValues.NOTE_MAX);
                Optional<HabitCheckin> existing = checkins.findActiveOn(habitId, date, userId);
                if (existing.isPresent()) {
                    throw new NaturalKeyConflictException(existing.get().getId());
                }
                HabitCheckin saved = checkins.saveAndFlush(
                        HabitCheckin.create(cmd.id(), userId, habitId, date, status, note));
                changeRecorder.record(userId, HabitCheckin.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationId);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (HabitCommandService.mentions(e, PK_CONSTRAINT)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    /** UPDATE qua sync: đổi status/note; habitId, localDate bất biến (đổi → VALIDATION_FAILED). */
    public CheckinResponse updateForSync(UUID userId, UUID checkinId, HabitCommands.UpdateCheckin cmd,
            UUID mutationId) {
        return versionedWrites.write(() -> {
            HabitCheckin checkin = checkins.findActive(checkinId, userId).orElseThrow(CheckinCommandService::notFound);
            VersionedWrites.requireVersion(checkin, cmd.expectedVersion());
            habits.findActiveForUpdate(checkin.getHabitId(), userId);
            requireUnchanged(cmd.habitId(), checkin.getHabitId(), "habitId");
            if (cmd.localDate().present() && !checkin.getLocalDate().equals(requiredDate(cmd.localDate().value()))) {
                throw HabitValues.invalid("localDate", "INVALID_VALUE", "localDate không đổi được.");
            }
            String status = cmd.status().present() ? HabitValues.status(cmd.status().value()) : checkin.getStatus();
            String note = cmd.note().present()
                    ? HabitValues.optionalText(cmd.note().value(), "note", HabitValues.NOTE_MAX) : checkin.getNote();
            if (!checkin.update(status, note)) {
                return mapper.toResponse(checkin);
            }
            HabitCheckin saved = checkins.saveAndFlush(checkin);
            changeRecorder.record(userId, HabitCheckin.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationId);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, checkinId));
    }

    /** DELETE qua sync (hoàn tác). @return version tombstone */
    public long deleteForSync(UUID userId, UUID checkinId, long expectedVersion, UUID mutationId) {
        return versionedWrites.write(() -> {
            HabitCheckin checkin = checkins.findActive(checkinId, userId).orElseThrow(CheckinCommandService::notFound);
            VersionedWrites.requireVersion(checkin, expectedVersion);
            habits.findActiveForUpdate(checkin.getHabitId(), userId);
            return softDelete(userId, checkin, mutationId);
        }, () -> currentVersion(userId, checkinId));
    }

    private long softDelete(UUID userId, HabitCheckin checkin, UUID mutationId) {
        checkin.softDelete(clock.instant());
        HabitCheckin saved = checkins.saveAndFlush(checkin);
        changeRecorder.record(userId, HabitCheckin.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                saved.getVersion(), mutationId);
        return saved.getVersion();
    }

    /** D231: startOn ≤ ngày ≤ endOn (nếu có) và ≤ hôm nay + 1 theo múi giờ người dùng. */
    private LocalDate checkDate(UUID userId, Habit habit, LocalDate date) {
        LocalDate latest = calendar.today(userId).plusDays(1);
        if (date.isBefore(habit.getStartOn()) || (habit.getEndOn() != null && date.isAfter(habit.getEndOn()))
                || date.isAfter(latest)) {
            throw new ValidationException(ErrorCode.HABIT_DATE_OUT_OF_RANGE,
                    ErrorCode.HABIT_DATE_OUT_OF_RANGE.title());
        }
        return date;
    }

    private static LocalDate requiredDate(String raw) {
        LocalDate date = HabitValues.date(raw, "localDate");
        if (date == null) {
            throw HabitValues.invalid("localDate", "REQUIRED", "localDate là bắt buộc.");
        }
        return date;
    }

    private static void requireUnchanged(Patch<UUID> patch, UUID current, String field) {
        if (patch.present() && !current.equals(patch.value())) {
            throw HabitValues.invalid(field, "INVALID_VALUE", field + " không đổi được.");
        }
    }

    private long currentVersion(UUID userId, UUID checkinId) {
        return checkins.findVersion(checkinId, userId).orElseThrow(CheckinCommandService::notFound);
    }

    private static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.HABIT_CHECKIN_NOT_FOUND);
    }
}
