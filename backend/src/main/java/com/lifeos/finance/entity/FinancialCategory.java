package com.lifeos.finance.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Danh mục thu/chi (FR-FIN-03, D106, D113). kind bất biến sau khi tạo; systemKey chỉ để nhận diện danh mục hệ thống. */
@Entity
@Table(name = "financial_categories")
public class FinancialCategory extends SyncedEntity {

    public static final String ENTITY_TYPE = "financial_category";

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "kind", nullable = false, updatable = false, length = 7)
    private String kind;

    @Column(name = "system_key", updatable = false, length = 30)
    private String systemKey;

    @Column(name = "archived_at")
    private Instant archivedAt;

    protected FinancialCategory() {
    }

    public FinancialCategory(UUID id, UUID userId) {
        super(id, userId);
    }

    public static FinancialCategory create(UUID id, UUID userId, String name, String kind, String systemKey) {
        FinancialCategory category = new FinancialCategory(id, userId);
        category.name = name;
        category.kind = kind;
        category.systemKey = systemKey;
        return category;
    }

    /** @return true nếu lần này mới lưu trữ */
    public boolean archive(Instant now) {
        if (archivedAt != null) {
            return false;
        }
        archivedAt = now;
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

    /** @return true nếu giá trị đổi */
    public boolean setName(String value) {
        if (Objects.equals(name, value)) {
            return false;
        }
        name = value;
        return true;
    }

    public String getKind() {
        return kind;
    }

    /** @return true nếu giá trị đổi */
    public boolean setKind(String value) {
        if (Objects.equals(kind, value)) {
            return false;
        }
        kind = value;
        return true;
    }

    public String getSystemKey() {
        return systemKey;
    }

    /** @return true nếu giá trị đổi */
    public boolean setSystemKey(String value) {
        if (Objects.equals(systemKey, value)) {
            return false;
        }
        systemKey = value;
        return true;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }
}
