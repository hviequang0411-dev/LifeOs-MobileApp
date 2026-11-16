package com.lifeos.task.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.project.api.ProjectApi;
import com.lifeos.task.dto.TaskResponse;
import com.lifeos.task.entity.Task;
import com.lifeos.task.entity.TaskPriority;
import com.lifeos.task.entity.TaskStatus;
import com.lifeos.task.mapper.TaskMapper;
import com.lifeos.task.repository.CategoryRepository;
import com.lifeos.task.repository.TaskRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh ghi Task (UC-TK-01, 04, 05, 06; API_CONVENTIONS §8.4; D58, D62, D78, D80). Mỗi thay đổi thật gọi ChangeRecorder
 * đúng một lần trong cùng transaction (A9), kể cả từng Task bị ảnh hưởng khi cascade.
 * <p>
 * Tham chiếu chéo (D80, SECURITY_RULES §7.4): projectId kiểm qua ProjectApi.lockActiveOwned (FOR SHARE), categoryId
 * qua CategoryRepository.findActiveForShare (FOR SHARE), LUÔN khóa Project trước Category. Không tồn tại / đã xóa /
 * của user khác → 422 PROJECT_NOT_FOUND / CATEGORY_NOT_FOUND giống hệt nhau (D61, IDOR-04). Khóa chia sẻ chặn việc xóa
 * Project/Category đồng thời cho đến khi Task được ghi xong, nên không có Task trỏ tới bản ghi vừa xóa.
 */
@Service
public class TaskCommandService {

    private static final String PK_CONSTRAINT = "pk_tasks";

    private final TaskRepository tasks;
    private final CategoryRepository categories;
    private final ProjectApi projectApi;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final TaskMapper mapper;
    private final Clock clock;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final com.lifeos.common.goal.GoalLinks goalLinks;

    public TaskCommandService(TaskRepository tasks, CategoryRepository categories, ProjectApi projectApi,
            ChangeRecorder changeRecorder, VersionedWrites versionedWrites, TaskMapper mapper, Clock clock,
            org.springframework.context.ApplicationEventPublisher events, com.lifeos.common.goal.GoalLinks goalLinks) {
        this.tasks = tasks;
        this.categories = categories;
        this.projectApi = projectApi;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
        this.events = events;
        this.goalLinks = goalLinks;
    }

    /** POST /tasks (REST). */
    public TaskResponse create(UUID userId, TaskCommands.Create cmd) {
        return create(userId, cmd, null);
    }

