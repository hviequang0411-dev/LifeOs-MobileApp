package com.lifeos.habit.entity;

import com.lifeos.common.persistence.SyncedEntity;
import com.lifeos.habit.domain.HabitFields;
import com.lifeos.habit.domain.HabitSchedule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Thói quen (FR-HAB-01..10, D230, D236): entity đồng bộ, id do client tạo. Giá trị đã được HabitCommandService kiểm
 * (HabitValues) trước khi gọi các method ở đây; DB giữ CHECK của lịch (V5).
 */
@Entity
@Table(name = "habits")
public class Habit extends SyncedEntity {

    public static final String ENTITY_TYPE = "habit";

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 7)
    private String color;

    @Column(length = 30)
    private String icon;

    @Column(name = "schedule_type", nullable = false, length = 14)
    private String scheduleType;

    private Short weekdays;

    @Column(name = "times_per_week")
    private Short timesPerWeek;

    @Column(name = "interval_days")
    private Short intervalDays;

    @Column(name = "start_on", nullable = false)
    private LocalDate startOn;

    @Column(name = "end_on")
    private LocalDate endOn;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Habit() {
    }

    private Habit(UUID id, UUID userId) {
        super(id, userId);
    }

    public static Habit create(UUID id, UUID userId, HabitFields fields) {
        Habit habit = new Habit(id, userId);
        habit.apply(fields);
        return habit;
    }

    /** Ghi đè mọi trường chỉnh sửa được. @return true nếu có giá trị đổi */
    public boolean apply(HabitFields f) {
        boolean changed = !Objects.equals(name, f.name()) || !Objects.equals(description, f.description())
                || !Objects.equals(color, f.color()) || !Objects.equals(icon, f.icon())
                || !Objects.equals(scheduleType, f.schedule().type())
                || !Objects.equals(toInt(weekdays), f.schedule().weekdays())
                || !Objects.equals(toInt(timesPerWeek), f.schedule().timesPerWeek())
                || !Objects.equals(toInt(intervalDays), f.schedule().intervalDays())
                || !Objects.equals(startOn, f.schedule().startOn()) || !Objects.equals(endOn, f.schedule().endOn())
                || !Objects.equals(reminderTime, f.reminderTime());
        name = f.name();
        description = f.description();
        color = f.color();
        icon = f.icon();
        scheduleType = f.schedule().type();
        weekdays = toShort(f.schedule().weekdays());
        timesPerWeek = toShort(f.schedule().timesPerWeek());
        intervalDays = toShort(f.schedule().intervalDays());
        startOn = f.schedule().startOn();
        endOn = f.schedule().endOn();
        reminderTime = f.reminderTime();
        return changed;
    }

    /** @return true nếu lần này mới lưu trữ */
    public boolean archive(Instant now) {
        if (archivedAt != null) {
            return false;
        }
        archivedAt = now;
        return true;
    }

    /** @return true nếu lần này mới bỏ lưu trữ */
    public boolean unarchive() {
        if (archivedAt == null) {
            return false;
        }
        archivedAt = null;
        return true;
    }

    public HabitSchedule schedule() {
        return new HabitSchedule(scheduleType, toInt(weekdays), toInt(timesPerWeek), toInt(intervalDays), startOn,
                endOn);
    }

    public HabitFields fields() {
        return new HabitFields(name, description, color, icon, schedule(), reminderTime);
    }

    private static Integer toInt(Short value) {
        return value == null ? null : value.intValue();
    }

    private static Short toShort(Integer value) {
        return value == null ? null : value.shortValue();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        return color;
    }

    public String getIcon() {
        return icon;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public Integer getWeekdays() {
        return toInt(weekdays);
    }

    public Integer getTimesPerWeek() {
        return toInt(timesPerWeek);
    }

    public Integer getIntervalDays() {
        return toInt(intervalDays);
    }

    public LocalDate getStartOn() {
        return startOn;
    }

    public LocalDate getEndOn() {
        return endOn;
    }

    public LocalTime getReminderTime() {
        return reminderTime;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
