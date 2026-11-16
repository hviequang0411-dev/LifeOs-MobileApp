package com.lifeos.identity.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.security.PasswordHasher;
import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.common.security.SecurityEvent;
import com.lifeos.common.security.SecurityEventLogger;
import com.lifeos.identity.entity.PasswordResetToken;
import com.lifeos.identity.entity.User;
import com.lifeos.identity.repository.PasswordResetTokenRepository;
import com.lifeos.identity.repository.RefreshTokenRepository;
import com.lifeos.identity.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Quên / đặt lại / đổi mật khẩu (UC-ID-06, UC-ID-07; SECURITY_RULES §3.4, §6; D54, D74, D76).
 * <p>
 * Như RegistrationService: rate limit, kiểm chính sách và băm Argon2 chạy TRƯỚC khi mở transaction (TransactionTemplate)
 * để transaction ngắn. Mỗi lần đổi/đặt lại thành công ghi outbox PASSWORD_CHANGED trong cùng transaction.
 */
@Service
public class PasswordService {

    static final Duration RESET_TOKEN_TTL = Duration.ofHours(1);

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final RefreshTokenRepository refreshTokens;
    private final IdentityOutboxEvents outboxEvents;
    private final PasswordHasher passwordHasher;
    private final RateLimiter rateLimiter;
    private final SecurityEventLogger securityEvents;
    private final TransactionTemplate tx;
    private final Clock clock;

