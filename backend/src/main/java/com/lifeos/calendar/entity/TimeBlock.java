package com.lifeos.calendar.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Khoảng thời gian dành cho một Task (FR-CAL-03, D128): ≤ 24 giờ, không chặn trùng. taskId bất biến. */
@Entity
@Table(name = "time_blocks")
public class TimeBlock extends SyncedEntity {

    public static final String ENTITY_TYPE = "time_block";

    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "note", length = 200)
    private String note;

    protected TimeBlock() {
    }

    public TimeBlock(UUID id, UUID userId) {
        super(id, userId);
    }

    public static TimeBlock create(UUID id, UUID userId, UUID taskId) {
        TimeBlock block = new TimeBlock(id, userId);
        block.taskId = taskId;
        return block;
    }

    public UUID getTaskId() {
        return taskId;
    }

    /** @return true nếu giá trị đổi */
    public boolean setTaskId(UUID value) {
        if (Objects.equals(taskId, value)) {
            return false;
        }
        taskId = value;
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

    public String getNote() {
        return note;
    }

    /** @return true nếu giá trị đổi */
    public boolean setNote(String value) {
        if (Objects.equals(note, value)) {
            return false;
        }
        note = value;
        return true;
    }
}
