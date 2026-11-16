package com.lifeos.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.ai.config.AiProperties;
import com.lifeos.ai.service.AiConcurrencyGate;
import com.lifeos.common.exception.BusinessException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class AiConcurrencyGateTest {
    @Test
    void AI_20_userAndGlobalPermitAreLimitedAndReleased() throws Exception {
        AiProperties properties = new AiProperties();
        properties.setMaxConcurrentGlobal(1);
        properties.setMaxConcurrentPerUser(1);
        AiConcurrencyGate gate = new AiConcurrencyGate(properties);
        UUID user = UUID.randomUUID();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> first = executor.submit(() -> gate.execute(user, () -> {
                entered.countDown();
                try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return "first";
            }));
            entered.await();
            for (int i = 0; i < 20; i++) {
                UUID attemptUser = i % 2 == 0 ? user : UUID.randomUUID();
                assertThatThrownBy(() -> gate.execute(attemptUser, () -> "blocked"))
                        .isInstanceOf(BusinessException.class)
                        .satisfies(error -> assertThat(((BusinessException) error).getProperties())
                                .containsEntry("errorCode", "AI_RATE_LIMITED"));
            }
            release.countDown();
            assertThat(first.get()).isEqualTo("first");
        }
        assertThat(gate.execute(user, () -> "released")).isEqualTo("released");
    }
}
