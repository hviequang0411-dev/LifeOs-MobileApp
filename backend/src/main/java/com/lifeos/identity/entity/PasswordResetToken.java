package com.lifeos.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Token đặt lại mật khẩu (SECURITY_RULES §3.4): chỉ lưu SHA-256 hex, dùng một lần, hạn 1 giờ. Đánh dấu đã dùng bằng
 * UPDATE có điều kiện ở repository.
 */
@Entity
@Table(name = "password_reset_tokens")
@EntityListeners(AuditingEntityListener.class)
public class PasswordResetToken implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "token_hash", nullable = false, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew;

    protected PasswordResetToken() {
    }

    public static PasswordResetToken issue(UUID id, UUID userId, String tokenHash, Instant expiresAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.id = Objects.requireNonNull(id);
        token.userId = Objects.requireNonNull(userId);
        token.tokenHash = Objects.requireNonNull(tokenHash);
        token.expiresAt = Objects.requireNonNull(expiresAt);
        token.isNew = true;
        return token;
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

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof PasswordResetToken other && id != null && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return PasswordResetToken.class.hashCode();
    }

    @Override
    public String toString() {
        return "PasswordResetToken[id=" + id + "]";
    }
}
