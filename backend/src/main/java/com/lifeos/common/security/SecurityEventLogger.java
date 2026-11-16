package com.lifeos.common.security;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Sự kiện bảo mật có cấu trúc (D53): event, userId, deviceId, ip; correlationId lấy từ MDC, timestamp do logger đặt.
 * Ghi dưới dạng key-value của SLF4J (xuất hiện trong log JSON ở prod và qua %kvp ở dev). Không bao giờ truyền email,
 * mật khẩu hay token vào đây.
 */
@Component
public class SecurityEventLogger {

    private static final Logger log = LoggerFactory.getLogger("com.lifeos.security.events");

    public void log(SecurityEvent event, UUID userId, UUID deviceId, String ip) {
        log.atInfo()
                .addKeyValue("event", event.name())
                .addKeyValue("userId", userId)
                .addKeyValue("deviceId", deviceId)
                .addKeyValue("ip", ip)
                .log("security_event {}", event.name());
    }
}
