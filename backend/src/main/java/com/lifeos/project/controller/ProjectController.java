package com.lifeos.project.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.common.web.VersionRequest;
import com.lifeos.project.dto.CreateProjectRequest;
import com.lifeos.project.dto.ProjectResponse;
import com.lifeos.project.dto.UpdateProjectRequest;
import com.lifeos.project.service.ProjectCommandService;
import com.lifeos.project.service.ProjectCommands;
import com.lifeos.project.service.ProjectQueryService;
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

/** Project của user hiện tại (API_CONVENTIONS §8.5). userId luôn lấy từ CurrentUser. GET /{id}/progress ở module task. */
@RestController
@RequestMapping("/api/v1/projects")
class ProjectController {

    private final CurrentUser currentUser;
    private final ProjectCommandService commands;
    private final ProjectQueryService queries;

    ProjectController(CurrentUser currentUser, ProjectCommandService commands, ProjectQueryService queries) {
        this.currentUser = currentUser;
        this.commands = commands;
        this.queries = queries;
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProjectResponse.class)))
    @PostMapping
    ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse created = commands.create(currentUser.userId(),
                new ProjectCommands.Create(request.id(), request.name(), request.description()));
        return ResponseEntity.created(URI.create("/api/v1/projects/" + created.id())).body(created);
    }

    @GetMapping
    PageResponse<ProjectResponse> list(@RequestParam(required = false) String archived,
            @RequestParam(required = false) Integer limit, @RequestParam(required = false) String cursor) {
        return queries.list(currentUser.userId(), archived, limit, cursor);
    }

    @GetMapping("/{id}")
    ProjectResponse get(@PathVariable UUID id) {
        return queries.get(currentUser.userId(), id);
    }

    @PatchMapping("/{id}")
    ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) {
        return commands.update(currentUser.userId(), id,
                new ProjectCommands.Update(request.version(), request.name(), request.description()));
    }

    @PostMapping("/{id}/archive")
    ProjectResponse archive(@PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
        return commands.archive(currentUser.userId(), id, request.version());
    }

    @PostMapping("/{id}/unarchive")
    ProjectResponse unarchive(@PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
        return commands.unarchive(currentUser.userId(), id, request.version());
    }

    /** 204; version là tham số truy vấn bắt buộc (D62). */
    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {
        commands.delete(currentUser.userId(), id, version);
        return ResponseEntity.noContent().build();
    }
}
