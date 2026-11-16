package com.lifeos.sync.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * Chỉ số sync (SYNC_PROTOCOL §7.3, D91). Không gắn nhãn có userId.
 */
@Component
public class SyncMetrics {

    private final MeterRegistry registry;
    private final Timer pushDuration;
    private final Counter lockTimeouts;
    private final Timer lockWait;
    private final Timer pullDuration;
    private final DistributionSummary pullChanges;

    public SyncMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.pushDuration = Timer.builder("lifeos.sync.push.duration")
                .description("Thời gian xử lý một request POST /sync/push")
                .register(registry);
        this.lockTimeouts = Counter.builder("lifeos.sync.lock.timeouts")
                .description("Số request sync dừng vì hết lock_timeout hoặc deadlock (503)")
                .register(registry);
        this.lockWait = Timer.builder("lifeos.sync.lock.wait")
                .description("Thời gian chạy câu upsert cấp seq trên user_sync_state, GỒM cả thời gian chờ khóa dòng"
                        + " theo user (không phải thời gian chờ thuần, D96)")
                .register(registry);
        this.pullDuration = Timer.builder("lifeos.sync.pull.duration")
                .description("Thời gian xử lý một request GET /sync/pull")
                .register(registry);
        this.pullChanges = DistributionSummary.builder("lifeos.sync.pull.changes")
                .description("Số change trả về trong một trang pull")
                .register(registry);
    }

    public void pull(Duration duration, int changes) {
        pullDuration.record(duration);
        pullChanges.record(changes);
    }

    public void mutations(String status, long count) {
        Counter.builder("lifeos.sync.push.mutations")
                .description("Số mutation đã xử lý theo trạng thái kết quả")
                .tag("status", status)
                .register(registry)
                .increment(count);
    }

    public void pushDuration(Duration duration) {
        pushDuration.record(duration);
    }

    public void lockTimeout() {
        lockTimeouts.increment();
    }

    public void lockWait(Duration duration) {
        lockWait.record(duration);
    }

    /** D169: lifeos.sync.full_resync.required. */
    public void fullResyncRequired() {
        registry.counter("lifeos.sync.full_resync.required").increment();
    }

    /** D169: lifeos.sync.snapshot.duration (một trang). */
    public void snapshot(Duration duration, int items) {
        registry.timer("lifeos.sync.snapshot.duration").record(duration);
        registry.summary("lifeos.sync.snapshot.items").record(items);
    }

    /** D169: lifeos.sync.retention.deleted{table}. */
    public void retentionDeleted(String table, long count) {
        if (count > 0) {
            registry.counter("lifeos.sync.retention.deleted", "table", table).increment(count);
        }
    }

    private final java.util.Map<String, java.util.concurrent.atomic.AtomicLong> violations =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** D172: lifeos.sync.consistency.violations{check} (gauge: kết quả lần kiểm gần nhất). */
    public void consistency(String check, long count) {
        violations.computeIfAbsent(check, c -> registry.gauge("lifeos.sync.consistency.violations",
                java.util.List.of(io.micrometer.core.instrument.Tag.of("check", c)),
                new java.util.concurrent.atomic.AtomicLong())).set(count);
    }
}
