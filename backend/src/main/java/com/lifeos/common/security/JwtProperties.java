package com.lifeos.common.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình access token (SECURITY_RULES §4.1, Q3 Giai đoạn 1). secret/previousSecret là chuỗi base64 của khóa
 * HMAC; kiểm tra độ dài (>= 32 byte) nằm ở JwtService để app không khởi động khi cấu hình sai.
 */
@ConfigurationProperties("lifeos.security.jwt")
public record JwtProperties(String secret, String kid, String previousSecret, String previousKid, String issuer,
        String audience, Duration accessTokenTtl, Duration clockSkew) {

    @Override
    public String toString() {
        // Không bao giờ in secret (vd khi Spring log lỗi binding).
        return "JwtProperties[kid=" + kid + ", previousKid=" + previousKid + ", issuer=" + issuer + ", audience="
                + audience + ", accessTokenTtl=" + accessTokenTtl + ", clockSkew=" + clockSkew + "]";
    }
}
