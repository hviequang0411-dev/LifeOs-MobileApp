package com.lifeos.notification.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

/**
 * Quy tắc thuần của thông báo: loại/kênh và mặc định (D136), giờ yên tĩnh (D135), backoff (D137).
 */
public final class NotificationRules {

    public static final String SECURITY = "SECURITY";
    public static final String REMINDER = "REMINDER";
    public static final List<String> CATEGORIES = List.of("SECURITY", "REMINDER", "TASK", "HABIT", "FINANCE",
            "SYSTEM");
    public static final List<String> CHANNELS = List.of("IN_APP", "PUSH", "EMAIL");
    private static final Set<String> PUSH_DEFAULT = Set.of("SECURITY", "REMINDER", "TASK", "HABIT");
    /** D137: backoff 30s, 2m, 10m, 1h, 6h; tối đa 5 lần. */
    public static final List<Duration> BACKOFF = List.of(Duration.ofSeconds(30), Duration.ofMinutes(2),
            Duration.ofMinutes(10), Duration.ofHours(1), Duration.ofHours(6));
    public static final int MAX_ATTEMPTS = 5;

    private NotificationRules() {
    }

    /** D136: IN_APP bật tất cả; PUSH bật SECURITY, REMINDER, TASK, HABIT; EMAIL chỉ SECURITY. */
    public static boolean defaultEnabled(String category, String channel) {
        return switch (channel) {
            case "IN_APP" -> true;
            case "PUSH" -> PUSH_DEFAULT.contains(category);
            default -> SECURITY.equals(category);
        };
    }

    /** D136: SECURITY × (IN_APP, EMAIL) không tắt được. */
    public static boolean locked(String category, String channel) {
        return SECURITY.equals(category) && ("IN_APP".equals(channel) || "EMAIL".equals(channel));
    }

    /**
     * D135: thời điểm gửi sớm nhất cho push/email. Trong khung yên tĩnh (có thể qua nửa đêm, giờ địa phương) → hết
     * khung; ngoài khung hoặc không đặt khung → now.
     */
    public static Instant notBefore(Instant now, ZoneId zone, LocalTime quietStart, LocalTime quietEnd) {
        if (quietStart == null || quietEnd == null || quietStart.equals(quietEnd)) {
            return now;
        }
        LocalDateTime local = LocalDateTime.ofInstant(now, zone);
        LocalTime t = local.toLocalTime();
        boolean overnight = quietStart.isAfter(quietEnd);
        boolean inside = overnight ? (!t.isBefore(quietStart) || t.isBefore(quietEnd))
                : (!t.isBefore(quietStart) && t.isBefore(quietEnd));
        if (!inside) {
            return now;
        }
        LocalDateTime end = local.toLocalDate().atTime(quietEnd);
        if (!end.isAfter(local)) {
            end = end.plusDays(1);
        }
        return ZonedDateTime.ofLocal(end, zone, null).toInstant();
    }

    /** Thời điểm thử lại sau lần thất bại thứ {@code attempt} (1-based), jitter ±20% theo {@code jitter} ∈ [0,1). */
    public static Instant retryAt(Instant now, int attempt, double jitter) {
        Duration base = BACKOFF.get(Math.min(attempt, BACKOFF.size()) - 1);
        long millis = base.toMillis();
        long delta = Math.round(millis * 0.4 * (jitter - 0.5));
        return now.plusMillis(millis + delta);
    }
}
