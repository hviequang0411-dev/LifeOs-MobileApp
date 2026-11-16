package com.lifeos.goal.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Cột mốc của Goal (FR-GOAL-02, D187): position bước 1000; goalId bất biến; completedAt do server. */
@Entity
@Table(name = "milestones")
public class Milestone extends SyncedEntity {

    public static final String ENTITY_TYPE = "milestone";

    @Column(name = "goal_id", nullable = false, updatable = false)
    private UUID goalId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "due_on")
    private LocalDate dueOn;

    @Column(name = "position", nullable = false)
    private Long position;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Milestone() {
    }

    public Milestone(UUID id, UUID userId) {
        super(id, userId);
    }

    public static Milestone create(UUID id, UUID userId, UUID goalId) {
        Milestone e = new Milestone(id, userId);
        e.goalId = goalId;
        return e;
    }

    public UUID getGoalId() {
        return goalId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setGoalId(UUID value) {
        if (Objects.equals(goalId, value)) {
            return false;
        }
        goalId = value;
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

    public LocalDate getDueOn() {
        return dueOn;
    }

    /** @return true nếu giá trị đổi */
    public boolean setDueOn(LocalDate value) {
        if (Objects.equals(dueOn, value)) {
            return false;
        }
        dueOn = value;
        return true;
    }

    public Long getPosition() {
        return position;
    }

    /** @return true nếu giá trị đổi */
    public boolean setPosition(Long value) {
        if (Objects.equals(position, value)) {
            return false;
        }
        position = value;
        return true;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    /** @return true nếu giá trị đổi */
    public boolean setCompletedAt(Instant value) {
        if (Objects.equals(completedAt, value)) {
            return false;
        }
        completedAt = value;
        return true;
    }
}
