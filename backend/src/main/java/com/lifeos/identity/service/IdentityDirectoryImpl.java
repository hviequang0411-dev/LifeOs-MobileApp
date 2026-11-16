package com.lifeos.identity.service;

import com.lifeos.identity.api.IdentityDirectory;
import com.lifeos.identity.api.NotificationProfile;
import com.lifeos.identity.entity.User;
import com.lifeos.identity.entity.UserSettings;
import com.lifeos.identity.repository.DeviceRepository;
import com.lifeos.identity.repository.UserRepository;
import com.lifeos.identity.repository.UserSettingsRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt {@link IdentityDirectory} (identity.api) — chỉ đọc. */
@Service
class IdentityDirectoryImpl implements IdentityDirectory {

    private final UserRepository users;
    private final UserSettingsRepository settings;
    private final DeviceRepository devices;

    IdentityDirectoryImpl(UserRepository users, UserSettingsRepository settings, DeviceRepository devices) {
        this.users = users;
        this.settings = settings;
        this.devices = devices;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationProfile> notificationProfile(UUID userId) {
        Optional<User> user = users.findById(userId).filter(User::isActive);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        UserSettings s = settings.findByUserId(userId).orElse(null);
        return Optional.of(new NotificationProfile(userId, user.get().getEmail(), user.get().isEmailVerified(),
                s == null ? UserSettings.DEFAULT_LOCALE : s.getLocale(),
                s == null ? "Asia/Ho_Chi_Minh" : s.getTimezone(),
                s == null ? null : s.getQuietStart(), s == null ? null : s.getQuietEnd(),
                s != null && s.isShowDetailsOnLockScreen()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveDevice(UUID userId, UUID deviceId) {
        return devices.findByIdAndUserId(deviceId, userId).filter(d -> d.getRevokedAt() == null).isPresent();
    }
}
