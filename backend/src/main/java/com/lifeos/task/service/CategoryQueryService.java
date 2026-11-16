package com.lifeos.task.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.web.Cursors;
import com.lifeos.task.dto.CategoryResponse;
import com.lifeos.task.dto.CategorySyncState;
import com.lifeos.task.entity.Category;
import com.lifeos.task.mapper.TaskMapper;
import com.lifeos.task.repository.CategoryRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc Category của user hiện tại (API_CONVENTIONS §7, §8.6; D79). Chỉ bản ghi chưa xóa. */
@Service
public class CategoryQueryService {

    /** Kiểu sắp xếp cố định của /categories (không có tham số sort, D79), ghi vào con trỏ. */
    static final String SORT = "createdAt:desc";

    private final CategoryRepository categories;
    private final TaskMapper mapper;

    public CategoryQueryService(CategoryRepository categories, TaskMapper mapper) {
        this.categories = categories;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(UUID userId, UUID categoryId) {
        return categories.findActive(categoryId, userId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    /**
     * Trạng thái hiện tại của các Category thuộc userId, KỂ CẢ tombstone (snapshot cho sync, SYNC_PROTOCOL §6). Một truy
     * vấn theo lô; id không thuộc user không được trả.
     */
    @Transactional(readOnly = true)
    public List<CategorySyncState> syncStates(UUID userId, Collection<UUID> categoryIds) {
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return categories.findAllOwned(userId, categoryIds).stream()
                .map(c -> new CategorySyncState(mapper.toResponse(c), c.isDeleted()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> list(UUID userId, Integer limit, String cursor) {
        int size = Cursors.limit(limit);
        Cursors.Position after = Cursors.decode(cursor, SORT);
        Instant afterCreatedAt = null;
        if (after != null) {
            try {
                afterCreatedAt = Instant.parse(after.key());
            } catch (DateTimeParseException | NullPointerException e) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "cursor", "INVALID_VALUE",
                        "Con trỏ phân trang không hợp lệ.");
            }
        }
        List<Category> rows = categories.page(userId, after == null, afterCreatedAt,
                after == null ? null : after.id(), Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<Category> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            Category last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(SORT, last.getCreatedAt().toString(), last.getId()));
        }
        return new PageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }
}
