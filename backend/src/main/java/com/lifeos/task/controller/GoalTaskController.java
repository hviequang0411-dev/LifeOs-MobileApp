package com.lifeos.task.controller;

import com.lifeos.common.response.PageResponse;
import com.lifeos.common.security.CurrentUser;
import com.lifeos.task.dto.TaskResponse;
import com.lifeos.task.service.TaskQueryService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GET /goals/{id}/tasks: Task gắn Goal (D186). Goal không có/của user khác → 404 GOAL_NOT_FOUND. */
@RestController
class GoalTaskController {

    private final CurrentUser currentUser;
    private final TaskQueryService queries;

    GoalTaskController(CurrentUser currentUser, TaskQueryService queries) {
        this.currentUser = currentUser;
        this.queries = queries;
    }

    @GetMapping("/api/v1/goals/{id}/tasks")
    PageResponse<TaskResponse> list(@PathVariable UUID id, @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor) {
        return queries.listByGoal(currentUser.userId(), id, limit, cursor);
    }
}
