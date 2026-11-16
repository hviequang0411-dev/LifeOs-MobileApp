package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.security.RevokedDeviceRegistry;
import com.lifeos.identity.service.RevokedDeviceLoader;
import com.lifeos.support.TestUsers;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * D45: khi khởi động, thiết bị bị thu hồi trong khoảng đời tối đa của access token (15 phút + 30 giây lệch) được nạp
 * lại vào tập thu hồi trong bộ nhớ; thiết bị thu hồi lâu hơn thì không cần.
 */
class RevokedDeviceLoaderIT extends IdentityIntegrationTest {

    @Autowired
    RevokedDeviceLoader loader;

    @Autowired
    RevokedDeviceRegistry registry;

    private UUID insertDevice(UUID userId, Duration revokedAgo) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO devices (id, user_id, name, platform, last_seen_at, revoked_at, created_at)"
                        + " VALUES (:id, :u, 'Cũ', 'IOS', :now, :revokedAt, :now)")
                .param("id", id).param("u", userId)
                .param("now", OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC))
                .param("revokedAt", revokedAgo == null ? null
                        : OffsetDateTime.ofInstant(clock.instant().minus(revokedAgo), ZoneOffset.UTC))
                .update();
        return id;
    }

    @Test
    void reloadsOnlyRecentlyRevokedDevices() {
        UUID userId = TestUsers.insert(jdbc);
        UUID recent = insertDevice(userId, Duration.ofMinutes(5));
        UUID old = insertDevice(userId, Duration.ofMinutes(20));
        UUID active = insertDevice(userId, null);

        int loaded = loader.load();

        assertThat(loaded).isGreaterThanOrEqualTo(1);
        assertThat(registry.isRevoked(recent)).isTrue();
        assertThat(registry.isRevoked(old)).isFalse();
        assertThat(registry.isRevoked(active)).isFalse();
    }
}
