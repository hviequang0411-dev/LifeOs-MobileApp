package com.lifeos.identity.dto;

import java.time.Instant;
import java.util.UUID;

/** Một thiết bị trong GET /devices (API_CONVENTIONS §8.3). current = thiết bị của access token đang dùng;
 * pushTokenSuffix = 6 ký tự cuối token push đã đăng ký (PHASE_4 D147), null nếu chưa có. */
public record DeviceResponse(UUID id, String name, String platform, String appVersion, Instant lastSeenAt,
        boolean current, Instant revokedAt, String pushTokenSuffix) {
}
