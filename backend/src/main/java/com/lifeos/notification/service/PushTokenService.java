package com.lifeos.notification.service;

import com.lifeos.common.device.DeviceLifecycleListener;
import com.lifeos.common.device.PushTokenLookup;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.identity.api.IdentityDirectory;
import com.lifeos.notification.repository.NotificationStore;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Token push của thiết bị (FR-NTF-08, D139, D141, D147). Thiết bị thuộc identity; token thuộc notification. Token là dữ
 * liệu nhạy cảm: không log, không metric, chỉ lộ 6 ký tự cuối ở GET /devices. UNIQUE(token) toàn cục — token đổi chủ
 * chuyển sang thiết bị mới (ghi nhật ký kiểm toán không kèm token); tối đa 10 token mỗi người dùng.
 */
@Service
public class PushTokenService implements DeviceLifecycleListener, PushTokenLookup {

    static final int MAX_TOKENS = 10;
    static final Duration STALE = Duration.ofDays(60);
    private static final Set<String> PLATFORMS = Set.of("ANDROID", "IOS");
    private static final Logger audit = LoggerFactory.getLogger("com.lifeos.audit.push-token");

    private final NotificationStore store;
    private final IdentityDirectory identity;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public PushTokenService(NotificationStore store, IdentityDirectory identity, RateLimiter rateLimiter, Clock clock) {
        this.store = store;
        this.identity = identity;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    /** PUT /devices/{id}/push-token: 204; thiết bị không thuộc user/đã thu hồi → 404 DEVICE_NOT_FOUND. */
    @Transactional
    public void register(UUID userId, UUID deviceId, String token, String platform) {
        RateLimiter.Decision decision = rateLimiter.tryAcquire(RateLimitPolicy.PUSH_TOKEN_USER, userId.toString());
        if (!decision.allowed()) {
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
        if (token == null || token.isBlank() || token.length() > 512 || token.chars().anyMatch(Character::isISOControl)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "token", "INVALID_VALUE", "Token không hợp lệ.");
        }
        if (!PLATFORMS.contains(platform)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "platform", "INVALID_VALUE",
                    "platform là ANDROID hoặc IOS.");
        }
        if (!identity.isActiveDevice(userId, deviceId)) {
            throw new NotFoundException(ErrorCode.DEVICE_NOT_FOUND);
        }
        Optional<NotificationStore.PushToken> existing = store.tokenByValueForUpdate(token);
        if (existing.isPresent() && !existing.get().deviceId().equals(deviceId)) {
            // Token đổi chủ / đổi thiết bị: gỡ dòng cũ rồi gắn vào thiết bị mới (D139).
            store.deleteTokenById(existing.get().id());
            audit.atInfo().addKeyValue("event", "push_token_reassigned").addKeyValue("userId", userId)
                    .addKeyValue("deviceId", deviceId)
                    .addKeyValue("ownerChanged", !existing.get().userId().equals(userId))
                    .log("push_token_reassigned");
        }
        store.upsertDeviceToken(userId, deviceId, platform, token, clock.instant());
        store.trimTokens(userId, MAX_TOKENS);
    }

    @Transactional
    public void unregister(UUID userId, UUID deviceId) {
        if (!identity.isActiveDevice(userId, deviceId)) {
            throw new NotFoundException(ErrorCode.DEVICE_NOT_FOUND);
        }
        store.deleteTokenOfDevice(userId, deviceId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onDeviceSignedOut(UUID userId, UUID deviceId) {
        store.deleteTokenOfDevice(userId, deviceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> tokenSuffixes(UUID userId) {
        return store.tokenSuffixes(userId);
    }

    /** D141: job hằng ngày gỡ token không thấy quá 60 ngày. */
    @Transactional
    public int removeStale() {
        return store.deleteStaleTokens(clock.instant().minus(STALE));
    }
}
