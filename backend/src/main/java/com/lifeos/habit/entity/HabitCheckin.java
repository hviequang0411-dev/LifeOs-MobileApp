package com.lifeos.habit.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Check-in một ngày của habit (D232): DONE hoặc SKIPPED, ghi chú ≤ 200. Duy nhất theo (habit_id, local_date) với bản
 * chưa xóa (ux_habit_checkins_habit_date); undo = xóa mềm, check-in lại tạo dòng mới. habitId/localDate bất biến.
 */
@Entity
@Table(name = "habit_checkins")
public class HabitCheckin extends SyncedEntity {

    public static final String ENTITY_TYPE = "habit_checkin";

    @Column(name = "habit_id", nullable = false, updatable = false)
    private UUID habitId;

    @Column(name = "local_date", nullable = false, updatable = false)
    private LocalDate localDate;

    @Column(nullable = false, length = 7)
    private String status;

    @Column(length = 200)
    private String note;

    protected HabitCheckin() {
    }

    private HabitCheckin(UUID id, UUID userId) {
        super(id, userId);
    }

    public static HabitCheckin create(UUID id, UUID userId, UUID habitId, LocalDate localDate, String status,
            String note) {
        HabitCheckin checkin = new HabitCheckin(id, userId);
        checkin.habitId = Objects.requireNonNull(habitId);
        checkin.localDate = Objects.requireNonNull(localDate);
        checkin.status = Objects.requireNonNull(status);
        checkin.note = note;
        return checkin;
    }

    /** @return true nếu có giá trị đổi */
    public boolean update(String newStatus, String newNote) {
        boolean changed = !Objects.equals(status, newStatus) || !Objects.equals(note, newNote);
        status = newStatus;
        note = newNote;
        return changed;
    }

    public UUID getHabitId() {
        return habitId;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public String getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }
}
