package com.lifeos.identity.api;

import java.util.Optional;
import java.util.UUID;

/** API công khai của identity cho module khác (chỉ đọc). */
public interface IdentityDirectory {

    /** @return hồ sơ thông báo của người dùng còn hoạt động; rỗng nếu không có */
    Optional<NotificationProfile> notificationProfile(UUID userId);

    /** Thiết bị thuộc người dùng và chưa bị thu hồi. */
    boolean isActiveDevice(UUID userId, UUID deviceId);
}
