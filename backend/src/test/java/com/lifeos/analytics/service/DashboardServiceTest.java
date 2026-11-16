package com.lifeos.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.analytics.cache.AggregateCache;
import com.lifeos.analytics.dto.DashboardResponse;
import com.lifeos.common.security.RateLimiter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

/**
 * INT-06 (PHASE_7 D196): một thẻ lỗi hoặc hết giờ không làm hỏng phản hồi; các thẻ chạy song song (tổng thời gian ≈ thẻ
 * chậm nhất, không phải tổng). Không dùng Thread.sleep để đồng bộ: thẻ treo chờ latch không bao giờ mở.
 */
class DashboardServiceTest {

    private static final AggregateCache NO_CACHE = new AggregateCache() {
        @Override
        public <T> T get(UUID userId, String kind, String periodKey, Supplier<T> compute) {
            return compute.get();
        }

        @Override
        public void invalidate(UUID userId) {
        }
    };

    private static DashboardCard card(String id, Function<UUID, Object> f) {
        return new DashboardCard() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Object compute(UUID userId) {
                return f.apply(userId);
            }
        };
    }

    @Test
    void Int_06_failingAndHangingCardsAreIsolatedAndCardsRunInParallel() {
        CountDownLatch never = new CountDownLatch(1);
        CountDownLatch bothStarted = new CountDownLatch(2);
        List<DashboardCard> cards = List.of(
                card("ok", u -> "dữ liệu"),
                card("boom", u -> {
                    throw new IllegalStateException("hỏng");
                }),
                card("hang", u -> {
                    try {
                        never.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "không tới";
                }),
                card("parallel-a", u -> await(bothStarted)),
                card("parallel-b", u -> await(bothStarted)));
        DashboardService service = new DashboardService(cards, new RateLimiter(Clock.systemUTC()), NO_CACHE,
                new SimpleMeterRegistry(), Clock.systemUTC(), Duration.ofMillis(800));
        long start = System.nanoTime();
        DashboardResponse response = service.compute(UUID.randomUUID());
        long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(response.cards()).extracting(DashboardResponse.Card::id).containsExactly("ok", "boom", "hang",
                "parallel-a", "parallel-b");
        assertThat(response.cards()).extracting(DashboardResponse.Card::status).containsExactly("OK", "ERROR",
                "ERROR", "OK", "OK");
        assertThat(response.cards().get(1).data()).isNull();
        assertThat(response.cards().get(3).data()).as("hai thẻ chỉ xong khi cùng chạy song song").isEqualTo("cùng chạy");
        assertThat(elapsedMs).as("timeout tính chung từ lúc bắt đầu, không cộng dồn").isLessThan(3_000);
        service.destroy();
    }

    private static Object await(CountDownLatch latch) {
        latch.countDown();
        try {
            return latch.await(5, java.util.concurrent.TimeUnit.SECONDS) ? "cùng chạy" : "tuần tự";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "bị ngắt";
        }
    }
}
