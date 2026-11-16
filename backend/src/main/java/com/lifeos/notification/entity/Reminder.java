package com.lifeos.notification.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Nhắc nhở (FR-NTF-01/02, D129–D134): gắn TASK/EVENT/HABIT/ROUTINE hoặc độc lập (NONE). nextFireAt, lastFiredAt, status DONE do server; targetType/targetId bất biến. */
@Entity
@Table(name = "reminders")
public class Reminder extends SyncedEntity {

    public static final String ENTITY_TYPE = "reminder";

    @Column(name = "target_type", nullable = false, updatable = false, length = 7)
    private String targetType;

    @Column(name = "target_id", updatable = false)
    private UUID targetId;

    @Column(name = "mode", nullable = false, length = 8)
    private String mode;

    @Column(name = "offset_minutes")
    private Integer offsetMinutes;

    @Column(name = "local_time")
    private LocalTime localTime;

    @Column(name = "local_date")
    private LocalDate localDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "repeat_rule")
    private String repeatRule;

    @Column(name = "urgent", nullable = false)
    private boolean urgent;

    @Column(name = "status", nullable = false, length = 6)
    private String status;

    @Column(name = "next_fire_at")
    private Instant nextFireAt;

    @Column(name = "last_fired_at")
    private Instant lastFiredAt;

    protected Reminder() {
    }

    public Reminder(UUID id, UUID userId) {
        super(id, userId);
    }

    public static Reminder create(UUID id, UUID userId, String targetType, UUID targetId) {
        Reminder reminder = new Reminder(id, userId);
        reminder.targetType = targetType;
        reminder.targetId = targetId;
        reminder.status = "ACTIVE";
        return reminder;
    }

    public String getTargetType() {
        return targetType;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTargetType(String value) {
        if (Objects.equals(targetType, value)) {
            return false;
        }
        targetType = value;
        return true;
    }

    public UUID getTargetId() {
        return targetId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTargetId(UUID value) {
        if (Objects.equals(targetId, value)) {
            return false;
        }
        targetId = value;
        return true;
    }

    public String getMode() {
        return mode;
    }

    /** @return true nếu giá trị đổi */
    public boolean setMode(String value) {
        if (Objects.equals(mode, value)) {
            return false;
        }
        mode = value;
        return true;
    }

    public Integer getOffsetMinutes() {
        return offsetMinutes;
    }

    /** @return true nếu giá trị đổi */
    public boolean setOffsetMinutes(Integer value) {
        if (Objects.equals(offsetMinutes, value)) {
            return false;
        }
        offsetMinutes = value;
        return true;
    }

    public LocalTime getLocalTime() {
        return localTime;
    }

    /** @return true nếu giá trị đổi */
    public boolean setLocalTime(LocalTime value) {
        if (Objects.equals(localTime, value)) {
            return false;
        }
        localTime = value;
        return true;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    /** @return true nếu giá trị đổi */
    public boolean setLocalDate(LocalDate value) {
        if (Objects.equals(localDate, value)) {
            return false;
        }
        localDate = value;
        return true;
    }

    public String getRepeatRule() {
        return repeatRule;
    }

    /** @return true nếu giá trị đổi */
    public boolean setRepeatRule(String value) {
        if (Objects.equals(repeatRule, value)) {
            return false;
        }
        repeatRule = value;
        return true;
    }

    public boolean isUrgent() {
        return urgent;
    }

    /** @return true nếu giá trị đổi */
    public boolean setUrgent(boolean value) {
        if (urgent == value) {
            return false;
        }
        urgent = value;
        return true;
    }

    public String getStatus() {
        return status;
    }

    /** @return true nếu giá trị đổi */
    public boolean setStatus(String value) {
        if (Objects.equals(status, value)) {
            return false;
        }
        status = value;
        return true;
    }

    public Instant getNextFireAt() {
        return nextFireAt;
    }

    /** @return true nếu giá trị đổi */
    public boolean setNextFireAt(Instant value) {
        if (Objects.equals(nextFireAt, value)) {
            return false;
        }
        nextFireAt = value;
        return true;
    }

    public Instant getLastFiredAt() {
        return lastFiredAt;
    }

    /** @return true nếu giá trị đổi */
    public boolean setLastFiredAt(Instant value) {
        if (Objects.equals(lastFiredAt, value)) {
            return false;
        }
        lastFiredAt = value;
        return true;
    }
}
