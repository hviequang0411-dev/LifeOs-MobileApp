package com.lifeos.task.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.task.dto.CategoryResponse;
import com.lifeos.task.entity.Category;
import com.lifeos.task.mapper.TaskMapper;
import com.lifeos.task.repository.CategoryRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi Category (UC-CT-01; API_CONVENTIONS §8.6; D25, D58, D62, D78). Mỗi thay đổi thật gọi ChangeRecorder đúng
 * một lần trong cùng transaction (A9). Tên duy nhất theo user không phân biệt hoa thường, bỏ qua bản đã xóa: kiểm trước
 * bằng lower() của DB, và nếu hai request đồng thời cùng vượt qua bước kiểm thì vi phạm ux_categories_user_name được
 * bắt SAU khi transaction rollback và trả cùng 409 CATEGORY_NAME_TAKEN.
 * <p>
 * Xóa Category gỡ category_id khỏi các Task còn sống trong cùng transaction (SYSTEM_ARCHITECTURE §3.8, D20); mỗi
 * Task bị ảnh hưởng có change_logs UPDATE riêng, đứng trước change_logs DELETE của Category.
 */
@Service
public class CategoryCommandService {

    static final int NAME_MAX = 50;
    private static final String PK_CONSTRAINT = "pk_categories";
    private static final String NAME_CONSTRAINT = "ux_categories_user_name";

    private final CategoryRepository categories;
    private final TaskCommandService taskCommands;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final TaskMapper mapper;
    private final Clock clock;

    public CategoryCommandService(CategoryRepository categories, TaskCommandService taskCommands,
            ChangeRecorder changeRecorder, VersionedWrites versionedWrites, TaskMapper mapper, Clock clock) {
        this.categories = categories;
        this.taskCommands = taskCommands;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** POST /categories (REST). */
    public CategoryResponse create(UUID userId, CategoryCommands.Create cmd) {
        return create(userId, cmd, null);
    }

    /**
     * Tạo Category. Thứ tự D96: id đã tồn tại (của ai, kể cả tombstone) → 409 ENTITY_ALREADY_EXISTS → giá trị (400) →
     * tên trùng → 409 CATEGORY_NAME_TAKEN.
     *
     * @param mutationIdOrNull mutationId nếu đến từ sync; null nếu từ REST
     */
    public CategoryResponse create(UUID userId, CategoryCommands.Create cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (categories.existsById(cmd.id())) {
                    throw new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
                }
                String name = validName(cmd.name().value());
                requireNameFree(userId, name, cmd.id());
                Category saved = categories.saveAndFlush(Category.create(cmd.id(), userId, name));
                changeRecorder.record(userId, Category.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            throw translateUniqueViolation(e);
        }
    }

    /** PATCH /categories/{id} (REST). */
    public CategoryResponse update(UUID userId, UUID categoryId, CategoryCommands.Update cmd) {
        return update(userId, categoryId, cmd, null);
    }

    /**
     * Sửa Category. Thứ tự D96: tồn tại (404) → version (409) → giá trị (400) → tên trùng (409). Đổi riêng kiểu chữ
     * của chính tên mình được phép. Không đổi gì: không tăng version, không ghi change_logs.
     */
    public CategoryResponse update(UUID userId, UUID categoryId, CategoryCommands.Update cmd,
            UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                Category category = categories.findActive(categoryId, userId)
                        .orElseThrow(CategoryCommandService::notFound);
                VersionedWrites.requireVersion(category, cmd.expectedVersion());
                String name = cmd.name().present() ? validName(cmd.name().value()) : null;
                if (name == null || name.equals(category.getName())) {
                    return mapper.toResponse(category);
                }
                requireNameFree(userId, name, categoryId);
                category.rename(name);
                Category saved = categories.saveAndFlush(category);
                changeRecorder.record(userId, Category.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> currentVersion(userId, categoryId));
        } catch (DataIntegrityViolationException e) {
            throw translateUniqueViolation(e);
        }
    }

    /** DELETE /categories/{id}?version=n (REST). */
    public long delete(UUID userId, UUID categoryId, long expectedVersion) {
        return delete(userId, categoryId, expectedVersion, null);
    }

    /**
     * Xóa Category (D80, D86). Khóa Category (FOR UPDATE) → kiểm version → xóa mềm và flush → gỡ category_id khỏi Task
     * còn sống (flush rồi mỗi Task một change_logs UPDATE, mutation_id NULL) → change_logs DELETE của Category, sau
     * cùng. Đã xóa hoặc không tồn tại → 404 (D78). Sau khi xóa, tên được dùng lại (partial unique index).
     *
     * @return version của Category sau khi xóa (tombstone)
     */
    public long delete(UUID userId, UUID categoryId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            Category category = categories.findActiveForUpdate(categoryId, userId)
                    .orElseThrow(CategoryCommandService::notFound);
            VersionedWrites.requireVersion(category, expectedVersion);
            category.softDelete(clock.instant());
            Category saved = categories.saveAndFlush(category);
            taskCommands.detachFromCategory(userId, categoryId);
            changeRecorder.record(userId, Category.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, categoryId));
    }

    private void requireNameFree(UUID userId, String name, UUID excludeId) {
        if (categories.nameTaken(userId, name, excludeId)) {
            throw new ConflictException(ErrorCode.CATEGORY_NAME_TAKEN);
        }
    }

    private long currentVersion(UUID userId, UUID categoryId) {
        return categories.findVersion(categoryId, userId).orElseThrow(CategoryCommandService::notFound);
    }

    /** Trim rồi kiểm 1–50 ký tự (D35, D78). */
    static String validName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "name", "REQUIRED", "Tên danh mục là bắt buộc.");
        }
        if (name.codePointCount(0, name.length()) > NAME_MAX) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "name", "SIZE",
                    "Tên danh mục tối đa " + NAME_MAX + " ký tự.");
        }
        return name;
    }

    private static RuntimeException translateUniqueViolation(DataIntegrityViolationException e) {
        if (mentions(e, NAME_CONSTRAINT)) {
            return new ConflictException(ErrorCode.CATEGORY_NAME_TAKEN);
        }
        if (mentions(e, PK_CONSTRAINT)) {
            return new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
        }
        return e;
    }

    private static boolean mentions(Throwable e, String constraint) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }

    private static NotFoundException notFound() {
        return new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND);
    }
}
