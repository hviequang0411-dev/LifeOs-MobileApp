package com.lifeos.goal.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Mục tiêu dài hạn (FR-GOAL-01/05, D185, D188). Tiến độ tính, không lưu; statusChangedAt do server. */
@Entity
@Table(name = "goals")
public class Goal extends SyncedEntity {

    public static final String ENTITY_TYPE = "goal";

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "start_on")
    private LocalDate startOn;

    @Column(name = "target_on")
    private LocalDate targetOn;

    @Column(name = "status", nullable = false, length = 9)
    private String status;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    protected Goal() {
    }

    public Goal(UUID id, UUID userId) {
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

    public LocalDate getStartOn() {
        return startOn;
    }

    /** @return true nếu giá trị đổi */
    public boolean setStartOn(LocalDate value) {
        if (Objects.equals(startOn, value)) {
            return false;
        }
        startOn = value;
        return true;
    }

    public LocalDate getTargetOn() {
        return targetOn;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTargetOn(LocalDate value) {
        if (Objects.equals(targetOn, value)) {
            return false;
        }
        targetOn = value;
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

    public Instant getStatusChangedAt() {
        return statusChangedAt;
    }

    /** @return true nếu giá trị đổi */
    public boolean setStatusChangedAt(Instant value) {
        if (Objects.equals(statusChangedAt, value)) {
            return false;
        }
        statusChangedAt = value;
        return true;
    }
}
