package com.lifeos.identity.dto;

import com.lifeos.common.web.StrictJson;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /auth/register (API_CONVENTIONS §8.1). Độ dài mật khẩu kiểm ở service (sau NFKC) và trả AUTH_PASSWORD_TOO_WEAK;
 * timezone tùy chọn, kiểm tra IANA ở service.
 */
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull String password,
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 64) String timezone) implements StrictJson {

    /** Bỏ khoảng trắng đầu/cuối email trước khi kiểm @Email (chuẩn hóa trim + lowercase, DATABASE_DESIGN §4.1). */
    public RegisterRequest {
        email = email == null ? null : email.trim();
    }

    @Override
    public String toString() {
        return "RegisterRequest[timezone=" + timezone + "]";
    }
}
