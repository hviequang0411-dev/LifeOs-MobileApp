package com.lifeos.identity.service;

import java.util.UUID;

/** Cặp token vừa cấp (API_CONVENTIONS §8.1). refreshToken là token thô: chỉ trả cho client, không log. */
public record TokenPair(String accessToken, long expiresIn, String refreshToken, UUID deviceId) {

    @Override
    public String toString() {
        return "TokenPair[deviceId=" + deviceId + ", expiresIn=" + expiresIn + "]";
    }
}
