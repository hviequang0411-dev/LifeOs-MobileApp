package com.lifeos.identity.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.lifeos.common.util.Patch;

/**
 * PATCH /me theo JSON Merge Patch: cần phân biệt "vắng mặt" với "null", nên dùng setter ghi nhận sự có mặt thay vì
 * record. Trường lạ (email, userId…) bị bỏ qua và không bao giờ ánh xạ vào entity (SECURITY_RULES §7.5, §9.4).
 * Quy tắc giá trị kiểm ở ProfileService.
 */
public class UpdateProfileRequest {

    private String displayName;
    private boolean displayNamePresent;

    public UpdateProfileRequest() {
    }

    @JsonSetter("displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.displayNamePresent = true;
    }

    public Patch<String> displayName() {
        return displayNamePresent ? Patch.of(displayName) : Patch.absent();
    }
}
