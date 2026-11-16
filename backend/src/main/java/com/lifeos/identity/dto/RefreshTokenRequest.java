package com.lifeos.identity.dto;

import com.lifeos.common.web.StrictJson;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /auth/refresh và POST /auth/logout: { "refreshToken" }. */
public record RefreshTokenRequest(@NotBlank @Size(max = 128) String refreshToken) implements StrictJson {

    @Override
    public String toString() {
        return "RefreshTokenRequest[***]";
    }
}
