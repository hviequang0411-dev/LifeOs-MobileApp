package com.lifeos.analytics.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** Bản no-op: luôn tính trực tiếp; đếm {@code lifeos.cache.requests{result=bypass}} (nhãn hữu hạn, D206). */
@Component
class NoOpAggregateCache implements AggregateCache {

    private final Counter bypass;

    NoOpAggregateCache(MeterRegistry registry) {
        this.bypass = Counter.builder("lifeos.cache.requests").tag("result", "bypass")
                .description("Yêu cầu cache tổng hợp").register(registry);
    }

    @Override
    public <T> T get(UUID userId, String kind, String periodKey, Supplier<T> compute) {
        bypass.increment();
        return compute.get();
    }

    @Override
    public void invalidate(UUID userId) {
        // Không có gì để vô hiệu.
    }
}
