package com.lifeos.common.security;

import java.util.UUID;

/** Principal sau khi xác thực access token: userId và deviceId lấy từ claim, không từ request body. */
public record AuthenticatedUser(UUID userId, UUID deviceId) {
}
