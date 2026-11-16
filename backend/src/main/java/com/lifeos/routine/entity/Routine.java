package com.lifeos.routine.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/** Routine (FR-RTN-01, D240): DAILY mọi ngày, WEEKLY theo bitmask thứ (như D230). Giá trị đã kiểm ở service. */
@Entity
@Table(name = "routines")
public class Routine extends SyncedEntity {

    public static final String ENTITY_TYPE = "routine";
    public static final String DAILY = "DAILY";
    public static final String WEEKLY = "WEEKLY";

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 6)
    private String kind;

    private Short weekdays;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Routine() {
    }

    private Routine(UUID id, UUID userId) {
        super(id, userId);
    }

    public static Routine create(UUID id, UUID userId, String name, String kind, Integer weekdays,
            LocalTime reminderTime) {
        Routine routine = new Routine(id, userId);
        routine.apply(name, kind, weekdays, reminderTime);
        return routine;
    }

    /** @return true nếu có giá trị đổi */
    public boolean apply(String newName, String newKind, Integer newWeekdays, LocalTime newReminderTime) {
        boolean changed = !Objects.equals(name, newName) || !Objects.equals(kind, newKind)
                || !Objects.equals(getWeekdays(), newWeekdays) || !Objects.equals(reminderTime, newReminderTime);
        name = newName;
        kind = newKind;
        weekdays = newWeekdays == null ? null : newWeekdays.shortValue();
        reminderTime = newReminderTime;
        return changed;
    }

    public boolean archive(Instant now) {
        if (archivedAt != null) {
            return false;
        }
        archivedAt = now;
        return true;
    }

    public boolean unarchive() {
        if (archivedAt == null) {
            return false;
        }
        archivedAt = null;
        return true;
    }

    /** Ngày đến lịch: DAILY mọi ngày, WEEKLY ngày có thứ trong bitmask (Thứ Hai = bit 0). */
    public boolean isScheduled(LocalDate day) {
        return DAILY.equals(kind) || (weekdays & (1 << (day.getDayOfWeek().getValue() - 1))) != 0;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public Integer getWeekdays() {
        return weekdays == null ? null : weekdays.intValue();
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
