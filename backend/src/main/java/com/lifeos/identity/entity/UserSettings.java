package com.lifeos.identity.entity;

import com.lifeos.common.persistence.SyncedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Cài đặt người dùng (DATABASE_RULES §8.1): entity đồng bộ, đúng một dòng mỗi user, id do server sinh (D31).
 * Cột chuẩn (version bắt đầu từ 1, deleted_at, created_at/updated_at) kế thừa từ SyncedEntity (D80).
 */
@Entity
@Table(name = "user_settings")
public class UserSettings extends SyncedEntity {

    /** Giá trị entity_type trong change_logs / sync. */
    public static final String ENTITY_TYPE = "user_settings";
    public static final String DEFAULT_LOCALE = "vi-VN";
    public static final String DEFAULT_CURRENCY = "VND";
    public static final short DEFAULT_WEEK_START_DAY = 1;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false, length = 10)
    private String locale;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "default_currency", nullable = false, length = 3)
    private String defaultCurrency;

    @Column(name = "week_start_day", nullable = false)
    private short weekStartDay;

    /** Giờ yên tĩnh theo giờ địa phương (PHASE_4 D135; BACKEND_MASTER_PLAN §5 #1): cả hai null hoặc cả hai có. */
    @Column(name = "quiet_start")
    private java.time.LocalTime quietStart;

    @Column(name = "quiet_end")
    private java.time.LocalTime quietEnd;

    /** D138: hiện tiêu đề nhắc nhở trên màn hình khóa (mặc định tắt). */
    @Column(name = "show_details_on_lock_screen", nullable = false)
    private boolean showDetailsOnLockScreen;

    protected UserSettings() {
    }

    private UserSettings(UUID id, UUID userId) {
        super(id, userId);
    }

    /** Cài đặt mặc định khi đăng ký; timezone đã được kiểm tra là IANA hợp lệ. */
    public static UserSettings createDefault(UUID id, UUID userId, String timezone) {
        UserSettings settings = new UserSettings(id, userId);
        settings.timezone = Objects.requireNonNull(timezone);
        settings.locale = DEFAULT_LOCALE;
        settings.defaultCurrency = DEFAULT_CURRENCY;
        settings.weekStartDay = DEFAULT_WEEK_START_DAY;
        return settings;
    }

    /**
     * Áp các giá trị mới đã được kiểm tra; tham số null = không đổi trường đó. Đổi timezone không viết lại dữ liệu
     * ngày đã lưu (DATABASE_RULES §8.1). version do Hibernate tăng khi flush nếu có thay đổi.
     *
     * @return true nếu có ít nhất một trường thật sự đổi giá trị
     */
    public boolean apply(String newTimezone, String newLocale, String newCurrency, Short newWeekStartDay) {
        boolean changed = false;
        if (newTimezone != null && !newTimezone.equals(timezone)) {
            timezone = newTimezone;
            changed = true;
        }
        if (newLocale != null && !newLocale.equals(locale)) {
            locale = newLocale;
            changed = true;
        }
        if (newCurrency != null && !newCurrency.equals(defaultCurrency)) {
            defaultCurrency = newCurrency;
            changed = true;
        }
        if (newWeekStartDay != null && newWeekStartDay != weekStartDay) {
            weekStartDay = newWeekStartDay;
            changed = true;
        }
        return changed;
    }

    /**
     * Giờ yên tĩnh và tùy chọn màn hình khóa (§5 #1). quietPresent = cặp quiet có mặt trong lệnh (đã kiểm).
     *
     * @return true nếu có giá trị đổi
     */
    public boolean applyNotificationSettings(boolean quietPresent, java.time.LocalTime newQuietStart,
            java.time.LocalTime newQuietEnd, Boolean newShowDetails) {
        boolean changed = false;
        if (quietPresent && (!Objects.equals(quietStart, newQuietStart) || !Objects.equals(quietEnd, newQuietEnd))) {
            quietStart = newQuietStart;
            quietEnd = newQuietEnd;
            changed = true;
        }
        if (newShowDetails != null && newShowDetails != showDetailsOnLockScreen) {
            showDetailsOnLockScreen = newShowDetails;
            changed = true;
        }
        return changed;
    }

    public java.time.LocalTime getQuietStart() {
        return quietStart;
    }

    public java.time.LocalTime getQuietEnd() {
        return quietEnd;
    }

    public boolean isShowDetailsOnLockScreen() {
        return showDetailsOnLockScreen;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getLocale() {
        return locale;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public short getWeekStartDay() {
        return weekStartDay;
    }
}
