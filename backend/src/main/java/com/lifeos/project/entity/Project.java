package com.lifeos.project.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Dự án (FR-PJ-01..03): entity đồng bộ, id do client tạo. Giá trị (tên đã trim, độ dài…) được kiểm ở
 * ProjectCommandService trước khi gọi các method ở đây.
 */
@Entity
@Table(name = "projects")
public class Project extends SyncedEntity {

    /** Giá trị entity_type trong change_logs / sync. */
    public static final String ENTITY_TYPE = "project";

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected Project() {
    }

    private Project(UUID id, UUID userId) {
        super(id, userId);
    }

    public static Project create(UUID id, UUID userId, String name, String description) {
        Project project = new Project(id, userId);
        project.name = Objects.requireNonNull(name);
        project.description = description;
        return project;
    }

    /** @return true nếu tên đổi giá trị */
    public boolean rename(String newName) {
        Objects.requireNonNull(newName);
        if (newName.equals(name)) {
            return false;
        }
        name = newName;
        return true;
    }

    /** null = xóa mô tả. @return true nếu mô tả đổi giá trị */
    public boolean describe(String newDescription) {
        if (Objects.equals(newDescription, description)) {
            return false;
        }
        description = newDescription;
        return true;
    }

    /** FR-PJ-02. Đã lưu trữ thì không làm gì (D78). @return true nếu lần này mới lưu trữ */
    public boolean archive(Instant now) {
        if (archivedAt != null) {
            return false;
        }
        archivedAt = Objects.requireNonNull(now);
        return true;
    }

    /** @return true nếu lần này mới bỏ lưu trữ */
    public boolean unarchive() {
        if (archivedAt == null) {
            return false;
        }
        archivedAt = null;
        return true;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
