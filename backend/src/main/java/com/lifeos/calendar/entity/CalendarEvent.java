package com.lifeos.calendar.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Sự kiện lịch (FR-CAL-01/02, D124–D127). startLocal/endLocal (hoặc startDate/endDate khi allDay) + timezone là nguồn sự thật; startAt/endAt/recurrenceUntil là giá trị dẫn xuất UTC do server tính. recurrence là JSON chuẩn D126. */
@Entity
@Table(name = "calendar_events")
public class CalendarEvent extends SyncedEntity {

    public static final String ENTITY_TYPE = "calendar_event";

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(name = "timezone", nullable = false, length = 64)
    private String timezone;

    @Column(name = "start_local")
    private LocalDateTime startLocal;

    @Column(name = "end_local")
    private LocalDateTime endLocal;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recurrence")
    private String recurrence;

    @Column(name = "recurrence_until")
    private Instant recurrenceUntil;

    protected CalendarEvent() {
    }

    public CalendarEvent(UUID id, UUID userId) {
        super(id, userId);
    }

    public String getTitle() {
        return title;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTitle(String value) {
        if (Objects.equals(title, value)) {
            return false;
        }
        title = value;
        return true;
    }

    public String getDescription() {
        return description;
    }

    /** @return true nếu giá trị đổi */
    public boolean setDescription(String value) {
        if (Objects.equals(description, value)) {
            return false;
        }
        description = value;
        return true;
    }

    public String getLocation() {
        return location;
    }

    /** @return true nếu giá trị đổi */
    public boolean setLocation(String value) {
        if (Objects.equals(location, value)) {
            return false;
        }
        location = value;
        return true;
    }

    public boolean isAllDay() {
        return allDay;
    }

    /** @return true nếu giá trị đổi */
    public boolean setAllDay(boolean value) {
        if (allDay == value) {
            return false;
        }
        allDay = value;
        return true;
    }

    public String getTimezone() {
        return timezone;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTimezone(String value) {
        if (Objects.equals(timezone, value)) {
            return false;
        }
        timezone = value;
        return true;
    }

    public LocalDateTime getStartLocal() {
        return startLocal;
    }

    /** @return true nếu giá trị đổi */
    public boolean setStartLocal(LocalDateTime value) {
        if (Objects.equals(startLocal, value)) {
            return false;
        }
        startLocal = value;
        return true;
    }

    public LocalDateTime getEndLocal() {
        return endLocal;
    }

    /** @return true nếu giá trị đổi */
    public boolean setEndLocal(LocalDateTime value) {
        if (Objects.equals(endLocal, value)) {
            return false;
        }
        endLocal = value;
        return true;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    /** @return true nếu giá trị đổi */
    public boolean setStartDate(LocalDate value) {
        if (Objects.equals(startDate, value)) {
            return false;
        }
        startDate = value;
        return true;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    /** @return true nếu giá trị đổi */
    public boolean setEndDate(LocalDate value) {
        if (Objects.equals(endDate, value)) {
            return false;
        }
        endDate = value;
        return true;
    }

    public Instant getStartAt() {
        return startAt;
    }

    /** @return true nếu giá trị đổi */
    public boolean setStartAt(Instant value) {
        if (Objects.equals(startAt, value)) {
            return false;
        }
        startAt = value;
        return true;
    }

    public Instant getEndAt() {
        return endAt;
    }

    /** @return true nếu giá trị đổi */
    public boolean setEndAt(Instant value) {
        if (Objects.equals(endAt, value)) {
            return false;
        }
        endAt = value;
        return true;
    }

    public String getRecurrence() {
        return recurrence;
    }

    /** @return true nếu giá trị đổi */
    public boolean setRecurrence(String value) {
        if (Objects.equals(recurrence, value)) {
            return false;
        }
        recurrence = value;
        return true;
    }

    public Instant getRecurrenceUntil() {
        return recurrenceUntil;
    }

    /** @return true nếu giá trị đổi */
    public boolean setRecurrenceUntil(Instant value) {
        if (Objects.equals(recurrenceUntil, value)) {
            return false;
        }
        recurrenceUntil = value;
        return true;
    }
}
