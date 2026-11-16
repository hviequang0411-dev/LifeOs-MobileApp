package com.lifeos.identity.service;

import com.lifeos.common.time.UserCalendar;
import com.lifeos.identity.repository.UserSettingsRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài đặt {@link UserCalendar} từ user_settings. Người dùng chưa có dòng settings (chỉ xảy ra với dữ liệu tạo bằng SQL
 * trong test) dùng mặc định của đăng ký: Asia/Ho_Chi_Minh, Thứ Hai (Q1, D74).
 */
@Service
public class UserCalendarImpl implements UserCalendar {

    static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserSettingsRepository settings;
    private final Clock clock;

    public UserCalendarImpl(UserSettingsRepository settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public ZoneId zone(UUID userId) {
        return settings.findByUserId(userId).map(s -> ZoneId.of(s.getTimezone())).orElse(DEFAULT_ZONE);
    }

    @Override
    @Transactional(readOnly = true)
    public DayOfWeek weekStart(UUID userId) {
        return settings.findByUserId(userId).map(s -> DayOfWeek.of(s.getWeekStartDay())).orElse(DayOfWeek.MONDAY);
    }

    @Override
    public LocalDate today(UUID userId) {
        return LocalDate.ofInstant(clock.instant(), zone(userId));
    }
}
