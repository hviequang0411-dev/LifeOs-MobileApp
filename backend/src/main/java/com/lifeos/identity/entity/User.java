package com.lifeos.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Tài khoản (bảng users). Không phải entity đồng bộ (không có version/deleted_at). id do server sinh. email luôn được
 * lưu ở dạng đã chuẩn hóa (trim + lowercase). Không có setter công khai: trạng thái đổi qua method nghiệp vụ.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** id do ứng dụng gán nên Spring Data không tự biết bản ghi mới: tránh merge (SELECT thừa) khi tạo. */
    @Transient
    private boolean isNew;

    protected User() {
    }

    public static User register(UUID id, String normalizedEmail, String passwordHash, String displayName) {
        User user = new User();
        user.id = Objects.requireNonNull(id);
        user.email = Objects.requireNonNull(normalizedEmail);
        user.passwordHash = Objects.requireNonNull(passwordHash);
        user.displayName = Objects.requireNonNull(displayName);
        user.status = UserStatus.ACTIVE;
        user.isNew = true;
        return user;
    }

    /** Xác minh email; gọi lại khi đã xác minh không đổi thời điểm cũ. */
    public void markEmailVerified(Instant at) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = Objects.requireNonNull(at);
        }
    }

    /** Thay hash mật khẩu (băm lại khi tham số Argon2 đổi; đổi/đặt lại mật khẩu ở bước 1d). */
    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash);
    }

    /** PATCH /me (FR-ID-07). Tên đã được kiểm tra (không rỗng, ≤ 100 ký tự). */
    public void changeDisplayName(String displayName) {
        this.displayName = Objects.requireNonNull(displayName);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        isNew = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof User other && id != null && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return User.class.hashCode();
    }

    @Override
    public String toString() {
        return "User[id=" + id + "]";
    }
}
