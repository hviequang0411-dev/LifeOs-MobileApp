package com.lifeos.identity.dto;

import java.util.UUID;

/** Phản hồi POST /auth/login (API_CONVENTIONS §8.1). */
public record LoginResponse(String tokenType, String accessToken, long expiresIn, String refreshToken,
        UUID deviceId, UserSummary user) {

    @Override
    public String toString() {
        return "LoginResponse[deviceId=" + deviceId + "]";
    }
}
