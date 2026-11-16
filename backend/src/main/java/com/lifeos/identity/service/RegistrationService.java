package com.lifeos.identity.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.exception.ValidationException;
import com.lifeos.common.security.PasswordHasher;
import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.common.security.SecurityEvent;
import com.lifeos.common.security.SecurityEventLogger;
import com.lifeos.identity.entity.EmailVerificationToken;
import com.lifeos.identity.entity.User;
import com.lifeos.identity.repository.EmailVerificationTokenRepository;
import com.lifeos.identity.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đăng ký, xác minh email, gửi lại email xác minh (UC-ID-01, UC-ID-02; SECURITY_RULES §3.3–3.4, §6; D76).
 * <p>
 * Chống dò email: register/resend luôn trả cùng phản hồi; đăng ký với email đã tồn tại vẫn chạy một phép băm giả có
 * cùng chi phí để thời gian phản hồi không lộ sự tồn tại. Gửi email đi qua outbox trong cùng transaction.
 * <p>
 * register và resend dùng TransactionTemplate (vẫn là transaction boundary ở Application Service): rate limit và băm
 * Argon2 chạy TRƯỚC khi mở transaction để transaction ngắn (JAVA_SPRING_RULES §3.7), và vi phạm unique khi hai đăng
 * ký đồng thời cùng email được bắt SAU khi transaction đã rollback (D76: vẫn trả 202).
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);
    private static final String EMAIL_UNIQUE_INDEX = "ux_users_email_lower";

    private final UserRepository users;
    private final EmailVerificationTokenRepository verificationTokens;
    private final UserSettingsCommandService userSettings;
    private final UserProvisioningService provisioning;
    private final IdentityOutboxEvents outboxEvents;
    private final PasswordHasher passwordHasher;
    private final RateLimiter rateLimiter;
    private final SecurityEventLogger securityEvents;
    private final TransactionTemplate tx;
    private final Clock clock;

    public RegistrationService(UserRepository users, EmailVerificationTokenRepository verificationTokens,
            UserSettingsCommandService userSettings, UserProvisioningService provisioning,
            IdentityOutboxEvents outboxEvents, PasswordHasher passwordHasher, RateLimiter rateLimiter,
            SecurityEventLogger securityEvents, TransactionTemplate tx, Clock clock) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.userSettings = userSettings;
        this.provisioning = provisioning;
        this.outboxEvents = outboxEvents;
        this.passwordHasher = passwordHasher;
        this.rateLimiter = rateLimiter;
        this.securityEvents = securityEvents;
        this.tx = tx;
        this.clock = clock;
    }

    /** UC-ID-01. Luôn kết thúc bình thường (→ 202) trừ lỗi validation (400) và rate limit (429). */
    public void register(RegisterCommand cmd) {
        acquire(RateLimitPolicy.REGISTER_IP, cmd.clientIp());
        acquire(RateLimitPolicy.EMAIL_ENDPOINTS_IP, cmd.clientIp());

        String email = EmailNormalizer.normalize(cmd.email());
        PasswordPolicy.check(cmd.password(), "password");
        String timezone = TimeZones.requireIanaOrDefault(cmd.timezone(), "timezone");
        String displayName = cmd.displayName().trim();

        Optional<User> existing = users.findByNormalizedEmail(email);
        if (existing.isPresent()) {
            // D47/D76: cùng chi phí băm như đường tạo mới, không lộ email đã tồn tại.
            passwordHasher.simulateMatch(cmd.password());
            User user = existing.get();
            if (!user.isEmailVerified()) {
                resendIfAllowed(user);
            }
            return;
        }

        String passwordHash = passwordHasher.hash(cmd.password());
        try {
            tx.executeWithoutResult(status -> {
                User user = users.saveAndFlush(User.register(UUID.randomUUID(), email, passwordHash, displayName));
                // D86: token xác minh + outbox + dữ liệu mặc định của module khác (D113) ghi TRƯỚC; createDefault flush
                // rồi mới gọi ChangeRecorder; change_logs của dữ liệu mặc định ghi sau cùng.
                issueVerification(user);
                var defaults = provisioning.provision(user.getId());
                userSettings.createDefault(user.getId(), timezone);
                provisioning.record(user.getId(), defaults);
            });
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicateEmail(e)) {
                throw e;
            }
            // Hai đăng ký đồng thời cùng email: request thua nhận phản hồi giống hệt (D76). Không gửi email.
            log.debug("Concurrent registration for an existing email ignored");
        }
    }

    /** UC-ID-02. Token sai/hết hạn/đã dùng → 400 AUTH_VERIFICATION_TOKEN_INVALID. */
    @Transactional
    public void verifyEmail(String rawToken, String clientIp) {
        acquire(RateLimitPolicy.TOKEN_ENDPOINTS_IP, clientIp);
        Instant now = clock.instant();
        EmailVerificationToken token = verificationTokens.findByTokenHash(OpaqueTokens.sha256Hex(rawToken))
                .orElseThrow(RegistrationService::invalidVerificationToken);
        if (verificationTokens.consume(token.getId(), now) != 1) {
            throw invalidVerificationToken();
        }
        User user = users.findById(token.getUserId()).orElseThrow(RegistrationService::invalidVerificationToken);
        user.markEmailVerified(now);
        securityEvents.log(SecurityEvent.EMAIL_VERIFIED, user.getId(), null, clientIp);
    }

    /** UC-ID-02 (thay thế). Luôn kết thúc bình thường (→ 202) trừ rate limit theo IP (429). */
    public void resendVerification(String rawEmail, String clientIp) {
        acquire(RateLimitPolicy.EMAIL_ENDPOINTS_IP, clientIp);
        String email = EmailNormalizer.normalize(rawEmail);
        users.findByNormalizedEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresentOrElse(this::resendIfAllowed,
                        // Đếm cả email không tồn tại để bucket theo email không lộ sự tồn tại.
                        () -> rateLimiter.tryAcquire(RateLimitPolicy.EMAIL_SEND_PER_EMAIL, email));
    }

    /** Gửi lại email xác minh nếu chưa vượt 5/giờ theo email; vượt thì im lặng (SECURITY_RULES §6). */
    private void resendIfAllowed(User user) {
        if (!rateLimiter.tryAcquire(RateLimitPolicy.EMAIL_SEND_PER_EMAIL, user.getEmail()).allowed()) {
            securityEvents.log(SecurityEvent.RATE_LIMITED, user.getId(), null, null);
            return;
        }
        tx.executeWithoutResult(status -> issueVerification(user));
    }

    /** Vô hiệu token cũ còn hiệu lực, cấp token mới và ghi outbox (cùng transaction của bên gọi). */
    private void issueVerification(User user) {
        Instant now = clock.instant();
        verificationTokens.invalidateActive(user.getId(), now);
        OpaqueTokens.OpaqueToken token = OpaqueTokens.generate();
        Instant expiresAt = now.plus(VERIFICATION_TOKEN_TTL);
        verificationTokens.save(EmailVerificationToken.issue(UUID.randomUUID(), user.getId(), token.hash(),
                expiresAt));
        outboxEvents.emailVerification(user.getId(), user.getEmail(), user.getDisplayName(), token.raw(), expiresAt);
    }

    private void acquire(RateLimitPolicy policy, String key) {
        RateLimiter.Decision decision = rateLimiter.tryAcquire(policy, key);
        if (!decision.allowed()) {
            securityEvents.log(SecurityEvent.RATE_LIMITED, null, null, key);
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
    }

    private static boolean isDuplicateEmail(DataIntegrityViolationException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(EMAIL_UNIQUE_INDEX)) {
                return true;
            }
        }
        return false;
    }

    private static ValidationException invalidVerificationToken() {
        return new ValidationException(ErrorCode.AUTH_VERIFICATION_TOKEN_INVALID, HttpStatus.BAD_REQUEST,
                ErrorCode.AUTH_VERIFICATION_TOKEN_INVALID.title());
    }
}
