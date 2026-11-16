package com.lifeos.identity.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.exception.UnauthorizedException;
import com.lifeos.common.security.PasswordHasher;
import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.common.security.SecurityEvent;
import com.lifeos.common.security.SecurityEventLogger;
import com.lifeos.identity.entity.User;
import com.lifeos.identity.mapper.IdentityMapper;
import com.lifeos.identity.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Đăng nhập, refresh, đăng xuất (UC-ID-03..05; SECURITY_RULES §3.3, §4, §6). Lớp này không mở transaction: phần đọc
 * và băm mật khẩu (Argon2, chậm) chạy ngoài transaction; phần ghi nằm trong TokenService. Nhờ vậy lỗi 401 được ném
 * SAU khi transaction ghi (vd thu hồi family khi phát hiện dùng lại) đã commit.
 */
@Service
public class AuthService {

    private static final List<RateLimitPolicy> LOGIN_FAILURE_POLICIES = List.of(
            RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, RateLimitPolicy.LOGIN_FAILURE_EMAIL,
            RateLimitPolicy.LOGIN_FAILURE_IP);

    private final UserRepository users;
    private final TokenService tokenService;
    private final PasswordHasher passwordHasher;
    private final RateLimiter rateLimiter;
    private final SecurityEventLogger securityEvents;
    private final IdentityMapper mapper;

    public AuthService(UserRepository users, TokenService tokenService, PasswordHasher passwordHasher,
            RateLimiter rateLimiter, SecurityEventLogger securityEvents, IdentityMapper mapper) {
        this.users = users;
        this.tokenService = tokenService;
        this.passwordHasher = passwordHasher;
        this.rateLimiter = rateLimiter;
        this.securityEvents = securityEvents;
        this.mapper = mapper;
    }

    /**
     * UC-ID-03. Thứ tự: throttle (chỉ đếm lần sai) → tìm user → so mật khẩu (email không tồn tại vẫn chạy băm giả) →
     * kiểm trạng thái → kiểm xác minh email (chỉ báo SAU khi mật khẩu đúng) → mở phiên.
     */
    public LoginResult login(LoginCommand cmd) {
        String email = EmailNormalizer.normalize(cmd.email());
        String ip = cmd.clientIp();
        checkLoginThrottle(email, ip);

        Optional<User> found = users.findByNormalizedEmail(email);
        if (found.isEmpty()) {
            passwordHasher.simulateMatch(cmd.password()); // D47: thời gian phản hồi không lộ email tồn tại
            throw failLogin(email, ip, null);
        }
        User user = found.get();
        if (!passwordHasher.matches(cmd.password(), user.getPasswordHash())) {
            throw failLogin(email, ip, user);
        }
        if (!user.isActive()) {
            securityEvents.log(SecurityEvent.LOGIN_FAILED, user.getId(), null, ip);
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        if (!user.isEmailVerified()) {
            throw new UnauthorizedException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }

        rateLimiter.reset(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, emailIpKey(email, ip));
        String rehashed = passwordHasher.needsRehash(user.getPasswordHash())
                ? passwordHasher.hash(cmd.password())
                : null;
        TokenPair tokens = tokenService.openSession(user.getId(), cmd.deviceId(), cmd.device(), rehashed);
        securityEvents.log(SecurityEvent.LOGIN_SUCCEEDED, user.getId(), tokens.deviceId(), ip);
        return new LoginResult(tokens, mapper.toSummary(user));
    }

    /** UC-ID-04. Token không dùng được → 401 AUTH_REFRESH_TOKEN_INVALID (sau khi transaction xoay vòng đã commit). */
    public TokenPair refresh(String rawRefreshToken, String clientIp) {
        acquire(RateLimitPolicy.REFRESH_IP, clientIp);
        return tokenService.rotate(rawRefreshToken, clientIp)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    }

    /** UC-ID-05. Luôn kết thúc bình thường (→ 204) trừ rate limit theo IP. */
    public void logout(String rawRefreshToken, String clientIp) {
        acquire(RateLimitPolicy.LOGOUT_IP, clientIp);
        tokenService.logout(rawRefreshToken);
    }

    private void checkLoginThrottle(String email, String ip) {
        long retryAfter = 0;
        for (RateLimitPolicy policy : LOGIN_FAILURE_POLICIES) {
            RateLimiter.Decision decision = rateLimiter.check(policy, loginKey(policy, email, ip));
            if (!decision.allowed()) {
                retryAfter = Math.max(retryAfter, decision.retryAfterSeconds());
            }
        }
        if (retryAfter > 0) {
            securityEvents.log(SecurityEvent.RATE_LIMITED, null, null, ip);
            throw new RateLimitedException(retryAfter);
        }
    }

    private UnauthorizedException failLogin(String email, String ip, User userOrNull) {
        for (RateLimitPolicy policy : LOGIN_FAILURE_POLICIES) {
            rateLimiter.recordFailure(policy, loginKey(policy, email, ip));
        }
        securityEvents.log(SecurityEvent.LOGIN_FAILED, userOrNull == null ? null : userOrNull.getId(), null, ip);
        return new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    private static String loginKey(RateLimitPolicy policy, String email, String ip) {
        return switch (policy) {
            case LOGIN_FAILURE_EMAIL_IP -> emailIpKey(email, ip);
            case LOGIN_FAILURE_EMAIL -> email;
            case LOGIN_FAILURE_IP -> ip;
            default -> throw new IllegalArgumentException(policy.name());
        };
    }

    private static String emailIpKey(String email, String ip) {
        return email + '|' + ip;
    }

    private void acquire(RateLimitPolicy policy, String key) {
        RateLimiter.Decision decision = rateLimiter.tryAcquire(policy, key);
        if (!decision.allowed()) {
            securityEvents.log(SecurityEvent.RATE_LIMITED, null, null, key);
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
    }
}
