package com.lifeos.project.service;

import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.project.api.ProjectDeletionStarted;
import com.lifeos.project.dto.ProjectResponse;
import com.lifeos.project.entity.Project;
import com.lifeos.project.mapper.ProjectMapper;
import com.lifeos.project.repository.ProjectRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Lệnh ghi Project (UC-PJ-01, 03, 04; API_CONVENTIONS §8.5; D58, D62, D78, D80). Mỗi thay đổi thật gọi ChangeRecorder
 * đúng một lần trong cùng transaction (A9). Ghi qua VersionedWrites: xung đột version dù phát hiện ở bước kiểm tường
 * minh hay ở @Version của Hibernate đều ra 409 ENTITY_VERSION_CONFLICT kèm currentVersion.
 * <p>
 * Quy tắc giá trị nằm ở đây (không chỉ ở DTO) vì sync push ở Giai đoạn 3 gọi cùng service (SECURITY_RULES §8.3).
 */
@Service
public class ProjectCommandService {

    static final int NAME_MAX = 100;
    static final int DESCRIPTION_MAX = 10_000;
    private static final String PK_CONSTRAINT = "pk_projects";

    private final ProjectRepository projects;
    private final ChangeRecorder changeRecorder;
    private final VersionedWrites versionedWrites;
    private final ApplicationEventPublisher events;
    private final ProjectMapper mapper;
    private final Clock clock;

