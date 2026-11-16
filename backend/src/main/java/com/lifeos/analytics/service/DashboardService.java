package com.lifeos.analytics.service;

import com.lifeos.analytics.cache.AggregateCache;
import com.lifeos.analytics.dto.DashboardResponse;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

/**
 * GET /dashboard (D196): các thẻ chạy song song (luồng ảo), timeout 1,5 s/thẻ tính từ lúc bắt đầu; thẻ lỗi/hết giờ
 * ⇒ status ERROR, phản hồi vẫn 200. Log chỉ ghi id thẻ và loại lỗi (không dữ liệu). 30 lần/phút/user (D208).
 */
@Service
public class DashboardService implements DisposableBean {

    static final Duration CARD_TIMEOUT = Duration.ofMillis(1500);
    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final List<DashboardCard> cards;
    private final RateLimiter rateLimiter;
    private final AggregateCache cache;
    private final MeterRegistry metrics;
    private final Clock clock;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Duration timeout;

    @org.springframework.beans.factory.annotation.Autowired
    public DashboardService(List<DashboardCard> cards, RateLimiter rateLimiter, AggregateCache cache,
            MeterRegistry metrics, Clock clock) {
        this(cards, rateLimiter, cache, metrics, clock, CARD_TIMEOUT);
    }

    DashboardService(List<DashboardCard> cards, RateLimiter rateLimiter, AggregateCache cache, MeterRegistry metrics,
            Clock clock, Duration timeout) {
        this.cards = List.copyOf(cards);
        this.rateLimiter = rateLimiter;
        this.cache = cache;
        this.metrics = metrics;
        this.clock = clock;
        this.timeout = timeout;
    }

    public DashboardResponse get(UUID userId) {
        RateLimiter.Decision decision = rateLimiter.tryAcquire(RateLimitPolicy.DASHBOARD_USER, userId.toString());
        if (!decision.allowed()) {
            throw new RateLimitedException(decision.retryAfterSeconds());
        }
        return cache.get(userId, "dashboard", "now", () -> compute(userId));
    }

    DashboardResponse compute(UUID userId) {
        List<Future<Object>> futures = new ArrayList<>();
        for (DashboardCard card : cards) {
            futures.add(executor.submit(() -> card.compute(userId)));
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        List<DashboardResponse.Card> out = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            DashboardCard card = cards.get(i);
            Future<Object> f = futures.get(i);
            String status;
            Object data = null;
            try {
                data = f.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                status = "OK";
            } catch (TimeoutException e) {
                f.cancel(true);
                status = "ERROR";
                log.warn("dashboard_card_timeout card={}", card.id());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                status = "ERROR";
            } catch (Exception e) {
                status = "ERROR";
                Throwable cause = e.getCause() == null ? e : e.getCause();
                log.warn("dashboard_card_failed card={} error={}", card.id(), cause.getClass().getSimpleName());
            }
            metrics.counter("lifeos.dashboard.cards", "card", card.id(), "status", status).increment();
            out.add(new DashboardResponse.Card(card.id(), status, data));
        }
        return new DashboardResponse(clock.instant(), out);
    }

    @Override
    public void destroy() {
        executor.shutdownNow();
    }
}
