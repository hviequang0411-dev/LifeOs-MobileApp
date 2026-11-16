package com.lifeos.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Tập thiết bị bị thu hồi trong bộ nhớ (D45): giữ đúng bằng đời access token + độ lệch đồng hồ. */
class InMemoryRevokedDeviceRegistryTest {

    private static final Instant START = Instant.parse("2026-11-06T10:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final InMemoryRevokedDeviceRegistry registry = new InMemoryRevokedDeviceRegistry(
            new JwtProperties("unused", "k1", null, null, "iss", "aud", Duration.ofMinutes(15), Duration.ofSeconds(30)),
            clock);

    @Test
    void revokedDeviceIsRejectedUntilEveryIssuedAccessTokenHasExpired() {
        UUID device = UUID.randomUUID();
        registry.revoke(device, START);

        assertThat(registry.isRevoked(device)).isTrue();
        clock.advance(Duration.ofMinutes(15).plusSeconds(29));
        assertThat(registry.isRevoked(device)).isTrue();
        clock.advance(Duration.ofSeconds(1));
        assertThat(registry.isRevoked(device)).isFalse();
    }

    @Test
    void otherDevicesAreNotAffected() {
        registry.revoke(UUID.randomUUID(), START);
        assertThat(registry.isRevoked(UUID.randomUUID())).isFalse();
        assertThat(registry.isRevoked(null)).isFalse();
    }

    @Test
    void reloadingAnOldRevocationAtStartupOnlyCoversTheRemainingWindow() {
        UUID recent = UUID.randomUUID();
        UUID old = UUID.randomUUID();
        registry.revoke(recent, START.minus(Duration.ofMinutes(10)));
        registry.revoke(old, START.minus(Duration.ofMinutes(16)));

        assertThat(registry.isRevoked(recent)).isTrue();
        assertThat(registry.isRevoked(old)).isFalse();
        clock.advance(Duration.ofMinutes(6));
        assertThat(registry.isRevoked(recent)).isFalse();
    }
}
