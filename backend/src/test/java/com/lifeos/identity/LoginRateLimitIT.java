package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SEC-05 (phần của bước 1c), SECURITY_RULES §6: login 5 sai/15 phút theo (email, IP), 20 sai theo email, 50 sai theo
 * IP; đăng nhập đúng đặt lại bộ đếm (email, IP); refresh 60/giờ/IP; logout 60/giờ/IP (bucket riêng, D74).
 */
class LoginRateLimitIT extends IdentityIntegrationTest {

    private static final String WRONG = "mat-khau-sai-000";

    private static void assertRateLimited(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(result.getResponse().getHeader("Retry-After")).isNotNull();
        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"RATE_LIMITED\"");
    }

    @Test
    void fiveFailuresPerEmailAndIpBlockEvenTheCorrectPasswordUntilTheWindowEnds() throws Exception {
        String email = newEmail();
        registerVerified(email);
        for (int i = 0; i < 5; i++) {
            assertThat(login(email, WRONG, null, ip).getResponse().getStatus()).isEqualTo(401);
        }

        MvcResult blocked = login(email, PASSWORD, null, ip);
        assertRateLimited(blocked);
        assertThat(Long.parseLong(blocked.getResponse().getHeader("Retry-After"))).isBetween(1L, 900L);
        // Cùng email từ IP khác chưa bị chặn (bucket email 20 chưa đầy).
        assertThat(login(email, PASSWORD, null, randomIp()).getResponse().getStatus()).isEqualTo(200);

        clock.advance(Duration.ofMinutes(15));
        assertThat(login(email, PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void successfulLoginResetsTheEmailIpCounter() throws Exception {
        String email = newEmail();
        registerVerified(email);
        for (int i = 0; i < 4; i++) {
            login(email, WRONG, null, ip);
        }
        assertThat(login(email, PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(200);
        for (int i = 0; i < 4; i++) {
            assertThat(login(email, WRONG, null, ip).getResponse().getStatus()).isEqualTo(401);
        }
        assertThat(login(email, PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void twentyFailuresForOneEmailFromManyIpsBlockThatEmail() throws Exception {
        String email = newEmail();
        registerVerified(email);
        for (int i = 0; i < 20; i++) {
            assertThat(login(email, WRONG, null, randomIp()).getResponse().getStatus()).isEqualTo(401);
        }
        assertRateLimited(login(email, PASSWORD, null, randomIp()));
    }

    @Test
    void fiftyFailuresFromOneIpBlockThatIpForEveryEmail() throws Exception {
        for (int i = 0; i < 50; i++) {
            assertThat(login(newEmail(), WRONG, null, ip).getResponse().getStatus()).isEqualTo(401);
        }
        assertRateLimited(login(newEmail(), WRONG, null, ip));
    }

    @Test
    void refreshIsLimitedTo60PerHourPerIp() throws Exception {
        for (int i = 0; i < 60; i++) {
            assertThat(refresh("sai-" + i).getResponse().getStatus()).isEqualTo(401);
        }
        assertRateLimited(refresh("sai-61"));
    }

    @Test
    void logoutIsLimitedTo60PerHourPerIpInItsOwnBucket() throws Exception {
        for (int i = 0; i < 60; i++) {
            assertThat(logout("sai-" + i).getResponse().getStatus()).isEqualTo(204);
        }
        assertRateLimited(logout("sai-61"));
        // Bucket riêng: refresh từ cùng IP chưa bị ảnh hưởng.
        assertThat(refresh("sai").getResponse().getStatus()).isEqualTo(401);
    }
}
