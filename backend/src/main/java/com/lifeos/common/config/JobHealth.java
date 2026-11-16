package com.lifeos.common.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Sức khỏe job định kỳ (BACKEND_FINAL_REPORT §12 OQ3, duyệt 2026-11-15; docs/ops/OBSERVABILITY.md):
 * <ul>
 *   <li>{@code lifeos.job.last_success{job}} (đơn vị giây ⇒ Prometheus: {@code lifeos_job_last_success_seconds}) — thời
 *       điểm epoch của lượt chạy THÀNH CÔNG gần nhất. Lượt ném ngoại lệ không cập nhật.</li>
 *   <li>{@code lifeos.job.expected_interval{job}} (giây) — chu kỳ mặc định của job, để cảnh báo "quá 3 chu kỳ không
 *       thành công" không phải chép tay chu kỳ [PROPOSED: đổi cron qua cấu hình thì con số này không tự đổi theo].</li>
 * </ul>
 * Tập {@code job} hữu hạn (enum), không nhãn theo người dùng (D206). Giá trị ban đầu là thời điểm khởi động: job chưa
 * từng chạy thành công sau khi khởi động vẫn bị cảnh báo sau ba chu kỳ, và khởi động lại không gây cảnh báo giả.
 */
@Component
public class JobHealth {

    /** Các job được theo dõi và chu kỳ mặc định của chúng (cron/fixedDelay trong application.yml). */
    public enum Job {
        RECURRING("recurring", Duration.ofHours(1)),
        RETENTION("retention", Duration.ofDays(1)),
        PURGE("purge", Duration.ofHours(1)),
        EXPORT("export", Duration.ofSeconds(30));

        private final String tag;
        private final Duration expectedInterval;

        Job(String tag, Duration expectedInterval) {
            this.tag = tag;
            this.expectedInterval = expectedInterval;
        }

        public String tag() {
            return tag;
        }

        public Duration expectedInterval() {
            return expectedInterval;
        }
    }

    public static final String LAST_SUCCESS = "lifeos.job.last_success";
    public static final String EXPECTED_INTERVAL = "lifeos.job.expected_interval";

    private final Map<Job, AtomicLong> lastSuccess = new EnumMap<>(Job.class);
    private final Clock clock;

    public JobHealth(MeterRegistry registry, Clock clock) {
        this.clock = clock;
        long started = clock.instant().getEpochSecond();
        for (Job job : Job.values()) {
            AtomicLong value = new AtomicLong(started);
            lastSuccess.put(job, value);
            Gauge.builder(LAST_SUCCESS, value, AtomicLong::doubleValue).tag("job", job.tag()).baseUnit("seconds")
                    .description("Thời điểm (epoch giây) job chạy thành công gần nhất; ban đầu = lúc khởi động")
                    .register(registry);
            Gauge.builder(EXPECTED_INTERVAL, job, j -> j.expectedInterval().toSeconds()).tag("job", job.tag())
                    .baseUnit("seconds").description("Chu kỳ mặc định của job").register(registry);
        }
    }

    /** Chạy một lượt job; chỉ ghi nhận thành công khi {@code run} kết thúc không ném ngoại lệ. */
    public void run(Job job, Runnable run) {
        run.run();
        succeeded(job);
    }

    public void succeeded(Job job) {
        lastSuccess.get(job).set(clock.instant().getEpochSecond());
    }
}
