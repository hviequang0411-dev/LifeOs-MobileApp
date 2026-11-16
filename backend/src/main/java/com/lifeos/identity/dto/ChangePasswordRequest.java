package com.lifeos.identity.dto;

import com.lifeos.common.web.StrictJson;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** POST /me/change-password (Q14: trường lạ bị từ chối). Chính sách mật khẩu mới kiểm ở service. */
public record ChangePasswordRequest(@NotNull @Size(max = 1024) String currentPassword, @NotNull String newPassword)
        implements StrictJson {

    @Override
    public String toString() {
        return "ChangePasswordRequest[***]";
    }
}
