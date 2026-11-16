package com.lifeos.common.time;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Lịch theo người dùng (Phase 2 D231, D233, D244): múi giờ hiện hành và ngày đầu tuần lấy từ user_settings. Đặt trong
 * common (đảo phụ thuộc như ChangeRecorder): module identity cài đặt, module nghiệp vụ (habit, routine…) chỉ biết
 * interface — không thêm cặp phụ thuộc module (A6).
 */
public interface UserCalendar {

    /** Múi giờ IANA hiện hành của người dùng. */
    ZoneId zone(UUID userId);

    /** Ngày bắt đầu tuần (user_settings.week_start_day: 1 = Thứ Hai … 7 = Chủ Nhật, ISO-8601). */
    DayOfWeek weekStart(UUID userId);

    /** "Hôm nay" theo múi giờ hiện hành của người dùng. */
    LocalDate today(UUID userId);
}
