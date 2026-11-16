package com.lifeos.calendar.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/** Ngoại lệ một lần xuất hiện của sự kiện lặp (D127): CANCELLED hoặc MODIFIED (trường ghi đè). eventId và originalStartLocal bất biến. */
@Entity
@Table(name = "event_exceptions")
public class EventException extends SyncedEntity {

    public static final String ENTITY_TYPE = "event_exception";

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "original_start_local", nullable = false, updatable = false)
    private LocalDateTime originalStartLocal;

    @Column(name = "kind", nullable = false, length = 9)
    private String kind;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "location", length = 200)
    private String location;

    @Column(name = "start_local")
    private LocalDateTime startLocal;

    @Column(name = "end_local")
    private LocalDateTime endLocal;

    protected EventException() {
    }

    public EventException(UUID id, UUID userId) {
        super(id, userId);
    }

    public static EventException create(UUID id, UUID userId, UUID eventId, LocalDateTime originalStartLocal) {
        EventException x = new EventException(id, userId);
        x.eventId = eventId;
        x.originalStartLocal = originalStartLocal;
        return x;
    }

    public UUID getEventId() {
        return eventId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setEventId(UUID value) {
        if (Objects.equals(eventId, value)) {
            return false;
        }
        eventId = value;
        return true;
    }

    public LocalDateTime getOriginalStartLocal() {
        return originalStartLocal;
    }

    /** @return true nếu giá trị đổi */
    public boolean setOriginalStartLocal(LocalDateTime value) {
        if (Objects.equals(originalStartLocal, value)) {
            return false;
        }
        originalStartLocal = value;
        return true;
    }

    public String getKind() {
        return kind;
    }

    /** @return true nếu giá trị đổi */
    public boolean setKind(String value) {
        if (Objects.equals(kind, value)) {
            return false;
        }
        kind = value;
        return true;
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
}
