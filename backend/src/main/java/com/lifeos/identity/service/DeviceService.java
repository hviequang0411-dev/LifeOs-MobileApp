package com.lifeos.identity.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.security.RevokedDeviceRegistry;
import com.lifeos.common.security.SecurityEvent;
import com.lifeos.common.security.SecurityEventLogger;
import com.lifeos.identity.dto.DeviceListResponse;
import com.lifeos.identity.entity.Device;
import com.lifeos.identity.entity.DevicePlatform;
import com.lifeos.identity.mapper.IdentityMapper;
import com.lifeos.identity.repository.DeviceRepository;
import com.lifeos.identity.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Thiết bị (UC-ID-09, FR-ID-09/10; SECURITY_RULES §4.4, §5). Thiết bị của user khác → 404 giống hệt thiết bị không
 * tồn tại (IDOR). Thu hồi: đặt revoked_at, thu hồi mọi refresh token của thiết bị, và SAU KHI commit đưa thiết bị vào
 * tập thu hồi trong bộ nhớ để access token còn hạn bị từ chối ngay (D45).
 */
@Service
public class DeviceService {

    private final DeviceRepository devices;
    private final RefreshTokenRepository refreshTokens;
    private final RevokedDeviceRegistry revokedDevices;
    private final SecurityEventLogger securityEvents;
    private final IdentityMapper mapper;
    private final Clock clock;
    private final java.util.List<com.lifeos.common.device.DeviceLifecycleListener> lifecycle;
    private final java.util.List<com.lifeos.common.device.PushTokenLookup> pushTokens;

    public DeviceService(DeviceRepository devices, RefreshTokenRepository refreshTokens,
            RevokedDeviceRegistry revokedDevices, SecurityEventLogger securityEvents, IdentityMapper mapper,
            Clock clock, java.util.List<com.lifeos.common.device.DeviceLifecycleListener> lifecycle,
            java.util.List<com.lifeos.common.device.PushTokenLookup> pushTokens) {
        this.devices = devices;
        this.refreshTokens = refreshTokens;
        this.revokedDevices = revokedDevices;
        this.securityEvents = securityEvents;
        this.mapper = mapper;
        this.clock = clock;
        this.lifecycle = lifecycle;
        this.pushTokens = pushTokens;
    }

    /**
     * Chọn thiết bị cho lần đăng nhập (D31): dùng lại deviceId client gửi nếu thuộc user và chưa bị thu hồi; ngược lại
     * tạo thiết bị mới. Tham gia transaction đăng nhập của bên gọi.
     */
    @Transactional
    public Device resolveForLogin(UUID userId, UUID requestedDeviceId, DeviceInfo info) {
        Instant now = clock.instant();
        if (requestedDeviceId != null) {
            Device existing = devices.findByIdAndUserId(requestedDeviceId, userId).orElse(null);
            if (existing != null && !existing.isRevoked()) {
                existing.signedInAgain(info.name(), DevicePlatform.valueOf(info.platform()), info.appVersion(), now);
                return devices.saveAndFlush(existing);
            }
        }
        return devices.saveAndFlush(Device.register(UUID.randomUUID(), userId, info.name(),
                DevicePlatform.valueOf(info.platform()),
                info.appVersion(), now));
    }

    @Transactional(readOnly = true)
    public DeviceListResponse list(UUID userId, UUID currentDeviceId) {
        java.util.Map<UUID, String> suffixes = new java.util.HashMap<>();
        pushTokens.forEach(lookup -> suffixes.putAll(lookup.tokenSuffixes(userId)));
        return new DeviceListResponse(devices.findByUserIdOrderByCreatedAtDescIdAsc(userId).stream()
                .map(d -> mapper.toResponse(d, d.getId().equals(currentDeviceId), suffixes.get(d.getId())))
                .toList());
    }

    /** DELETE /devices/{id}. Thu hồi chính thiết bị đang dùng được phép; gọi lại trên thiết bị đã thu hồi → 204. */
    @Transactional
    public void revoke(UUID userId, UUID currentDeviceId, UUID deviceId, String clientIp) {
        Device device = devices.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.DEVICE_NOT_FOUND));
        Instant now = clock.instant();
        boolean newlyRevoked = device.revoke(now);
        refreshTokens.revokeByDevice(userId, deviceId, now);
        // D139: gỡ token push của thiết bị trong cùng transaction.
        signedOut(userId, deviceId);
        if (newlyRevoked) {
            Instant revokedAt = device.getRevokedAt();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    revokedDevices.revoke(deviceId, revokedAt);
                }
            });
            securityEvents.log(SecurityEvent.DEVICE_REVOKED, userId, currentDeviceId, clientIp);
        }
    }

    /** Thông báo các module (notification) rằng thiết bị đã đăng xuất/bị thu hồi; gọi trong transaction đang mở. */
    public void signedOut(UUID userId, UUID deviceId) {
        lifecycle.forEach(listener -> listener.onDeviceSignedOut(userId, deviceId));
    }
}
