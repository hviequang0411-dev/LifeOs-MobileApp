package com.lifeos.task.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.web.Cursors;
import com.lifeos.project.api.ProjectApi;
import com.lifeos.task.dto.ProjectProgressResponse;
import com.lifeos.task.dto.TaskResponse;
import com.lifeos.task.dto.TaskSyncState;
import com.lifeos.task.api.TaskAiQueryApi;
import com.lifeos.task.entity.Task;
import com.lifeos.task.entity.TaskPriority;
import com.lifeos.task.entity.TaskStatus;
import com.lifeos.task.mapper.TaskMapper;
import com.lifeos.task.repository.ProjectProgressCounts;
import com.lifeos.task.repository.TaskListCriteria;
import com.lifeos.task.repository.TaskListRepository;
import com.lifeos.task.repository.TaskRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc Task của user hiện tại (FR-TK-02, 09, 11; FR-PJ-05; API_CONVENTIONS §7, §8.4–8.5; D59, D78, D79). Chỉ bản ghi
 * chưa xóa; tìm kiếm văn bản không có ở REST (D40). Không tự ẩn Task thuộc Project đã lưu trữ (D78).
 */
@Service
public class TaskQueryService implements TaskAiQueryApi {

    private static final Set<String> SORT_FIELDS = Set.of("dueDate", "priority", "createdAt", "updatedAt");
    private static final String DEFAULT_SORT = "createdAt:desc";

    private final TaskRepository tasks;
    private final TaskListRepository taskList;
    private final ProjectApi projectApi;
    private final TaskMapper mapper;
    private final com.lifeos.common.goal.GoalLinks goalLinks;

    public TaskQueryService(TaskRepository tasks, TaskListRepository taskList, ProjectApi projectApi,
            TaskMapper mapper, com.lifeos.common.goal.GoalLinks goalLinks) {
        this.tasks = tasks;
        this.taskList = taskList;
        this.projectApi = projectApi;
        this.mapper = mapper;
        this.goalLinks = goalLinks;
    }

