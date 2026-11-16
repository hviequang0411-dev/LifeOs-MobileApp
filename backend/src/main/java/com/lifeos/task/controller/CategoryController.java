package com.lifeos.task.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.task.dto.CategoryResponse;
import com.lifeos.task.dto.CreateCategoryRequest;
import com.lifeos.task.dto.UpdateCategoryRequest;
import com.lifeos.task.service.CategoryCommandService;
import com.lifeos.task.service.CategoryCommands;
import com.lifeos.task.service.CategoryQueryService;
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

/** Category của user hiện tại (API_CONVENTIONS §8.6). userId luôn lấy từ CurrentUser. */
@RestController
@RequestMapping("/api/v1/categories")
class CategoryController {

    private final CurrentUser currentUser;
    private final CategoryCommandService commands;
    private final CategoryQueryService queries;

    CategoryController(CurrentUser currentUser, CategoryCommandService commands, CategoryQueryService queries) {
        this.currentUser = currentUser;
        this.commands = commands;
        this.queries = queries;
    }

    @ApiResponse(responseCode = "201", description = "Đã tạo",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CategoryResponse.class)))
    @PostMapping
    ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse created = commands.create(currentUser.userId(),
                new CategoryCommands.Create(request.id(), request.name()));
        return ResponseEntity.created(URI.create("/api/v1/categories/" + created.id())).body(created);
    }

    @GetMapping
    PageResponse<CategoryResponse> list(@RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.list(currentUser.userId(), limit, cursor);
    }

    @GetMapping("/{id}")
    CategoryResponse get(@PathVariable UUID id) {
        return queries.get(currentUser.userId(), id);
    }

    @PatchMapping("/{id}")
    CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return commands.update(currentUser.userId(), id,
                new CategoryCommands.Update(request.version(), request.name()));
    }

    /** 204; version là tham số truy vấn bắt buộc (D62). */
    @ApiResponse(responseCode = "204", description = "Đã xóa (tombstone)")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestParam long version) {
        commands.delete(currentUser.userId(), id, version);
        return ResponseEntity.noContent().build();
    }
}
