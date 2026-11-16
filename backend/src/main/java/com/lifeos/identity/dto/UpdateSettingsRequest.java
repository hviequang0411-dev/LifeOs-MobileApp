package com.lifeos.identity.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;
import jakarta.validation.constraints.NotNull;

/**
 * PATCH /me/settings theo JSON Merge Patch: { version, timezone?, locale?, defaultCurrency?, weekStartDay? }.
 * version bắt buộc (thiếu → 400 VALIDATION_FAILED, fieldErrors[version, REQUIRED], API_CONVENTIONS §8.4). Phân biệt
 * "vắng mặt" với "null" bằng setter; quy tắc giá trị kiểm ở UserSettingsCommandService (dùng chung với sync).
 * Trường lạ (id, userId…) bị bỏ qua.
 */
public class UpdateSettingsRequest {

    @NotNull
    private Long version;
    private Patch<String> timezone = Patch.absent();
    private Patch<String> locale = Patch.absent();
    private Patch<String> defaultCurrency = Patch.absent();
    private Patch<Integer> weekStartDay = Patch.absent();
    private Patch<String> quietStart = Patch.absent();
    private Patch<String> quietEnd = Patch.absent();
    private Patch<Boolean> showDetailsOnLockScreen = Patch.absent();

    public UpdateSettingsRequest() {
    }

    @JsonSetter("version")
    public void setVersion(Long version) {
        this.version = version;
    }

    @JsonSetter("timezone")
    public void setTimezone(String timezone) {
        this.timezone = Patch.of(timezone);
    }

    @JsonSetter("locale")
    public void setLocale(String locale) {
        this.locale = Patch.of(locale);
    }

    @JsonSetter("defaultCurrency")
    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = Patch.of(defaultCurrency);
    }

    @JsonSetter("weekStartDay")
    public void setWeekStartDay(Integer weekStartDay) {
        this.weekStartDay = Patch.of(weekStartDay);
    }

    public Long version() {
        return version;
    }

    public Patch<String> timezone() {
        return timezone;
    }

    public Patch<String> locale() {
        return locale;
    }

    public Patch<String> defaultCurrency() {
        return defaultCurrency;
    }

    public Patch<Integer> weekStartDay() {
        return weekStartDay;
    }

    @JsonSetter("quietStart")
    public void setQuietStart(String quietStart) {
        this.quietStart = Patch.of(quietStart);
    }

    @JsonSetter("quietEnd")
    public void setQuietEnd(String quietEnd) {
        this.quietEnd = Patch.of(quietEnd);
    }

    @JsonSetter("showDetailsOnLockScreen")
    public void setShowDetailsOnLockScreen(Boolean showDetailsOnLockScreen) {
        this.showDetailsOnLockScreen = Patch.of(showDetailsOnLockScreen);
    }

    public Patch<String> quietStart() {
        return quietStart;
    }

    public Patch<String> quietEnd() {
        return quietEnd;
    }

    public Patch<Boolean> showDetailsOnLockScreen() {
        return showDetailsOnLockScreen;
    }
}
