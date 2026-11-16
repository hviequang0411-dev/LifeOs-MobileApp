package com.lifeos.notification.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.notification.domain.NotificationRules;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Hủy đăng ký email từ liên kết (PHASE_4 D143): công khai, giới hạn 20/giờ/IP, chỉ TẮT, idempotent; token hết hạn, giả
 * mạo hoặc loại SECURITY → 400 VALIDATION_FAILED (cùng một dạng, không lộ lý do).
 */
@Service
public class UnsubscribeService {

    private final UnsubscribeTokens tokens;
    private final PreferenceService preferences;
    private final RateLimiter rateLimiter;

    public UnsubscribeService(UnsubscribeTokens tokens, PreferenceService preferences, RateLimiter rateLimiter) {
        this.tokens = tokens;
        this.preferences = preferences;
        this.rateLimiter = rateLimiter;
    }

    public void unsubscribe(String token, String clientIp) {
        RateLimiter.Decision decision = rateLimiter.tryAcquire(RateLimitPolicy.UNSUBSCRIBE_IP, clientIp);
        if (!decision.allowed()) {
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
        Optional<UnsubscribeTokens.Claims> claims = tokens.verify(token);
        if (claims.isEmpty() || NotificationRules.SECURITY.equals(claims.get().category())
                || !NotificationRules.CATEGORIES.contains(claims.get().category())) {
            throw new ValidationException(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST,
                    "Liên kết hủy đăng ký không hợp lệ hoặc đã hết hạn.");
        }
        preferences.disableEmail(claims.get().userId(), claims.get().category());
    }
}
