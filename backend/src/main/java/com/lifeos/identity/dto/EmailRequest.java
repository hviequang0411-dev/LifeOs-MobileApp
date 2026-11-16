package com.lifeos.identity.dto;

import com.lifeos.common.web.StrictJson;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /auth/resend-verification (và /auth/forgot-password ở bước 1d). */
public record EmailRequest(@NotBlank @Email @Size(max = 254) String email) implements StrictJson {

    /** Bỏ khoảng trắng đầu/cuối trước khi kiểm @Email (chuẩn hóa trim + lowercase, DATABASE_DESIGN §4.1). */
    public EmailRequest {
        email = email == null ? null : email.trim();
    }

    @Override
    public String toString() {
        return "EmailRequest[***]";
    }
}
