package com.lifeos.common.security;

import java.time.Instant;
import java.util.UUID;

/**
 * Tập thiết bị vừa bị thu hồi (D45): JwtAuthFilter từ chối access token có did nằm trong tập, không cần truy vấn DB.
 * Mỗi phần tử chỉ cần giữ bằng đời tối đa của access token. Module identity ghi vào (sau khi commit) và nạp lại khi
 * khởi động.
 */
public interface RevokedDeviceRegistry {

    void revoke(UUID deviceId, Instant revokedAt);

    boolean isRevoked(UUID deviceId);
}