    /**
     * Trạng thái hiện tại của các Task thuộc userId, KỂ CẢ tombstone (snapshot cho sync, SYNC_PROTOCOL §6). Một truy vấn
     * theo lô; id không thuộc user không được trả.
     */
    @Transactional(readOnly = true)
    public List<TaskSyncState> syncStates(UUID userId, Collection<UUID> taskIds) {
        if (taskIds.isEmpty()) {
            return List.of();
        }
        return tasks.findAllOwned(userId, taskIds).stream()
                .map(t -> new TaskSyncState(mapper.toResponse(t), t.isDeleted()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID userId, UUID taskId) {
        return tasks.findActive(taskId, userId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException(ErrorCode.TASK_NOT_FOUND));
    }

    /** GET /tasks: lọc (AND), sắp xếp, phân trang keyset theo con trỏ. */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(UUID userId, TaskListQuery query) {
        TaskStatus status = query.status() == null ? null : TaskValues.status(query.status(), false);
        TaskPriority priority = query.priority() == null ? null : TaskValues.priority(query.priority(), false);
        LocalDate dueFrom = date(query.dueFrom(), "dueFrom");
        LocalDate dueTo = date(query.dueTo(), "dueTo");
        if (dueFrom != null && dueTo != null && dueFrom.isAfter(dueTo)) {
            throw invalid("dueFrom", "dueFrom phải trước hoặc bằng dueTo.");
        }
        if (Boolean.TRUE.equals(query.inbox()) && query.projectId() != null) {
            throw invalid("inbox", "Không dùng inbox=true cùng projectId.");
        }

        String sort = query.sort() == null ? DEFAULT_SORT : query.sort();
        String[] parts = sort.split(":", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])
                || !(parts[1].equals("asc") || parts[1].equals("desc"))) {
            throw invalid("sort", "sort = dueDate|priority|createdAt|updatedAt kèm :asc hoặc :desc.");
        }
        String field = parts[0];
        boolean ascending = parts[1].equals("asc");
        int size = Cursors.limit(query.limit());

        Cursors.Position after = Cursors.decode(query.cursor(), sort);
        Object afterKey = after == null ? null : parseKey(field, after.key());

        List<Task> rows = taskList.page(new TaskListCriteria(userId, status, query.projectId(), query.inbox(),
                query.categoryId(), priority, dueFrom, dueTo, field, ascending, afterKey,
                after == null ? null : after.id(), size));
        boolean hasMore = rows.size() > size;
        List<Task> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            Task last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(sort, keyOf(field, last), last.getId()));
        }
        return new PageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskAiQueryApi.Snapshot> aiWindow(UUID userId, LocalDate from, LocalDate to, int limit) {
        PageResponse<TaskResponse> page = list(userId, new TaskListQuery(null, null, null, null, null,
                from.toString(), to.toString(), "dueDate:asc", Math.min(limit, 200), null));
        return page.items().stream().map(t -> new TaskAiQueryApi.Snapshot(t.id(), t.title(), t.description(),
                t.status(), t.priority(), t.dueDate(), t.dueTime(), t.version())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TaskAiQueryApi.Snapshot> aiTask(UUID userId, UUID taskId) {
        return tasks.findActive(taskId, userId).map(mapper::toResponse)
                .map(t -> new TaskAiQueryApi.Snapshot(t.id(), t.title(), t.description(), t.status(), t.priority(),
                        t.dueDate(), t.dueTime(), t.version()));
    }

    /** GET /projects/{id}/progress (FR-PJ-05, D2): Project không tồn tại/đã xóa/của user khác → 404. */
    @Transactional(readOnly = true)
    public ProjectProgressResponse progress(UUID userId, UUID projectId) {
        if (projectApi.findActiveOwned(userId, projectId).isEmpty()) {
            throw new NotFoundException(ErrorCode.PROJECT_NOT_FOUND);
        }
        ProjectProgressCounts counts = tasks.progress(userId, projectId);
        BigDecimal ratio = counts.total() == 0
                ? BigDecimal.ZERO.setScale(4)
                : BigDecimal.valueOf(counts.completed()).divide(BigDecimal.valueOf(counts.total()), 4,
                        RoundingMode.HALF_UP);
        return new ProjectProgressResponse(projectId, counts.total(), counts.completed(),
                counts.total() - counts.completed(), ratio);
    }

    /** Khóa sắp xếp của bản ghi, ghi vào con trỏ. */
    private static String keyOf(String field, Task task) {
        return switch (field) {
            case "dueDate" -> task.getDueDate() == null ? null : task.getDueDate().toString();
            case "priority" -> Integer.toString(task.getPriority().ordinal());
            case "createdAt" -> task.getCreatedAt().toString();
            case "updatedAt" -> task.getUpdatedAt().toString();
            default -> throw new IllegalArgumentException(field);
        };
    }

    /** Đọc khóa từ con trỏ; hỏng → 400 field cursor. */
    private static Object parseKey(String field, String key) {
        try {
            return switch (field) {
                case "dueDate" -> key == null ? null : LocalDate.parse(key);
                case "priority" -> {
                    int rank = Integer.parseInt(key);
                    if (rank < 0 || rank >= TaskPriority.values().length) {
                        throw new NumberFormatException();
                    }
                    yield rank;
                }
                default -> Instant.parse(key);
            };
        } catch (DateTimeParseException | NumberFormatException | NullPointerException e) {
            throw invalid("cursor", "Con trỏ phân trang không hợp lệ.");
        }
    }

    private static LocalDate date(String raw, String field) {
        if (raw == null) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, "PATTERN", "Ngày theo dạng yyyy-MM-dd.");
        }
    }

    private static ValidationException invalid(String field, String message) {
        return FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, "INVALID_VALUE", message);
    }

    private static final String GOAL_SORT = "id:asc";

    /** GET /goals/{id}/tasks (PHASE_6 §6): Task còn sống gắn Goal (thẳng hoặc qua milestone), theo id. */
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> listByGoal(UUID userId, UUID goalId, Integer limit, String cursor) {
        if (!goalLinks.goalExists(userId, goalId)) {
            throw new NotFoundException(ErrorCode.GOAL_NOT_FOUND);
        }
        int size = com.lifeos.common.web.Cursors.limit(limit);
        com.lifeos.common.web.Cursors.Position after = com.lifeos.common.web.Cursors.decode(cursor, GOAL_SORT);
        List<com.lifeos.task.entity.Task> rows = tasks.pageByGoal(userId, goalId, after == null,
                after == null ? null : after.id(), org.springframework.data.domain.Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<com.lifeos.task.entity.Task> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            com.lifeos.task.entity.Task last = page.get(page.size() - 1);
            next = com.lifeos.common.web.Cursors.encode(
                    new com.lifeos.common.web.Cursors.Position(GOAL_SORT, last.getId().toString(), last.getId()));
        }
        return new PageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }
}
