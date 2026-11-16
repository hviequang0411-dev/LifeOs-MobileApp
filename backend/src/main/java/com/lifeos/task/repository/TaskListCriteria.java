package com.lifeos.task.repository;

import com.lifeos.task.entity.TaskPriority;
import com.lifeos.task.entity.TaskStatus;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Điều kiện danh sách Task đã được kiểm tra (API_CONVENTIONS §7, D59, D79). Mọi trường lọc null = không lọc.
 *
 * @param inbox      true = chưa thuộc Project; false = thuộc một Project; null = không lọc
 * @param sortField  dueDate | priority | createdAt | updatedAt
 * @param ascending  chiều sắp xếp; id (tiêu chí phụ) cùng chiều
 * @param afterKey   khóa sắp xếp của bản ghi cuối trang trước (đã chuyển kiểu: LocalDate, Integer hạng ưu tiên,
 *                   Instant) — null khi trang đầu, hoặc khi bản ghi cuối có dueDate null
 * @param afterId    id của bản ghi cuối trang trước; null = trang đầu
 */
public record TaskListCriteria(UUID userId, TaskStatus status, UUID projectId, Boolean inbox, UUID categoryId,
        TaskPriority priority, LocalDate dueFrom, LocalDate dueTo, String sortField, boolean ascending,
        Object afterKey, UUID afterId, int limit) {
}
