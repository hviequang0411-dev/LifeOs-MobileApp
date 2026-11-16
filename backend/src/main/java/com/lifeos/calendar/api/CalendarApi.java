package com.lifeos.calendar.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** API công khai của module calendar (chỉ đọc). Mọi truy vấn theo userId (IDOR). */
public interface CalendarApi {

    /** Sự kiện còn sống của user, khóa FOR SHARE trong transaction ghi của bên gọi (reminder không mồ côi). */
    boolean lockForShare(UUID userId, UUID eventId);

    /**
     * Thời điểm bắt đầu (UTC) của lần xuất hiện đầu tiên ≥ {@code from}, đã áp ngoại lệ (CANCELLED bị bỏ, MODIFIED dùng
     * giờ ghi đè). Rỗng nếu sự kiện không còn lần xuất hiện nào / không tồn tại.
     */
    Optional<Instant> firstStartAtOrAfter(UUID userId, UUID eventId, Instant from);

    /** Kiểm quy tắc lặp D126 (dùng cho repeatRule của reminder); sai → 422 CALENDAR_INVALID_RECURRENCE. */
    void validateRule(java.util.Map<String, Object> rule);

    /** Lần xuất hiện đầu tiên (giờ địa phương) ≥ {@code from} của quy tắc bắt đầu từ {@code seriesStart}. */
    Optional<java.time.LocalDateTime> firstOccurrence(java.util.Map<String, Object> rule,
            java.time.LocalDateTime seriesStart, java.time.LocalDateTime from);

    /** Tiêu đề sự kiện để hiển thị thông báo (D138: chỉ khi người dùng bật hiện chi tiết). */
    Optional<String> title(UUID userId, UUID eventId);
}
