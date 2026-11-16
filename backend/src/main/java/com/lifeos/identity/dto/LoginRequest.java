package com.lifeos.identity.dto;

import com.lifeos.common.web.StrictJson;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** POST /auth/login (API_CONVENTIONS §8.1). deviceId tùy chọn: id server đã cấp ở lần đăng nhập trước. */
public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull @Size(max = 1024) String password,
        UUID deviceId,
        @NotNull @Valid Device device) implements StrictJson {

    public LoginRequest {
        email = email == null ? null : email.trim();
    }

    @Override
    public String toString() {
        return "LoginRequest[deviceId=" + deviceId + "]";
    }

    /** Thông tin thiết bị do client tự khai (chỉ hiển thị). */
    public record Device(
            @NotBlank @Size(max = 100) String name,
            @NotNull @Pattern(regexp = "ANDROID|IOS") String platform,
            @Size(max = 32) String appVersion) implements StrictJson {
    }
}
