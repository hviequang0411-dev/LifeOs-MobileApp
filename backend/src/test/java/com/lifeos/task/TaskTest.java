package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.task.entity.Task;
import com.lifeos.task.entity.TaskPriority;
import com.lifeos.task.entity.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Quy tắc miền của Task (TESTING_RULES §5.1): dueTime cần dueDate; OPEN↔DONE đặt/xóa completedAt; báo thay đổi thật. */
class TaskTest {

    private static final Instant NOW = Instant.parse("2026-11-06T10:00:00Z");

    private static Task task(TaskStatus status) {
        return Task.create(UUID.randomUUID(), UUID.randomUUID(), "Mua sữa", null, status, TaskPriority.NONE, null,
                null, null, null, NOW);
    }

    @Test
    void newTaskDefaultsToOpenWithoutCompletedAt() {
        Task t = task(TaskStatus.OPEN);
        assertThat(t.getStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(t.getCompletedAt()).isNull();
        assertThat(t.getProjectId()).as("Inbox").isNull();
    }

    @Test
    void creatingAsDoneSetsCompletedAtByServer() {
        Task t = task(TaskStatus.DONE);
        assertThat(t.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(t.getCompletedAt()).isEqualTo(NOW);
    }

    @Test
    void completeAndReopenToggleCompletedAtAndAreIdempotent() {
        Task t = task(TaskStatus.OPEN);
        assertThat(t.complete(NOW)).isTrue();
        assertThat(t.getCompletedAt()).isEqualTo(NOW);
        assertThat(t.complete(NOW.plusSeconds(60))).isFalse();
        assertThat(t.getCompletedAt()).isEqualTo(NOW);

        assertThat(t.reopen()).isTrue();
        assertThat(t.getStatus()).isEqualTo(TaskStatus.OPEN);
        assertThat(t.getCompletedAt()).isNull();
        assertThat(t.reopen()).isFalse();

        assertThat(t.changeStatus(TaskStatus.DONE, NOW)).isTrue();
        assertThat(t.changeStatus(TaskStatus.DONE, NOW)).isFalse();
    }

    @Test
    void dueTimeNeedsDueDate() {
        Task t = task(TaskStatus.OPEN);
        assertThatThrownBy(() -> t.schedule(null, LocalTime.of(8, 0))).isInstanceOf(IllegalArgumentException.class);
        assertThat(t.schedule(LocalDate.of(2026, 11, 7), LocalTime.of(8, 0))).isTrue();
        assertThat(t.schedule(LocalDate.of(2026, 11, 7), LocalTime.of(8, 0))).isFalse();
        assertThat(t.schedule(null, null)).isTrue();
    }

    @Test
    void attributeChangesReportWhetherAnythingChanged() {
        Task t = task(TaskStatus.OPEN);
        UUID project = UUID.randomUUID();
        assertThat(t.retitle("Mua sữa")).isFalse();
        assertThat(t.retitle("Mua bánh")).isTrue();
        assertThat(t.prioritize(TaskPriority.NONE)).isFalse();
        assertThat(t.prioritize(TaskPriority.HIGH)).isTrue();
        assertThat(t.moveToProject(project)).isTrue();
        assertThat(t.moveToProject(project)).isFalse();
        assertThat(t.moveToProject(null)).isTrue();
        assertThat(t.categorize(null)).isFalse();
        assertThat(t.describe(null)).isFalse();
        assertThat(t.describe("x")).isTrue();
    }
}
