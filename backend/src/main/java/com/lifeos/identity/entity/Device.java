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
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Thiết bị đã đăng nhập (SECURITY_RULES §5, D31). id do server sinh, client lưu và gửi lại khi đăng nhập sau.
 * name/platform/appVersion do client cung cấp: chỉ để hiển thị, không tin cậy. Thu hồi = đặt revoked_at (giữ bản ghi
 * để hiển thị lịch sử, DATABASE_DESIGN §7).
 */
@Entity
@Table(name = "devices")
@EntityListeners(AuditingEntityListener.class)
public class Device implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "app_version", length = 32)
    private String appVersion;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew;

    protected Device() {
    }

    public static Device register(UUID id, UUID userId, String name, DevicePlatform platform, String appVersion,
            Instant now) {
        Device device = new Device();
        device.id = Objects.requireNonNull(id);
        device.userId = Objects.requireNonNull(userId);
        device.name = Objects.requireNonNull(name);
        device.platform = Objects.requireNonNull(platform);
        device.appVersion = appVersion;
        device.lastSeenAt = Objects.requireNonNull(now);
        device.isNew = true;
        return device;
    }

    /** Đăng nhập lại trên thiết bị đã biết: cập nhật thông tin hiển thị và lần hoạt động cuối (FR-ID-09). */
    public void signedInAgain(String name, DevicePlatform platform, String appVersion, Instant now) {
        if (isRevoked()) {
            throw new IllegalStateException("Thiết bị đã bị thu hồi");
        }
        this.name = Objects.requireNonNull(name);
        this.platform = Objects.requireNonNull(platform);
        this.appVersion = appVersion;
        this.lastSeenAt = Objects.requireNonNull(now);
    }

    /** Thu hồi; gọi lại trên thiết bị đã thu hồi không đổi thời điểm cũ. @return true nếu lần này mới thu hồi */
    public boolean revoke(Instant now) {
        if (isRevoked()) {
            return false;
        }
        this.revokedAt = Objects.requireNonNull(now);
        return true;
    }

    public boolean isRevoked() {
        return revokedAt != null;
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

    public String getName() {
        return name;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Device other && id != null && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return Device.class.hashCode();
    }

    @Override
    public String toString() {
        return "Device[id=" + id + "]";
    }
}
