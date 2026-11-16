package com.lifeos.identity.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.UnauthorizedException;
import com.lifeos.common.persistence.VersionedWrites;
import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.common.util.Patch;
import com.lifeos.identity.dto.SettingsResponse;
import com.lifeos.identity.entity.UserSettings;
import com.lifeos.identity.mapper.IdentityMapper;
import com.lifeos.identity.repository.UserSettingsRepository;
import java.util.Currency;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh ghi user_settings (entity đồng bộ, Q1). Mọi thao tác ghi thật sự thay đổi dữ liệu gọi ChangeRecorder đúng một
 * lần trong cùng transaction (luật A9, SYNC_RULES §9.3). Quy tắc giá trị nằm ở đây (không chỉ ở DTO) vì sync push ở
 * Giai đoạn 3 gọi cùng service (SECURITY_RULES §8.3).
 */
@Service
public class UserSettingsCommandService {

    private static final Pattern LOCALE = Pattern.compile("^[a-z]{2,3}(-[A-Z]{2})?$");
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");

    private final UserSettingsRepository repository;
    private final ChangeRecorder changeRecorder;
    private final IdentityMapper mapper;
    private final VersionedWrites versionedWrites;
    private final org.springframework.context.ApplicationEventPublisher events;

    public UserSettingsCommandService(UserSettingsRepository repository, ChangeRecorder changeRecorder,
            IdentityMapper mapper, VersionedWrites versionedWrites,
            org.springframework.context.ApplicationEventPublisher events) {
        this.repository = repository;
        this.changeRecorder = changeRecorder;
        this.mapper = mapper;
        this.versionedWrites = versionedWrites;
        this.events = events;
    }

    /**
     * Tạo cài đặt mặc định khi đăng ký: version 1, change_logs CREATE với mutationId null. Tham gia transaction đăng
     * ký của bên gọi; user phải đã được flush (change_logs có FK tới users).
     */
    @Transactional
    public UUID createDefault(UUID userId, String ianaTimezone) {
        UserSettings settings = repository.saveAndFlush(
                UserSettings.createDefault(UUID.randomUUID(), userId, ianaTimezone));
        changeRecorder.record(userId, UserSettings.ENTITY_TYPE, settings.getId(), ChangeOperation.CREATE,
                settings.getVersion(), null);
        return settings.getId();
    }

    /**
     * PATCH /me/settings. Thứ tự (SYSTEM_ARCHITECTURE §3.4): tải theo userId → kiểm version (lệch → 409 kèm
     * currentVersion) → kiểm giá trị → áp dụng → saveAndFlush (version +1) → ChangeRecorder.record(UPDATE).
     * Patch không đổi giá trị nào: không tăng version, không ghi change_logs.
     * <p>
     * Hai request đồng thời cùng version: @Version của Hibernate chặn request thứ hai khi flush; VersionedWrites (D80)
     * đọc lại version trong transaction mới và trả cùng 409 kèm currentVersion như kiểm tra tường minh.
     */
    public SettingsResponse update(UUID userId, UpdateSettingsCommand cmd) {
        return update(userId, cmd, null);
    }

