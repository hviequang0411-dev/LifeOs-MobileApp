package com.lifeos.notification.dto;

import jakarta.validation.constraints.NotNull;

/** Một dòng tùy chọn (D136): category × channel. */
public record PreferenceItem(@NotNull String category, @NotNull String channel, @NotNull Boolean enabled) {
}
