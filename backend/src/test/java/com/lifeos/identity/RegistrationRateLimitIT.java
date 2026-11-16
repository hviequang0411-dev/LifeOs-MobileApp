package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SEC-05 (phần các endpoint của bước 1b), SECURITY_RULES §6 + D74: register 10/giờ/IP (bucket riêng);
 * register + resend (+ forgot ở 1d) chung bucket 20/giờ/IP; verify-email (+ reset ở 1d) chung bucket 20/giờ/IP.
 * Bộ test rate limit đầy đủ mọi dòng bảng ở bước 1e.
 */
class RegistrationRateLimitIT extends IdentityIntegrationTest {

    private static void assertRateLimited(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(result.getResponse().getHeader("Retry-After")).isNotNull();
        assertThat(Long.parseLong(result.getResponse().getHeader("Retry-After"))).isBetween(1L, 3600L);
        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"RATE_LIMITED\"");
    }

    @Test
    void registerIsLimitedTo10PerHourPerIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(register(newEmail()).getResponse().getStatus()).isEqualTo(202);
        }
        String eleventh = newEmail();
        assertRateLimited(register(eleventh));
        assertThat(userId(eleventh)).as("bị chặn trước khi tạo tài khoản").isEmpty();

        // IP khác không bị ảnh hưởng.
        assertThat(postJson("/api/v1/auth/register",
                body("email", newEmail(), "password", PASSWORD, "displayName", "B"), randomIp())
                .getResponse().getStatus()).isEqualTo(202);
    }

    @Test
    void registerAndResendShareOne20PerHourIpBucket() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(register(newEmail()).getResponse().getStatus()).isEqualTo(202);
        }
        for (int i = 0; i < 10; i++) {
            assertThat(resend(newEmail()).getResponse().getStatus()).isEqualTo(202);
        }
        assertRateLimited(resend(newEmail()));
    }

    @Test
    void verifyEmailIsLimitedTo20PerHourPerIp() throws Exception {
        for (int i = 0; i < 20; i++) {
            assertThat(verifyEmail("sai-" + i).getResponse().getStatus()).isEqualTo(400);
        }
        assertRateLimited(verifyEmail("sai-21"));
    }
}