    /** Như {@link #update(UUID, UpdateSettingsCommand)}; mutationIdOrNull ghi vào change_logs khi đến từ sync. */
    public SettingsResponse update(UUID userId, UpdateSettingsCommand cmd, UUID mutationIdOrNull) {
        return versionedWrites.write(() -> doUpdate(userId, cmd, mutationIdOrNull),
                () -> repository.findByUserId(userId)
                .map(UserSettings::getVersion)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID)));
    }

    private SettingsResponse doUpdate(UUID userId, UpdateSettingsCommand cmd, UUID mutationIdOrNull) {
        UserSettings settings = repository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID));
        VersionedWrites.requireVersion(settings, cmd.expectedVersion());
        String timezone = cmd.timezone().present()
                ? TimeZones.requireIanaOrDefault(requireValue(cmd.timezone(), "timezone"), "timezone")
                : null;
        String locale = validated(cmd.locale(), "locale", LOCALE, "Ngôn ngữ không hợp lệ (ví dụ vi-VN).");
        String currency = validated(cmd.defaultCurrency(), "defaultCurrency", CURRENCY,
                "Mã tiền tệ không hợp lệ (ISO 4217, ví dụ VND).");
        if (currency != null && !isIsoCurrency(currency)) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "defaultCurrency", "INVALID_VALUE",
                    "Mã tiền tệ không hợp lệ (ISO 4217, ví dụ VND).");
        }
        Short weekStartDay = null;
        if (cmd.weekStartDay().present()) {
            int value = requireValue(cmd.weekStartDay(), "weekStartDay");
            if (value < 1 || value > 7) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "weekStartDay", "SIZE",
                        "Ngày bắt đầu tuần từ 1 (Thứ Hai) đến 7 (Chủ Nhật).");
            }
            weekStartDay = (short) value;
        }

        boolean quietPresent = cmd.quietStart().present() || cmd.quietEnd().present();
        java.time.LocalTime quietStart = null;
        java.time.LocalTime quietEnd = null;
        if (quietPresent) {
            // Cặp giờ yên tĩnh: cả hai null (tắt) hoặc cả hai "HH:mm" khác nhau (§5 #1, D135).
            quietStart = time(cmd.quietStart().present() ? cmd.quietStart().value()
                    : formatTime(settings.getQuietStart()), "quietStart");
            quietEnd = time(cmd.quietEnd().present() ? cmd.quietEnd().value()
                    : formatTime(settings.getQuietEnd()), "quietEnd");
            if ((quietStart == null) != (quietEnd == null) || (quietStart != null && quietStart.equals(quietEnd))) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, quietStart == null ? "quietStart" : "quietEnd",
                        "INVALID_VALUE", "quietStart và quietEnd cùng có giá trị (khác nhau) hoặc cùng null.");
            }
        }
        Boolean showDetails = cmd.showDetailsOnLockScreen().present()
                ? requireValue(cmd.showDetailsOnLockScreen(), "showDetailsOnLockScreen") : null;

        String oldTimezone = settings.getTimezone();
        boolean changed = settings.apply(timezone, locale, currency, weekStartDay);
        changed |= settings.applyNotificationSettings(quietPresent, quietStart, quietEnd, showDetails);
        if (!changed) {
            return mapper.toSettings(settings);
        }
        UserSettings saved = repository.saveAndFlush(settings);
        if (!oldTimezone.equals(saved.getTimezone())) {
            // D134: module notification tính lại reminder theo giờ địa phương trong CÙNG transaction, trước change_logs
            // của user_settings (D86).
            events.publishEvent(new com.lifeos.identity.api.UserTimezoneChanged(userId, saved.getTimezone()));
        }
        changeRecorder.record(userId, UserSettings.ENTITY_TYPE, saved.getId(), ChangeOperation.UPDATE,
                saved.getVersion(), mutationIdOrNull);
        return mapper.toSettings(saved);
    }

    private static String validated(Patch<String> patch, String field, Pattern pattern, String message) {
        if (!patch.present()) {
            return null;
        }
        String value = requireValue(patch, field);
        if (!pattern.matcher(value).matches()) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, "PATTERN", message);
        }
        return value;
    }

    /** Trường không cho phép null: gửi null tường minh → 400 REQUIRED. */
    private static <T> T requireValue(Patch<T> patch, String field) {
        if (patch.isExplicitNull()) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, "REQUIRED", "Trường này không được để trống.");
        }
        return patch.value();
    }

    private static boolean isIsoCurrency(String code) {
        try {
            Currency.getInstance(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static final java.time.format.DateTimeFormatter HH_MM = java.time.format.DateTimeFormatter
            .ofPattern("HH:mm").withResolverStyle(java.time.format.ResolverStyle.STRICT);

    private static java.time.LocalTime time(String raw, String field) {
        if (raw == null) {
            return null;
        }
        try {
            return java.time.LocalTime.parse(raw, HH_MM);
        } catch (java.time.format.DateTimeParseException e) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, field, "PATTERN", "Giờ theo dạng HH:mm.");
        }
    }

    private static String formatTime(java.time.LocalTime time) {
        return time == null ? null : time.format(HH_MM);
    }
}
