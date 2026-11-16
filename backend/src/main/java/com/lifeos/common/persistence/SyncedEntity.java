package com.lifeos.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Cột chuẩn của entity đồng bộ (DATABASE_RULES §8.2, D80): id (UUID gán trước khi lưu), user_id, version (do
 * Hibernate @Version quản lý, BẮT ĐẦU TỪ 1), deleted_at (tombstone), created_at/updated_at (JPA auditing + Clock).
 * <p>
 * version bắt đầu từ 1: Hibernate 7 tự khởi tạo version = 0 khi version null, còn version khác null lúc persist làm
 * entity bị coi là detached. Vì vậy version để null khi tạo đối tượng và chỉ được đặt 1 trong @PrePersist — callback
 * chạy SAU khi Hibernate xác định entity mới và TRƯỚC khi khởi tạo version (giá trị khác null được giữ nguyên).
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class SyncedEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** id do ứng dụng gán nên Spring Data không tự biết bản ghi mới: dùng persist thay vì merge khi tạo. */
    @Transient
    private boolean isNew;

    protected SyncedEntity() {
    }

    protected SyncedEntity(UUID id, UUID userId) {
        this.id = Objects.requireNonNull(id, "id");
        this.userId = Objects.requireNonNull(userId, "userId");
        this.isNew = true;
    }

    @PrePersist
    void initialVersion() {
        if (version == null) {
            version = 1L;
        }
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        isNew = false;
    }

    /** Xóa mềm (tombstone, DATABASE_RULES §8.3). Hibernate tăng version khi flush. */
    public void softDelete(Instant now) {
        if (deletedAt != null) {
            throw new IllegalStateException("Bản ghi đã bị xóa");
        }
        deletedAt = Objects.requireNonNull(now);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public UUID getUserId() {
        return userId;
    }

    /** version hiện tại; 0 nếu chưa từng được lưu. */
    public long getVersion() {
        return version == null ? 0 : version;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || (o != null && o.getClass() == getClass() && id != null
                && id.equals(((SyncedEntity) o).id));
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + id + ", version=" + version + "]";
    }
}
