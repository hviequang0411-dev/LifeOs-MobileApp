package com.lifeos.notification.dto;

import java.util.List;

/** GET/PUT /notifications/preferences: đủ 6 × 3 dòng (thiếu dòng = mặc định D136). */
public record PreferencesResponse(List<PreferenceItem> items) {
}
