package com.lifeos.identity.dto;

import java.util.UUID;

/** Phản hồi POST /auth/refresh (API_CONVENTIONS §8.1). */
public record TokenResponse(String tokenType, String accessToken, long expiresIn, String refreshToken,
        UUID deviceId) {

    @Override
    public String toString() {
        return "TokenResponse[deviceId=" + deviceId + "]";
    }
}
