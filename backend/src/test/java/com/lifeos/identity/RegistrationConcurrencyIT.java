package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.identity.service.RegisterCommand;
import com.lifeos.identity.service.RegistrationService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * D76: hai đăng ký đồng thời cùng email đều kết thúc bình thường (→ 202), chỉ một tài khoản được tạo; request thua
 * (vi phạm ux_users_email_lower) không để lại dữ liệu nửa vời. Lặp nhiều lần vì thứ tự xen kẽ không xác định; mỗi lần
 * có timeout cứng (TESTING_RULES §6).
 */
class RegistrationConcurrencyIT extends IdentityIntegrationTest {

    private static final int ITERATIONS = 10;

    @Autowired
    RegistrationService registrationService;

    @Test
    void concurrentRegistrationsWithSameEmailCreateExactlyOneAccount() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < ITERATIONS; i++) {
                String email = newEmail();
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> futures = new ArrayList<>();
                for (int t = 0; t < 2; t++) {
                    String clientIp = randomIp();
                    futures.add(pool.submit(() -> {
                        start.await();
                        registrationService.register(
                                new RegisterCommand(email, PASSWORD, "Đồng thời", null, clientIp));
                        return null;
                    }));
                }
                start.countDown();
                for (Future<?> f : futures) {
                    f.get(30, TimeUnit.SECONDS); // ném lỗi nếu một luồng thất bại
                }

                assertThat(countUsers(email)).isEqualTo(1);
                UUID userId = userId(email).orElseThrow();
                assertThat(jdbc.sql("SELECT count(*) FROM user_settings WHERE user_id = :u").param("u", userId)
                        .query(Integer.class).single()).isEqualTo(1);
                assertThat(jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u AND entity_type ="
                        + " 'user_settings'").param("u", userId).query(Integer.class).single()).isEqualTo(1);
                // D113: đúng một bộ danh mục mặc định dù đăng ký đồng thời.
                assertThat(jdbc.sql("SELECT count(*) FROM financial_categories WHERE user_id = :u")
                        .param("u", userId).query(Integer.class).single()).isEqualTo(15);
                assertThat(jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u").param("u", userId)
                        .query(Integer.class).single()).isEqualTo(16);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
