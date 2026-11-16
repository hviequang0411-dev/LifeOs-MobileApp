package com.lifeos.notification.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Token hủy đăng ký email (PHASE_4 D143): HMAC-SHA256 bằng khóa RIÊNG (không dùng khóa JWT), chứa userId, category, hạn
 * 30 ngày. Dạng {@code base64url(userId|category|expEpochSecond).base64url(hmac)}. Khóa lấy từ biến môi trường
 * {@code NOTIFICATION_UNSUBSCRIBE_SECRET} (≥ 32 ký tự; profile dev/test có giá trị riêng).
 */
@Component
public class UnsubscribeTokens {

    static final Duration TTL = Duration.ofDays(30);

    public record Claims(UUID userId, String category) {
    }

    private final byte[] key;
    private final Clock clock;

    public UnsubscribeTokens(@Value("${lifeos.notification.unsubscribe-secret}") String secret, Clock clock) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("lifeos.notification.unsubscribe-secret phải ≥ 32 ký tự");
        }
        this.key = secret.getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    public String issue(UUID userId, String category) {
        String payload = userId + "|" + category + "|" + clock.instant().plus(TTL).getEpochSecond();
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        return b64.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + b64.encodeToString(mac(payload));
    }

    /** @return claims nếu chữ ký đúng và chưa hết hạn; rỗng nếu giả mạo/hỏng/hết hạn */
    public Optional<Claims> verify(String token) {
        if (token == null || token.length() > 512) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            byte[] signature = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(signature, mac(payload))) {
                return Optional.empty();
            }
            String[] fields = payload.split("\\|");
            if (fields.length != 3 || Instant.ofEpochSecond(Long.parseLong(fields[2])).isBefore(clock.instant())) {
                return Optional.empty();
            }
            return Optional.of(new Claims(UUID.fromString(fields[0]), fields[1]));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private byte[] mac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
