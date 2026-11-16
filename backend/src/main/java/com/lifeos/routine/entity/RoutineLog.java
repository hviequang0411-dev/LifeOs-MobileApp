package com.lifeos.routine.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Lượt chạy routine trong một ngày (FR-RTN-03, D242): một lượt/ngày (ux_routine_logs_routine_date); totalSteps do
 * client chụp lúc Start và bất biến (D249); completedAt do server đặt khi done + skipped ≥ totalSteps.
 */
@Entity
@Table(name = "routine_logs")
public class RoutineLog extends SyncedEntity {

    public static final String ENTITY_TYPE = "routine_log";

    @Column(name = "routine_id", nullable = false, updatable = false)
    private UUID routineId;

    @Column(name = "local_date", nullable = false, updatable = false)
    private LocalDate localDate;

    @Column(name = "total_steps", nullable = false, updatable = false)
    private short totalSteps;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected RoutineLog() {
    }

    private RoutineLog(UUID id, UUID userId) {
        super(id, userId);
    }

    public static RoutineLog start(UUID id, UUID userId, UUID routineId, LocalDate localDate, int totalSteps,
            Instant now) {
        RoutineLog log = new RoutineLog(id, userId);
        log.routineId = Objects.requireNonNull(routineId);
        log.localDate = Objects.requireNonNull(localDate);
        log.totalSteps = (short) totalSteps;
        log.startedAt = now;
        return log;
    }

    /** Đặt/xóa completedAt theo số bước đã có kết quả (D242, D243). @return true nếu đổi */
    public boolean settle(int finishedSteps, Instant now) {
        boolean complete = finishedSteps >= totalSteps;
        if (complete && completedAt == null) {
            completedAt = now;
            return true;
        }
        if (!complete && completedAt != null) {
            completedAt = null;
            return true;
        }
        return false;
    }

    public UUID getRoutineId() {
        return routineId;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