    /**
     * Tạo Task. Chỉ title bắt buộc; mặc định OPEN, NONE, Inbox (FR-TK-01). Thứ tự D96: id đã dùng (409) → giá trị
     * (400) → tham chiếu (422) → ghi.
     *
     * @param mutationIdOrNull mutationId nếu đến từ sync; null nếu từ REST
     */
    public TaskResponse create(UUID userId, TaskCommands.Create cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (tasks.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String title = TaskValues.title(cmd.title().value());
                String description = TaskValues.description(cmd.description().value());
                TaskStatus status = TaskValues.status(cmd.status().value(), true);
                TaskPriority priority = TaskValues.priority(cmd.priority().value(), true);
                LocalDate dueDate = TaskValues.dueDate(cmd.dueDate().value());
                LocalTime dueTime = TaskValues.dueTime(cmd.dueTime().value());
                TaskValues.requireDateForTime(dueDate, dueTime);
                UUID projectId = cmd.projectId().value();
                UUID categoryId = cmd.categoryId().value();
                lockReferences(userId, projectId, categoryId);
                Task task = Task.create(cmd.id(), userId, title, description, status, priority, dueDate, dueTime,
                        projectId, categoryId, clock.instant());
                UUID goalId = cmd.goalId().value();
                UUID milestoneId = cmd.milestoneId().value();
                if (goalId != null || milestoneId != null) {
                    com.lifeos.common.goal.GoalLinks.Link link = goalLinks.resolve(userId, goalId, milestoneId);
                    task.link(link.goalId(), link.milestoneId());
                }
                Task saved = tasks.saveAndFlush(task);
                changeRecorder.record(userId, Task.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (mentions(e, PK_CONSTRAINT)) {
                throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    /** PATCH /tasks/{id} (REST). */
    public TaskResponse update(UUID userId, UUID taskId, TaskCommands.Update cmd) {
        return update(userId, taskId, cmd, null);
    }

    /**
     * Sửa Task theo JSON Merge Patch (D78: được đổi status; completedAt do server). Thứ tự D96: tồn tại (404) →
     * version (409) → giá trị (400) → tham chiếu (422) → áp dụng.
     */
    public TaskResponse update(UUID userId, UUID taskId, TaskCommands.Update cmd, UUID mutationIdOrNull) {
        return writeExisting(userId, taskId, cmd.expectedVersion(), mutationIdOrNull, task -> {
            String title = cmd.title().present() ? TaskValues.title(cmd.title().value()) : null;
            String description = cmd.description().present()
                    ? TaskValues.description(cmd.description().value()) : null;
            TaskStatus status = cmd.status().present() ? TaskValues.status(cmd.status().value(), false) : null;
            TaskPriority priority = cmd.priority().present()
                    ? TaskValues.priority(cmd.priority().value(), false) : null;
            LocalDate dueDate = cmd.dueDate().present() ? TaskValues.dueDate(cmd.dueDate().value()) : task.getDueDate();
            LocalTime dueTime = cmd.dueTime().present() ? TaskValues.dueTime(cmd.dueTime().value()) : task.getDueTime();
            TaskValues.requireDateForTime(dueDate, dueTime);

            UUID projectId = cmd.projectId().present() ? cmd.projectId().value() : task.getProjectId();
            UUID categoryId = cmd.categoryId().present() ? cmd.categoryId().value() : task.getCategoryId();
            // Chỉ kiểm (và khóa) tham chiếu MỚI; thứ tự Project trước Category.
            lockReferences(userId,
                    projectId != null && !projectId.equals(task.getProjectId()) ? projectId : null,
                    categoryId != null && !categoryId.equals(task.getCategoryId()) ? categoryId : null);

            Instant now = clock.instant();
            boolean changed = false;
            changed |= title != null && task.retitle(title);
            changed |= cmd.description().present() && task.describe(description);
            changed |= status != null && task.changeStatus(status, now);
            changed |= priority != null && task.prioritize(priority);
            changed |= task.schedule(dueDate, dueTime);
            changed |= task.moveToProject(projectId);
            changed |= task.categorize(categoryId);
            changed |= relink(userId, task, cmd);
            return changed;
        });
    }

    /** POST /tasks/{id}/complete. Đã DONE: kiểm version rồi 200, không đổi gì (D78). */
    public TaskResponse complete(UUID userId, UUID taskId, long expectedVersion) {
        return writeExisting(userId, taskId, expectedVersion, null, task -> task.complete(clock.instant()));
    }

    /** POST /tasks/{id}/reopen. Đang OPEN: kiểm version rồi 200, không đổi gì (D78). */
    public TaskResponse reopen(UUID userId, UUID taskId, long expectedVersion) {
        return writeExisting(userId, taskId, expectedVersion, null, Task::reopen);
    }

    /** DELETE /tasks/{id}?version=n (REST). */
    public long delete(UUID userId, UUID taskId, long expectedVersion) {
        return delete(userId, taskId, expectedVersion, null);
    }

    /**
     * Xóa mềm Task (FR-TK-04). Đã xóa / không tồn tại → 404 (D78).
     *
     * @return version của Task sau khi xóa (tombstone)
     */
    public long delete(UUID userId, UUID taskId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            Task task = tasks.findActive(taskId, userId).orElseThrow(TaskCommandService::notFound);
            VersionedWrites.requireVersion(task, expectedVersion);
            task.softDelete(clock.instant());
            Task saved = tasks.saveAndFlush(task);
            // PHASE_4 D128/D129: time block và reminder của Task bị xóa mềm trong cùng transaction (trước record).
            com.lifeos.common.sync.PendingChanges cascade = new com.lifeos.common.sync.PendingChanges();
            events.publishEvent(new com.lifeos.task.api.TaskDeletionStarted(userId, taskId, cascade));
            cascade.recordAll(changeRecorder, userId);
            changeRecorder.record(userId, Task.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, taskId));
    }

    /**
     * Cascade khi xóa Project (SYSTEM_ARCHITECTURE §3.8, D20, D80, D86): mọi Task còn sống của Project (kể cả DONE) về
     * Inbox, mỗi Task tăng version và có change_logs UPDATE riêng với mutation_id NULL (SYNC_PROTOCOL §3.2). Chạy trong
     * transaction xóa Project của bên gọi (Project đã khóa FOR UPDATE, đã xóa mềm và flush).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int detachFromProject(UUID userId, UUID projectId) {
        return detachAll(userId, tasks.findLiveByProject(userId, projectId), task -> task.moveToProject(null));
    }

    /** Cascade khi xóa Category: Task về "không danh mục" (FR-CT-01), tương tự detachFromProject. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int detachFromCategory(UUID userId, UUID categoryId) {
        return detachAll(userId, tasks.findLiveByCategory(userId, categoryId), task -> task.categorize(null));
    }

    /** D86: tải hết (ORDER BY id) → sửa → flush tất cả → rồi mới ghi change_logs từng Task. */
    private int detachAll(UUID userId, List<Task> affected, Predicate<Task> detach) {
        affected.forEach(detach::test);
        List<Task> saved = tasks.saveAllAndFlush(affected);
        for (Task task : saved) {
            changeRecorder.record(userId, Task.ENTITY_TYPE, task.getId(), ChangeOperation.UPDATE, task.getVersion(),
                    null);
        }
        return saved.size();
    }

    /**
     * Khung chung (D96): tải (404) → kiểm version (409) → kiểm giá trị/tham chiếu và áp thay đổi trong {@code change}
     * → nếu đổi thật thì flush (version +1) và ghi log.
     */
    private TaskResponse writeExisting(UUID userId, UUID taskId, long expectedVersion, UUID mutationIdOrNull,
            Predicate<Task> change) {
        return versionedWrites.write(() -> {
            Task task = tasks.findActive(taskId, userId).orElseThrow(TaskCommandService::notFound);
            VersionedWrites.requireVersion(task, expectedVersion);
            LocalDate oldDueDate = task.getDueDate();
            LocalTime oldDueTime = task.getDueTime();
            if (!change.test(task)) {
                return mapper.toResponse(task);
            }
            Task saved = tasks.saveAndFlush(task);
            if (!java.util.Objects.equals(oldDueDate, saved.getDueDate())
                    || !java.util.Objects.equals(oldDueTime, saved.getDueTime())) {
                events.publishEvent(new com.lifeos.task.api.TaskScheduleChanged(userId, taskId, saved.getDueDate(),
                        saved.getDueTime()));
            }
            changeRecorder.record(userId, Task.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, taskId));
    }

    /** Khóa tham chiếu (FOR SHARE), Project trước Category; thiếu → 422 không phân biệt lý do. */
    private void lockReferences(UUID userId, UUID projectId, UUID categoryId) {
        if (projectId != null && projectApi.lockActiveOwned(userId, projectId).isEmpty()) {
            throw new ValidationException(ErrorCode.PROJECT_NOT_FOUND, ErrorCode.PROJECT_NOT_FOUND.title());
        }
        if (categoryId != null && categories.findActiveForShare(categoryId, userId).isEmpty()) {
            throw new ValidationException(ErrorCode.CATEGORY_NOT_FOUND, ErrorCode.CATEGORY_NOT_FOUND.title());
        }
    }

    private long currentVersion(UUID userId, UUID taskId) {
        return tasks.findVersion(taskId, userId).orElseThrow(TaskCommandService::notFound);
    }

    private static boolean mentions(Throwable e, String constraint) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }

    private static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.TASK_NOT_FOUND);
    }

    /**
     * D186: milestoneId có mặt ⇒ goal = goal của milestone (goalId gửi kèm phải khớp); chỉ goalId đổi ⇒ milestone cũ
     * thuộc goal khác bị gỡ [PROPOSED]; goalId = null ⇒ gỡ cả milestone.
     */
    private boolean relink(UUID userId, Task task, TaskCommands.Update cmd) {
        if (!cmd.goalId().present() && !cmd.milestoneId().present()) {
            return false;
        }
        UUID goalId = cmd.goalId().present() ? cmd.goalId().value() : task.getGoalId();
        UUID milestoneId = cmd.milestoneId().present() ? cmd.milestoneId().value()
                : (cmd.goalId().present() && !java.util.Objects.equals(goalId, task.getGoalId()) ? null
                        : task.getMilestoneId());
        if (goalId == null && milestoneId == null) {
            return task.link(null, null);
        }
        com.lifeos.common.goal.GoalLinks.Link link = goalLinks.resolve(userId,
                cmd.goalId().present() || milestoneId == null ? goalId : null, milestoneId);
        return task.link(link.goalId(), link.milestoneId());
    }
}
