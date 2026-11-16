package com.lifeos.task.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * GET /projects/{id}/progress (API_CONVENTIONS §8.5, D79): completionRatio là số thập phân 0–1, làm tròn 4 chữ số
 * HALF_UP; không có Task → 0.
 */
public record ProjectProgressResponse(UUID projectId, long totalTasks, long completedTasks, long openTasks,
        BigDecimal completionRatio) {
}
