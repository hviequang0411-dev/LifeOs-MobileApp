package com.lifeos.task.service;

import java.util.UUID;

/**
 * Tham số thô của GET /tasks (API_CONVENTIONS §7) — kiểm tra và chuyển kiểu ở TaskQueryService, lỗi → 400
 * VALIDATION_FAILED kèm fieldError (D79).
 */
public record TaskListQuery(String status, UUID projectId, Boolean inbox, UUID categoryId, String priority,
        String dueFrom, String dueTo, String sort, Integer limit, String cursor) {
}
