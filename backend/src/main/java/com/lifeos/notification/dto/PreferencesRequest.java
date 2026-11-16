package com.lifeos.notification.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** PUT /notifications/preferences: thay toàn bộ tập (tối đa 18 dòng). */
public record PreferencesRequest(@NotNull List<@jakarta.validation.Valid PreferenceItem> items) {
}
