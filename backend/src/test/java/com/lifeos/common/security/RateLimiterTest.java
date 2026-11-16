package com.lifeos.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Bộ đếm tần suất (SECURITY_RULES §6, D49/D74). */
class RateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-06T10:00:00Z"));
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    void policyValuesMatchTheApprovedTable() {
        assertPolicy(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, 5, Duration.ofMinutes(15));
        assertPolicy(RateLimitPolicy.LOGIN_FAILURE_EMAIL, 20, Duration.ofMinutes(15));
        assertPolicy(RateLimitPolicy.LOGIN_FAILURE_IP, 50, Duration.ofMinutes(15));
        assertPolicy(RateLimitPolicy.REGISTER_IP, 10, Duration.ofHours(1));
        assertPolicy(RateLimitPolicy.EMAIL_SEND_PER_EMAIL, 5, Duration.ofHours(1));
        assertPolicy(RateLimitPolicy.EMAIL_ENDPOINTS_IP, 20, Duration.ofHours(1));
        assertPolicy(RateLimitPolicy.TOKEN_ENDPOINTS_IP, 20, Duration.ofHours(1));
        assertPolicy(RateLimitPolicy.LOGOUT_IP, 60, Duration.ofHours(1));
        assertPolicy(RateLimitPolicy.CHANGE_PASSWORD_FAILURE_USER, 5, Duration.ofMinutes(15));
        assertPolicy(RateLimitPolicy.REFRESH_IP, 60, Duration.ofHours(1));
        assertPolicy(RateLimitPolicy.API_USER, 600, Duration.ofMinutes(1));
        // WP4: PHASE_4 §8 (token push) và D143 (hủy đăng ký email).
        assertPolicy(RateLimitPolicy.PUSH_TOKEN_USER, 20, Duration.ofHours(1));
        assertPolicy(RateLimitPolicy.UNSUBSCRIBE_IP, 20, Duration.ofHours(1));
        // WP5: PHASE_5 D174, D170.
        assertPolicy(RateLimitPolicy.SYNC_SNAPSHOT_USER, 6, Duration.ofHours(1));
        assertPolicy(RateLimitPolicy.SYNC_ENTITY_USER, 120, Duration.ofMinutes(1));
        assertPolicy(RateLimitPolicy.SYNC_DIAGNOSTICS_USER, 5, Duration.ofHours(1));
        assertPolicy(RateLimitPolicy.NOTE_SEARCH_USER, 60, Duration.ofMinutes(1));
        assertPolicy(RateLimitPolicy.DASHBOARD_USER, 30, Duration.ofMinutes(1));
        assertPolicy(RateLimitPolicy.ACCOUNT_DELETION_CODE_USER, 5, Duration.ofHours(1));
        assertPolicy(RateLimitPolicy.ACCOUNT_DELETION_FAILURE_USER, 5, Duration.ofMinutes(15));
        assertThat(RateLimitPolicy.values()).hasSize(20);
    }

    private static void assertPolicy(RateLimitPolicy policy, int limit, Duration window) {
        assertThat(policy.limit()).as(policy.name()).isEqualTo(limit);
        assertThat(policy.window()).as(policy.name()).isEqualTo(window);
    }

    @Test
    void allowsUpToLimitThenRejectsWithRetryAfter() {
        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire(RateLimitPolicy.REGISTER_IP, "10.0.0.1").allowed()).isTrue();
        }
        clock.advance(Duration.ofMinutes(20));
        RateLimiter.Decision denied = limiter.tryAcquire(RateLimitPolicy.REGISTER_IP, "10.0.0.1");

        assertThat(denied.allowed()).isFalse();
        // Cửa sổ bắt đầu từ lần đầu: còn 40 phút.
        assertThat(denied.retryAfterSeconds()).isEqualTo(Duration.ofMinutes(40).toSeconds());
    }

    @Test
    void windowResetsAfterItEnds() {
        for (int i = 0; i < 11; i++) {
            limiter.tryAcquire(RateLimitPolicy.REGISTER_IP, "10.0.0.2");
        }
        assertThat(limiter.tryAcquire(RateLimitPolicy.REGISTER_IP, "10.0.0.2").allowed()).isFalse();

        clock.advance(Duration.ofHours(1));
        assertThat(limiter.tryAcquire(RateLimitPolicy.REGISTER_IP, "10.0.0.2").allowed()).isTrue();
    }

    @Test
    void retryAfterIsRoundedUpAndAtLeastOneSecond() {
        for (int i = 0; i < 600; i++) {
            limiter.tryAcquire(RateLimitPolicy.API_USER, "u");
        }
        clock.advance(Duration.ofMillis(59_500));
        RateLimiter.Decision denied = limiter.tryAcquire(RateLimitPolicy.API_USER, "u");
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterSeconds()).isEqualTo(1);
    }

    @Test
    void keysAndPoliciesAreIndependentBuckets() {
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire(RateLimitPolicy.REGISTER_IP, "10.0.0.3");
        }
        assertThat(limiter.tryAcquire(RateLimitPolicy.REGISTER_IP, "10.0.0.3").allowed()).isFalse();
        assertThat(limiter.tryAcquire(RateLimitPolicy.REGISTER_IP, "10.0.0.4").allowed()).isTrue();
        // Cùng IP nhưng dòng khác của bảng (bucket chung forgot/resend/register) đếm riêng.
        assertThat(limiter.tryAcquire(RateLimitPolicy.EMAIL_ENDPOINTS_IP, "10.0.0.3").allowed()).isTrue();
    }

    @Test
    void failureCountingBlocksAtLimitAndResetClears() {
        String key = "a@example.test|10.0.0.5";
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, key);
            assertThat(limiter.check(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, key).allowed()).isTrue();
        }
        limiter.recordFailure(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, key);
        RateLimiter.Decision blocked = limiter.check(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, key);
        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(Duration.ofMinutes(15).toSeconds());

        // check() không tự đếm thêm.
        limiter.check(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, key);
        limiter.reset(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, key);
        assertThat(limiter.check(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, key).allowed()).isTrue();
    }

    @Test
    void failureWindowExpires() {
        String key = "b@example.test|10.0.0.6";
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, key);
        }
        assertThat(limiter.check(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, key).allowed()).isFalse();
        clock.advance(Duration.ofMinutes(15));
        assertThat(limiter.check(RateLimitPolicy.LOGIN_FAILURE_EMAIL_IP, key).allowed()).isTrue();
    }
}
