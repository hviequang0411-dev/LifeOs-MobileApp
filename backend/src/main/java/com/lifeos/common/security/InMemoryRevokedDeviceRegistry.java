package com.lifeos.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Cài đặt trong bộ nhớ (một instance ở Phase 1, D45). Hết hạn được tính theo Clock để test được; expireAfterWrite của
 * Caffeine chỉ để dọn bộ nhớ.
 */
@Component
public class InMemoryRevokedDeviceRegistry implements RevokedDeviceRegistry {

    private final Duration window;
    private final Clock clock;
    private final Cache<UUID, Instant> revoked;

    public InMemoryRevokedDeviceRegistry(JwtProperties props, Clock clock) {
        this.window = Objects.requireNonNull(props.accessTokenTtl(), "accessTokenTtl").plus(props.clockSkew());
        this.clock = clock;
        this.revoked = Caffeine.newBuilder().expireAfterWrite(window).build();
    }

    @Override
    public void revoke(UUID deviceId, Instant revokedAt) {
        Objects.requireNonNull(deviceId, "deviceId");
        Objects.requireNonNull(revokedAt, "revokedAt");
        if (isWithinWindow(revokedAt)) {
            revoked.asMap().merge(deviceId, revokedAt, (a, b) -> a.isAfter(b) ? a : b);
        }
    }

    @Override
    public boolean isRevoked(UUID deviceId) {
        Instant at = deviceId == null ? null : revoked.getIfPresent(deviceId);
        return at != null && isWithinWindow(at);
    }

    private boolean isWithinWindow(Instant revokedAt) {
        return revokedAt.plus(window).isAfter(clock.instant());
    }
}
