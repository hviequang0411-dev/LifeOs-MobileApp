package com.lifeos.identity.service;

import com.lifeos.common.security.JwtProperties;
import com.lifeos.common.security.RevokedDeviceRegistry;
import com.lifeos.identity.repository.DeviceRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Khi khởi động, nạp lại các thiết bị bị thu hồi trong khoảng đời tối đa của access token vào tập thu hồi trong bộ
 * nhớ (D45), để khởi động lại backend không mở lại cửa sổ dùng access token của thiết bị đã thu hồi.
 */
@Service
public class RevokedDeviceLoader {

    private final DeviceRepository devices;
    private final RevokedDeviceRegistry registry;
    private final Duration window;
    private final Clock clock;

    public RevokedDeviceLoader(DeviceRepository devices, RevokedDeviceRegistry registry, JwtProperties jwt,
            Clock clock) {
        this.devices = devices;
        this.registry = registry;
        this.window = jwt.accessTokenTtl().plus(jwt.clockSkew());
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public int load() {
        var recent = devices.findRevokedSince(clock.instant().minus(window));
        recent.forEach(d -> registry.revoke(d.getId(), d.getRevokedAt()));
        return recent.size();
    }
}
