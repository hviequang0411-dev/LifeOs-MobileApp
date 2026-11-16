package com.lifeos.identity.dto;

import com.lifeos.common.web.StrictJson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** POST /auth/reset-password. Chính sách mật khẩu mới kiểm ở service (AUTH_PASSWORD_TOO_WEAK). */
public record ResetPasswordRequest(@NotBlank @Size(max = 128) String token, @NotNull String newPassword)
        implements StrictJson {

    @Override
    public String toString() {
        return "ResetPasswordRequest[***]";
    }
}
