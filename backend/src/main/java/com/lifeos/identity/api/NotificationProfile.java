package com.lifeos.identity.api;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Thông tin người nhận cho module notification (PHASE_4 D135, D138, D140): địa chỉ email (PII — không log), ngôn ngữ,
 * múi giờ, giờ yên tĩnh, tùy chọn hiện chi tiết trên màn hình khóa. toString không in email.
 */
public record NotificationProfile(UUID userId, String email, boolean emailVerified, String locale, String timezone,
        LocalTime quietStart, LocalTime quietEnd, boolean showDetailsOnLockScreen) {

    @Override
    public String toString() {
        return "NotificationProfile[userId=" + userId + "]";
    }
}
