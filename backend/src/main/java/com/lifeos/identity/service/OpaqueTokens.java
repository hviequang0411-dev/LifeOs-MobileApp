package com.lifeos.identity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token ngẫu nhiên 256 bit (SecureRandom, base64url không padding) và hash SHA-256 hex để lưu DB
 * (SECURITY_RULES §3.4, §4.2). Dùng cho token xác minh email, đặt lại mật khẩu, refresh token.
 */
public final class OpaqueTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private OpaqueTokens() {
    }

    public static OpaqueToken generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new OpaqueToken(raw, sha256Hex(raw));
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không có sẵn", e);
        }
    }

    /** raw chỉ đi vào payload outbox / phản hồi cho client; hash đi vào DB. */
    public record OpaqueToken(String raw, String hash) {

        @Override
        public String toString() {
            return "OpaqueToken[hash=" + hash + "]";
        }
    }
}
