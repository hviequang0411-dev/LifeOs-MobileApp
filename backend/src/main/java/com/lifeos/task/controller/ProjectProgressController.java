package com.lifeos.task.controller;

import com.lifeos.common.security.CurrentUser;
import com.lifeos.task.dto.ProjectProgressResponse;
import com.lifeos.task.service.TaskQueryService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /projects/{id}/progress (API_CONVENTIONS §8.5, FR-PJ-05): do module task phục vụ vì tiến độ là số liệu về Task
 * (D2: task → project một chiều). userId luôn lấy từ CurrentUser.
 */
@RestController
class ProjectProgressController {

    private final CurrentUser currentUser;
    private final TaskQueryService queries;

    ProjectProgressController(CurrentUser currentUser, TaskQueryService queries) {
        this.currentUser = currentUser;
        this.queries = queries;
    }

    @GetMapping("/api/v1/projects/{id}/progress")
    ProjectProgressResponse progress(@PathVariable UUID id) {
        return queries.progress(currentUser.userId(), id);
    }
}
