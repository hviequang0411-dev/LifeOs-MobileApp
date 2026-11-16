package com.lifeos.notification.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.notification.domain.NotificationRules;
import com.lifeos.notification.dto.PreferenceItem;
import com.lifeos.notification.dto.PreferencesResponse;
import com.lifeos.notification.repository.NotificationStore;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tùy chọn thông báo (FR-NTF-04, D136): category × channel; thiếu dòng = mặc định; SECURITY × (IN_APP, EMAIL) không tắt
 * được (422 NOTIFICATION_PREFERENCE_LOCKED). Không đồng bộ qua sync — REST GET/PUT (BACKEND_MASTER_PLAN §4).
 */
@Service
public class PreferenceService {

    private final NotificationStore store;

    public PreferenceService(NotificationStore store) {
        this.store = store;
    }

    /** Bảng đầy đủ "CATEGORY:CHANNEL" → bật/tắt (đã áp mặc định và khóa SECURITY). */
    @Transactional(readOnly = true)
    public Map<String, Boolean> effective(UUID userId) {
        Map<String, Boolean> stored = store.preferences(userId);
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (String category : NotificationRules.CATEGORIES) {
            for (String channel : NotificationRules.CHANNELS) {
                String key = category + ":" + channel;
                boolean enabled = NotificationRules.locked(category, channel)
                        || stored.getOrDefault(key, NotificationRules.defaultEnabled(category, channel));
                out.put(key, enabled);
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public PreferencesResponse get(UUID userId) {
        List<PreferenceItem> items = new ArrayList<>();
        effective(userId).forEach((key, enabled) -> {
            String[] parts = key.split(":");
            items.add(new PreferenceItem(parts[0], parts[1], enabled));
        });
        return new PreferencesResponse(items);
    }

    /** PUT: thay toàn bộ tập (tối đa 18 dòng); dòng vắng giữ nguyên giá trị hiện tại. */
    @Transactional
    public PreferencesResponse replace(UUID userId, List<PreferenceItem> items) {
        if (items.size() > NotificationRules.CATEGORIES.size() * NotificationRules.CHANNELS.size()) {
            throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "items", "SIZE", "Tối đa 18 dòng.");
        }
        Set<String> seen = new HashSet<>();
        for (PreferenceItem item : items) {
            if (!NotificationRules.CATEGORIES.contains(item.category())
                    || !NotificationRules.CHANNELS.contains(item.channel())) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "items", "INVALID_VALUE",
                        "category/channel không hợp lệ.");
            }
            if (!seen.add(item.category() + ":" + item.channel())) {
                throw FieldErrors.single(ErrorCode.VALIDATION_FAILED, "items", "INVALID_VALUE", "Dòng bị lặp.");
            }
            if (NotificationRules.locked(item.category(), item.channel()) && !item.enabled()) {
                throw new ValidationException(ErrorCode.NOTIFICATION_PREFERENCE_LOCKED,
                        ErrorCode.NOTIFICATION_PREFERENCE_LOCKED.title());
            }
        }
        for (PreferenceItem item : items) {
            store.upsertPreference(userId, item.category(), item.channel(), item.enabled());
        }
        return get(userId);
    }

    /** Hủy đăng ký email từ liên kết (D143): chỉ tắt, idempotent, không áp cho SECURITY. */
    @Transactional
    public void disableEmail(UUID userId, String category) {
        store.upsertPreference(userId, category, "EMAIL", false);
    }
}