    public ProjectCommandService(ProjectRepository projects, ChangeRecorder changeRecorder,
            VersionedWrites versionedWrites, ApplicationEventPublisher events, ProjectMapper mapper, Clock clock) {
        this.projects = projects;
        this.changeRecorder = changeRecorder;
        this.versionedWrites = versionedWrites;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** POST /projects (REST: không có mutationId). */
    public ProjectResponse create(UUID userId, ProjectCommands.Create cmd) {
        return create(userId, cmd, null);
    }

    /**
     * Tạo Project. id do client cấp; id đã tồn tại — của chính user, của user khác, hay của tombstone — đều trả 409
     * ENTITY_ALREADY_EXISTS giống hệt nhau (D36, D62, D78) và được kiểm TRƯỚC validation giá trị (D96). Hai request
     * đồng thời cùng id: vi phạm khóa chính được bắt và trả cùng lỗi.
     *
     * @param mutationIdOrNull mutationId nếu đến từ sync (ghi vào change_logs, SYNC_PROTOCOL §3); null nếu từ REST
     */
    public ProjectResponse create(UUID userId, ProjectCommands.Create cmd, UUID mutationIdOrNull) {
        try {
            return versionedWrites.write(() -> {
                if (projects.existsById(cmd.id())) {
                    throw alreadyExists();
                }
                String name = validName(cmd.name().value());
                String description = validDescription(cmd.description().value());
                Project saved = projects.saveAndFlush(Project.create(cmd.id(), userId, name, description));
                changeRecorder.record(userId, Project.ENTITY_TYPE, saved.getId(), ChangeOperation.CREATE,
                        saved.getVersion(), mutationIdOrNull);
                return mapper.toResponse(saved);
            }, () -> 0L);
        } catch (DataIntegrityViolationException e) {
            if (mentions(e, PK_CONSTRAINT)) {
                throw alreadyExists();
            }
            throw e;
        }
    }

    /** PATCH /projects/{id} (REST). */
    public ProjectResponse update(UUID userId, UUID projectId, ProjectCommands.Update cmd) {
        return update(userId, projectId, cmd, null);
    }

    /**
     * Sửa Project theo JSON Merge Patch. Thứ tự D96: tồn tại (404) → version (409) → giá trị (400) → áp dụng. Không đổi
     * giá trị nào (kể cả archived đã đúng giá trị): không tăng version, không ghi change_logs (D78, SYNC_PROTOCOL §4.4).
     */
    public ProjectResponse update(UUID userId, UUID projectId, ProjectCommands.Update cmd, UUID mutationIdOrNull) {
        return writeExisting(userId, projectId, cmd.expectedVersion(), mutationIdOrNull, project -> {
            String name = cmd.name().present() ? validName(cmd.name().value()) : null;
            String description = cmd.description().present() ? validDescription(cmd.description().value()) : null;
            if (cmd.archived().isExplicitNull()) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "archived", "REQUIRED",
                        "Trạng thái lưu trữ là bắt buộc.");
            }
            boolean changed = name != null && project.rename(name);
            if (cmd.description().present()) {
                changed |= project.describe(description);
            }
            if (cmd.archived().present()) {
                changed |= cmd.archived().value() ? project.archive(clock.instant()) : project.unarchive();
            }
            return changed;
        });
    }

    /** POST /projects/{id}/archive. Đã lưu trữ: kiểm version rồi 200, không đổi gì (D78). */
    public ProjectResponse archive(UUID userId, UUID projectId, long expectedVersion) {
        return writeExisting(userId, projectId, expectedVersion, null, project -> project.archive(clock.instant()));
    }

    /** POST /projects/{id}/unarchive. Chưa lưu trữ: kiểm version rồi 200, không đổi gì (D78). */
    public ProjectResponse unarchive(UUID userId, UUID projectId, long expectedVersion) {
        return writeExisting(userId, projectId, expectedVersion, null, Project::unarchive);
    }

    /** DELETE /projects/{id}?version=n (REST). */
    public long delete(UUID userId, UUID projectId, long expectedVersion) {
        return delete(userId, projectId, expectedVersion, null);
    }

    /**
     * Xóa Project (UC-PJ-04, SYSTEM_ARCHITECTURE §3.8, D80, D86). Trong MỘT transaction: khóa Project (FOR UPDATE) →
     * kiểm version → xóa mềm và flush → phát ProjectDeletionStarted (module task gỡ project_id khỏi Task còn sống,
     * flush rồi ghi change_logs từng Task với mutation_id NULL) → change_logs DELETE của Project, sau cùng. Mọi khóa/ghi
     * nghiệp vụ đều xảy ra trước lần gọi ChangeRecorder đầu tiên. Đã xóa hoặc không tồn tại → 404 (D78).
     *
     * @return version của Project sau khi xóa (tombstone)
     */
    public long delete(UUID userId, UUID projectId, long expectedVersion, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> {
            Project project = projects.findActiveForUpdate(projectId, userId).orElseThrow(ProjectCommandService::notFound);
            VersionedWrites.requireVersion(project, expectedVersion);
            project.softDelete(clock.instant());
            Project saved = projects.saveAndFlush(project);
            events.publishEvent(new ProjectDeletionStarted(userId, projectId));
            changeRecorder.record(userId, Project.ENTITY_TYPE, saved.getId(), ChangeOperation.DELETE,
                    saved.getVersion(), mutationIdOrNull);
            return saved.getVersion();
        }, () -> currentVersion(userId, projectId));
    }

    /**
     * Khung chung (D96): tải (404) → kiểm version (409) → kiểm giá trị và áp thay đổi trong {@code change} → nếu đổi
     * thật thì flush (version +1) và ghi log.
     */
    private ProjectResponse writeExisting(UUID userId, UUID projectId, long expectedVersion, UUID mutationIdOrNull,
            Function<Project, Boolean> change) {
        return versionedWrites.write(() -> {
            Project project = projects.findActive(projectId, userId).orElseThrow(ProjectCommandService::notFound);
            VersionedWrites.requireVersion(project, expectedVersion);
            if (!change.apply(project)) {
                return mapper.toResponse(project);
            }
            Project saved = projects.saveAndFlush(project);
            changeRecorder.record(userId, Project.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                    saved.getVersion(), mutationIdOrNull);
            return mapper.toResponse(saved);
        }, () -> currentVersion(userId, projectId));
    }

    private long currentVersion(UUID userId, UUID projectId) {
        return projects.findVersion(projectId, userId).orElseThrow(ProjectCommandService::notFound);
    }

    /** Trim rồi kiểm 1–100 ký tự (D78). */
    static String validName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "name", "REQUIRED", "Tên dự án là bắt buộc.");
        }
        if (name.codePointCount(0, name.length()) > NAME_MAX) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "name", "SIZE",
                    "Tên dự án tối đa " + NAME_MAX + " ký tự.");
        }
        return name;
    }

    /** Giữ nguyên (không trim, D78); null hợp lệ; tối đa 10.000 ký tự. */
    static String validDescription(String description) {
        if (description != null && description.codePointCount(0, description.length()) > DESCRIPTION_MAX) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "description", "SIZE",
                    "Mô tả tối đa " + DESCRIPTION_MAX + " ký tự.");
        }
        return description;
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
        return new NotFoundException(ErrorCode.PROJECT_NOT_FOUND);
    }

    private static ConflictException alreadyExists() {
        return new ConflictException(ErrorCode.ENTITY_ALREADY_EXISTS);
    }
}
