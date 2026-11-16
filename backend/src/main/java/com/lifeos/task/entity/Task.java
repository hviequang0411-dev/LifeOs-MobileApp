package com.lifeos.task.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Công việc (FR-TK-01..08): entity đồng bộ, id do client tạo. projectId/categoryId là cột UUID, KHÔNG có quan hệ JPA
 * (D3; Project thuộc module khác). Bất biến giữ ở đây và ở DB:
 * <ul>
 *   <li>DONE ⇔ completedAt khác null (ck_tasks_done_completed); completedAt do server đặt;</li>
 *   <li>dueTime chỉ khi có dueDate (ck_tasks_due_time_needs_date).</li>
 * </ul>
 * Giá trị (trim, độ dài, định dạng) được kiểm ở TaskCommandService trước khi gọi các method ở đây.
 */
@Entity
@Table(name = "tasks")
public class Task extends SyncedEntity {

    /** Giá trị entity_type trong change_logs / sync. */
    public static final String ENTITY_TYPE = "task";

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskPriority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "due_time")
    private LocalTime dueTime;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "category_id")
    private UUID categoryId;

    /** PHASE_6 D186: Task thuộc tối đa một milestone (và qua đó một goal); server đặt/kiểm. */
    @jakarta.persistence.Column(name = "goal_id")
    private UUID goalId;

    @jakarta.persistence.Column(name = "milestone_id")
    private UUID milestoneId;

    protected Task() {
    }

    private Task(UUID id, UUID userId) {
        super(id, userId);
    }

    /** Tạo Task; status DONE thì completedAt = now (server đặt). */
    public static Task create(UUID id, UUID userId, String title, String description, TaskStatus status,
            TaskPriority priority, LocalDate dueDate, LocalTime dueTime, UUID projectId, UUID categoryId,
            Instant now) {
        Task task = new Task(id, userId);
        task.title = Objects.requireNonNull(title);
        task.description = description;
        task.priority = Objects.requireNonNull(priority);
        task.schedule(dueDate, dueTime);
        task.projectId = projectId;
        task.categoryId = categoryId;
        task.status = TaskStatus.OPEN;
        if (status == TaskStatus.DONE) {
            task.complete(now);
        }
        return task;
    }

    /** OPEN → DONE, đặt completedAt. Đã DONE: không làm gì (D78). @return true nếu đổi */
    public boolean complete(Instant now) {
        if (status == TaskStatus.DONE) {
            return false;
        }
        status = TaskStatus.DONE;
        completedAt = Objects.requireNonNull(now);
        return true;
    }

    /** DONE → OPEN, xóa completedAt. Đã OPEN: không làm gì (D78). @return true nếu đổi */
    public boolean reopen() {
        if (status == TaskStatus.OPEN) {
            return false;
        }
        status = TaskStatus.OPEN;
        completedAt = null;
        return true;
    }

    /** Đặt trạng thái theo PATCH/sync (D78: PATCH được đổi status). @return true nếu đổi */
    public boolean changeStatus(TaskStatus newStatus, Instant now) {
        return newStatus == TaskStatus.DONE ? complete(now) : reopen();
    }

    /** Đặt hạn; dueTime cần dueDate. @return true nếu đổi */
    public boolean schedule(LocalDate newDueDate, LocalTime newDueTime) {
        if (newDueTime != null && newDueDate == null) {
            throw new IllegalArgumentException("dueTime cần dueDate");
        }
        if (Objects.equals(newDueDate, dueDate) && Objects.equals(newDueTime, dueTime)) {
            return false;
        }
        dueDate = newDueDate;
        dueTime = newDueTime;
        return true;
    }

    public boolean retitle(String newTitle) {
        Objects.requireNonNull(newTitle);
        if (newTitle.equals(title)) {
            return false;
        }
        title = newTitle;
        return true;
    }

    public boolean describe(String newDescription) {
        if (Objects.equals(newDescription, description)) {
            return false;
        }
        description = newDescription;
        return true;
    }

    public boolean prioritize(TaskPriority newPriority) {
        Objects.requireNonNull(newPriority);
        if (newPriority == priority) {
            return false;
        }
        priority = newPriority;
        return true;
    }

    /** Gắn/bỏ gắn Project (null = Inbox). Tham chiếu đã được kiểm ở service. @return true nếu đổi */
    public boolean moveToProject(UUID newProjectId) {
        if (Objects.equals(newProjectId, projectId)) {
            return false;
        }
        projectId = newProjectId;
        return true;
    }

    /** Gắn/bỏ liên kết Goal/Milestone (D186; đã kiểm ở service). @return true nếu đổi */
    public boolean link(UUID newGoalId, UUID newMilestoneId) {
        if (Objects.equals(newGoalId, goalId) && Objects.equals(newMilestoneId, milestoneId)) {
            return false;
        }
        goalId = newGoalId;
        milestoneId = newMilestoneId;
        return true;
    }

    public UUID getGoalId() {
        return goalId;
    }

    public UUID getMilestoneId() {
        return milestoneId;
    }

    /** Gắn/bỏ Category (null = không danh mục). @return true nếu đổi */
    public boolean categorize(UUID newCategoryId) {
        if (Objects.equals(newCategoryId, categoryId)) {
            return false;
        }
        categoryId = newCategoryId;
        return true;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }
}
