package com.lifeos.routine.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** Bước của routine (FR-RTN-02, D241): position bước 1000, chèn giữa bằng trung điểm (client); routineId bất biến. */
@Entity
@Table(name = "routine_steps")
public class RoutineStep extends SyncedEntity {

    public static final String ENTITY_TYPE = "routine_step";

    @Column(name = "routine_id", nullable = false, updatable = false)
    private UUID routineId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private long position;

    @Column(name = "duration_minutes")
    private Short durationMinutes;

    protected RoutineStep() {
    }

    private RoutineStep(UUID id, UUID userId) {
        super(id, userId);
    }

    public static RoutineStep create(UUID id, UUID userId, UUID routineId, String title, long position,
            Integer durationMinutes) {
        RoutineStep step = new RoutineStep(id, userId);
        step.routineId = Objects.requireNonNull(routineId);
        step.apply(title, position, durationMinutes);
        return step;
    }

    /** @return true nếu có giá trị đổi */
    public boolean apply(String newTitle, long newPosition, Integer newDuration) {
        boolean changed = !Objects.equals(title, newTitle) || position != newPosition
                || !Objects.equals(getDurationMinutes(), newDuration);
        title = newTitle;
        position = newPosition;
        durationMinutes = newDuration == null ? null : newDuration.shortValue();
        return changed;
    }

    public UUID getRoutineId() {
        return routineId;
    }

    public String getTitle() {
        return title;
    }

    public long getPosition() {
        return position;
    }

    public Integer getDurationMinutes() {
        return durationMinutes == null ? null : durationMinutes.intValue();
    }
}
