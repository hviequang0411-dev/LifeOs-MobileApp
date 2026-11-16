package com.lifeos.identity.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * GET/PATCH /me/settings (API_CONVENTIONS §8.2). updatedAt thêm ở Giai đoạn 3 (quyết định A, 2026-11-07) để data của
 * snapshot sync user_settings = Response REST bỏ id/version, đúng danh sách SYNC_PROTOCOL §6.1.
 */
public record SettingsResponse(UUID id, String timezone, String locale, String defaultCurrency, int weekStartDay,
        String quietStart, String quietEnd, boolean showDetailsOnLockScreen, long version, Instant updatedAt) {
}
