package com.lifeos.identity.service;

import com.lifeos.identity.repository.EmailVerificationTokenRepository;
import com.lifeos.identity.repository.PasswordResetTokenRepository;
import com.lifeos.identity.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job dọn token (SYSTEM_ARCHITECTURE §3.12, DATABASE_DESIGN §7): hằng ngày xóa refresh_tokens,
 * email_verification_tokens, password_reset_tokens đã hết hạn quá 7 ngày. Idempotent; không đụng bảng đồng bộ nên
 * không liên quan thứ tự khóa D86.
 */
@Service
public class TokenCleanupService {

    /** DATABASE_DESIGN §7: "hết hạn quá 7 ngày" [CONFIRMED]. */
    public static final Duration RETENTION = Duration.ofDays(7);

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    private final RefreshTokenRepository refreshTokens;
    private final EmailVerificationTokenRepository verificationTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final Clock clock;

    public TokenCleanupService(RefreshTokenRepository refreshTokens,
            EmailVerificationTokenRepository verificationTokens, PasswordResetTokenRepository resetTokens,
            Clock clock) {
        this.refreshTokens = refreshTokens;
        this.verificationTokens = verificationTokens;
        this.resetTokens = resetTokens;
        this.clock = clock;
    }

    /** @return tổng số dòng đã xóa */
    @Scheduled(cron = "${lifeos.cleanup.cron}", zone = "UTC")
    @Transactional
    public int cleanup() {
        Instant cutoff = clock.instant().minus(RETENTION);
        int refresh = refreshTokens.deleteExpiredBefore(cutoff);
        int verification = verificationTokens.deleteExpiredBefore(cutoff);
        int reset = resetTokens.deleteExpiredBefore(cutoff);
        log.atInfo().addKeyValue("event", "token_cleanup")
                .addKeyValue("refreshTokens", refresh)
                .addKeyValue("verificationTokens", verification)
                .addKeyValue("resetTokens", reset)
                .log("token_cleanup");
        return refresh + verification + reset;
    }
}
