package com.lifeos.identity.service;

import com.lifeos.common.util.Patch;

/**
 * PATCH /me/settings (API_CONVENTIONS §8.2) — cũng là dạng lệnh mà sync handler của user_settings sẽ gọi ở Giai đoạn
 * 3. expectedVersion bắt buộc (D58); các trường theo JSON Merge Patch.
 */
public record UpdateSettingsCommand(long expectedVersion, Patch<String> timezone, Patch<String> locale,
        Patch<String> defaultCurrency, Patch<Integer> weekStartDay, Patch<String> quietStart,
        Patch<String> quietEnd, Patch<Boolean> showDetailsOnLockScreen) {

    /** Lệnh Phase 1 (không có trường thông báo). */
    public UpdateSettingsCommand(long expectedVersion, Patch<String> timezone, Patch<String> locale,
            Patch<String> defaultCurrency, Patch<Integer> weekStartDay) {
        this(expectedVersion, timezone, locale, defaultCurrency, weekStartDay, Patch.absent(), Patch.absent(),
                Patch.absent());
    }
}
