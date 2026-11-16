package com.lifeos.notification.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Thông báo trong ứng dụng (D142). data: deep link (type, id), không dữ liệu nhạy cảm. */
public record NotificationResponse(UUID id, String category, String templateKey, String title, String body,
        Map<String, Object> data, Instant readAt, Instant createdAt, Instant expiresAt) {

    @Override
    public String toString() {
        return "NotificationResponse[id=" + id + "]";
    }
}
