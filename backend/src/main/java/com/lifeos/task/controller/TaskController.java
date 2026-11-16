package com.lifeos.task.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.web.VersionRequest;
import com.lifeos.task.dto.CreateTaskRequest;
import com.lifeos.task.dto.TaskResponse;
import com.lifeos.task.dto.UpdateTaskRequest;
import com.lifeos.task.service.TaskCommandService;
import com.lifeos.task.service.TaskCommands;
import com.lifeos.task.service.TaskListQuery;
import com.lifeos.task.service.TaskQueryService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Task của user hiện tại (API_CONVENTIONS §7, §8.4). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1/tasks")
class TaskController {

    private final CurrentUser currentUser;
    private final TaskCommandService commands;
    private final TaskQueryService queries;

    TaskController(CurrentUser currentUser, TaskCommandService commands, TaskQueryService queries) {
        this.currentUser = currentUser;
        this.commands = commands;
        this.queries = queries;
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TaskResponse.class)))
    @PostMapping
    ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = commands.create(currentUser.userId(), new TaskCommands.Create(request.id(),
                request.title(), request.description(), request.status(), request.priority(), request.dueDate(),
                request.dueTime(), request.projectId(), request.categoryId(), request.goalId(),
                request.milestoneId()));
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.id())).body(created);
    }

    /** GET /tasks: lọc, sắp xếp, phân trang theo con trỏ (API_CONVENTIONS §7). */
    @GetMapping
    PageResponse<TaskResponse> list(@RequestParam(required = false) String status,
            @RequestParam(required = false) UUID projectId, @RequestParam(required = false) Boolean inbox,
            @RequestParam(required = false) UUID categoryId, @RequestParam(required = false) String priority,
            @RequestParam(required = false) String dueFrom, @RequestParam(required = false) String dueTo,
            @RequestParam(required = false) String sort, @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.list(currentUser.userId(), new TaskListQuery(status, projectId, inbox, categoryId, priority,
                dueFrom, dueTo, sort, limit, cursor));
    }

    @GetMapping("/{id}")
    TaskResponse get(@PathVariable UUID id) {
        return queries.get(currentUser.userId(), id);
    }

    @PatchMapping("/{id}")
    TaskResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
        return commands.update(currentUser.userId(), id, new TaskCommands.Update(request.version(),
                request.title(), request.description(), request.status(), request.priority(), request.dueDate(),
                request.dueTime(), request.projectId(), request.categoryId(), request.goalId(),
                request.milestoneId()));
    }

    @PostMapping("/{id}/complete")
    TaskResponse complete(@PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
        return commands.complete(currentUser.userId(), id, request.version());
    }

    @PostMapping("/{id}/reopen")
    TaskResponse reopen(@PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
        return commands.reopen(currentUser.userId(), id, request.version());
    }

    /** 204; version là tham số truy vấn bắt buộc (D62). */
    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {
        commands.delete(currentUser.userId(), id, version);
        return ResponseEntity.noContent().build();
    }
}