    public PasswordService(UserRepository users, PasswordResetTokenRepository resetTokens,
            RefreshTokenRepository refreshTokens, IdentityOutboxEvents outboxEvents, PasswordHasher passwordHasher,
            RateLimiter rateLimiter, SecurityEventLogger securityEvents, TransactionTemplate tx, Clock clock) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.refreshTokens = refreshTokens;
        this.outboxEvents = outboxEvents;
        this.passwordHasher = passwordHasher;
        this.rateLimiter = rateLimiter;
        this.securityEvents = securityEvents;
        this.tx = tx;
        this.clock = clock;
    }

    /**
     * POST /auth/forgot-password. Luôn kết thúc bình thường (→ 202) trừ 429 theo IP. Gửi email cho cả tài khoản chưa
     * xác minh (D76). Bucket 5/giờ theo email dùng chung với resend-verification; vượt thì im lặng.
     */
    public void forgotPassword(String rawEmail, String clientIp) {
        acquire(RateLimitPolicy.EMAIL_ENDPOINTS_IP, clientIp);
        String email = EmailNormalizer.normalize(rawEmail);
        boolean allowed = rateLimiter.tryAcquire(RateLimitPolicy.EMAIL_SEND_PER_EMAIL, email).allowed();
        users.findByNormalizedEmail(email).ifPresent(user -> {
            if (!allowed) {
                securityEvents.log(SecurityEvent.RATE_LIMITED, user.getId(), null, clientIp);
                return;
            }
            tx.executeWithoutResult(status -> {
                Instant now = clock.instant();
                resetTokens.invalidateActive(user.getId(), now);
                OpaqueTokens.OpaqueToken token = OpaqueTokens.generate();
                Instant expiresAt = now.plus(RESET_TOKEN_TTL);
                resetTokens.save(PasswordResetToken.issue(UUID.randomUUID(), user.getId(), token.hash(), expiresAt));
                outboxEvents.passwordReset(user.getId(), user.getEmail(), user.getDisplayName(), token.raw(),
                        expiresAt);
            });
        });
    }

    /**
     * POST /auth/reset-password. Mật khẩu mới yếu → 400 và token KHÔNG bị dùng (người dùng thử lại được). Thành
     * công: đổi hash, xác minh email nếu chưa (D76), thu hồi MỌI refresh token, ghi PASSWORD_CHANGED.
     */
    public void resetPassword(String rawToken, String newPassword, String clientIp) {
        acquire(RateLimitPolicy.TOKEN_ENDPOINTS_IP, clientIp);
        PasswordPolicy.check(newPassword, "newPassword");
        String newHash = passwordHasher.hash(newPassword);
        UUID userId = tx.execute(status -> {
            Instant now = clock.instant();
            PasswordResetToken token = resetTokens.findByTokenHash(OpaqueTokens.sha256Hex(rawToken))
                    .orElseThrow(PasswordService::invalidResetToken);
            if (resetTokens.consume(token.getId(), now) != 1) {
                throw invalidResetToken();
            }
            User user = users.findById(token.getUserId()).orElseThrow(PasswordService::invalidResetToken);
            user.changePasswordHash(newHash);
            user.markEmailVerified(now);
            refreshTokens.revokeAllForUser(user.getId(), now);
            outboxEvents.passwordChanged(user.getId(), user.getEmail(), user.getDisplayName(), now);
            return user.getId();
        });
        securityEvents.log(SecurityEvent.PASSWORD_RESET, userId, null, clientIp);
    }

    /**
     * POST /me/change-password (UC-ID-07). Thứ tự: throttle 5 lần sai/15 phút theo user (D74) → kiểm mật khẩu hiện tại
     * → chính sách mật khẩu mới → đổi hash, thu hồi refresh token của các thiết bị KHÁC, ghi PASSWORD_CHANGED.
     * Access token còn hạn của thiết bị khác vẫn dùng được tối đa 15 phút (Q15).
     */
    public void changePassword(UUID userId, UUID currentDeviceId, String currentPassword, String newPassword,
            String clientIp) {
        String key = userId.toString();
        RateLimiter.Decision decision = rateLimiter.check(RateLimitPolicy.CHANGE_PASSWORD_FAILURE_USER, key);
        if (!decision.allowed()) {
            securityEvents.log(SecurityEvent.RATE_LIMITED, userId, currentDeviceId, clientIp);
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
        User user = users.findById(userId).orElseThrow(() -> new ValidationException(
                ErrorCode.AUTH_CURRENT_PASSWORD_WRONG, HttpStatus.BAD_REQUEST,
                ErrorCode.AUTH_CURRENT_PASSWORD_WRONG.title()));
        if (!passwordHasher.matches(currentPassword, user.getPasswordHash())) {
            rateLimiter.recordFailure(RateLimitPolicy.CHANGE_PASSWORD_FAILURE_USER, key);
            throw new ValidationException(ErrorCode.AUTH_CURRENT_PASSWORD_WRONG, HttpStatus.BAD_REQUEST,
                    ErrorCode.AUTH_CURRENT_PASSWORD_WRONG.title());
        }
        PasswordPolicy.check(newPassword, "newPassword");
        String newHash = passwordHasher.hash(newPassword);
        tx.executeWithoutResult(status -> {
            Instant now = clock.instant();
            User managed = users.findById(userId).orElseThrow();
            managed.changePasswordHash(newHash);
            refreshTokens.revokeAllForUserExceptDevice(userId, currentDeviceId, now);
            outboxEvents.passwordChanged(userId, managed.getEmail(), managed.getDisplayName(), now);
        });
        rateLimiter.reset(RateLimitPolicy.CHANGE_PASSWORD_FAILURE_USER, key);
        securityEvents.log(SecurityEvent.PASSWORD_CHANGED, userId, currentDeviceId, clientIp);
    }

    private void acquire(RateLimitPolicy policy, String key) {
        RateLimiter.Decision decision = rateLimiter.tryAcquire(policy, key);
        if (!decision.allowed()) {
            securityEvents.log(SecurityEvent.RATE_LIMITED, null, null, key);
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
    }

    private static ValidationException invalidResetToken() {
        return new ValidationException(ErrorCode.AUTH_RESET_TOKEN_INVALID, HttpStatus.BAD_REQUEST,
                ErrorCode.AUTH_RESET_TOKEN_INVALID.title());
    }
}
