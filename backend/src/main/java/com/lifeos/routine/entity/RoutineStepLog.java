package com.lifeos.routine.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

/** Kết quả một bước trong một lượt (D243): DONE/SKIPPED, duy nhất theo (lượt, bước) với bản chưa xóa. */
@Entity
@Table(name = "routine_step_logs")
public class RoutineStepLog extends SyncedEntity {

    public static final String ENTITY_TYPE = "routine_step_log";

    @Column(name = "routine_log_id", nullable = false, updatable = false)
    private UUID routineLogId;

    @Column(name = "step_id", nullable = false, updatable = false)
    private UUID stepId;

    @Column(nullable = false, length = 7)
    private String status;

    protected RoutineStepLog() {
    }

    private RoutineStepLog(UUID id, UUID userId) {
        super(id, userId);
    }

    public static RoutineStepLog create(UUID id, UUID userId, UUID routineLogId, UUID stepId, String status) {
        RoutineStepLog log = new RoutineStepLog(id, userId);
        log.routineLogId = Objects.requireNonNull(routineLogId);
        log.stepId = Objects.requireNonNull(stepId);
        log.status = Objects.requireNonNull(status);
        return log;
    }

    /** @return true nếu đổi */
    public boolean changeStatus(String newStatus) {
        if (status.equals(newStatus)) {
            return false;
        }
        status = newStatus;
        return true;
    }

    public UUID getRoutineLogId() {
        return routineLogId;
    }

    public UUID getStepId() {
        return stepId;
    }

    public String getStatus() {
        return status;
    }
}
