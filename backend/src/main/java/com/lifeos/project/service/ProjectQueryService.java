package com.lifeos.project.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.response.PageResponse;
import com.lifeos.common.web.Cursors;
import com.lifeos.project.dto.ProjectResponse;
import com.lifeos.project.dto.ProjectSyncState;
import com.lifeos.project.entity.Project;
import com.lifeos.project.mapper.ProjectMapper;
import com.lifeos.project.repository.ProjectRepository;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đọc Project của user hiện tại (API_CONVENTIONS §7, §8.5; D59, D79). Chỉ bản ghi chưa xóa. */
@Service
public class ProjectQueryService {

    /** Tên kiểu sắp xếp cố định của /projects (không có tham số sort, D79), ghi vào con trỏ. */
    static final String SORT = "createdAt:desc";
    private static final Set<String> ARCHIVED_MODES = Set.of("false", "true", "all");

    private final ProjectRepository projects;
    private final ProjectMapper mapper;

    public ProjectQueryService(ProjectRepository projects, ProjectMapper mapper) {
        this.projects = projects;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID userId, UUID projectId) {
        return projects.findActive(projectId, userId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new NotFoundException(ErrorCode.PROJECT_NOT_FOUND));
    }

    /**
     * Trạng thái hiện tại của các Project thuộc userId, KỂ CẢ tombstone (snapshot cho sync, SYNC_PROTOCOL §6). Một truy
     * vấn theo lô; id không thuộc user không được trả.
     */
    @Transactional(readOnly = true)
    public List<ProjectSyncState> syncStates(UUID userId, Collection<UUID> projectIds) {
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return projects.findAllOwned(userId, projectIds).stream()
                .map(p -> new ProjectSyncState(mapper.toResponse(p), p.isDeleted()))
                .toList();
    }

    /** GET /projects?archived=false|true|all&limit&cursor. Mặc định archived=false (FR-PJ-02). */
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> list(UUID userId, String archived, Integer limit, String cursor) {
        String mode = archived == null ? "false" : archived;
        if (!ARCHIVED_MODES.contains(mode)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "archived", "INVALID_VALUE",
                    "archived phải là false, true hoặc all.");
        }
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
        List<Project> rows = projects.page(userId, mode, after == null, afterCreatedAt,
                after == null ? null : after.id(), Limit.of(size + 1));
        boolean hasMore = rows.size() > size;
        List<Project> page = hasMore ? rows.subList(0, size) : rows;
        String next = null;
        if (hasMore) {
            Project last = page.get(page.size() - 1);
            next = Cursors.encode(new Cursors.Position(SORT, last.getCreatedAt().toString(), last.getId()));
        }
        return new PageResponse<>(page.stream().map(mapper::toResponse).toList(), next, hasMore);
    }
}
