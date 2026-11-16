package com.lifeos.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.identity.service.AccountDeletionService;
import com.lifeos.identity.service.DataExportService;
import com.lifeos.support.IntegrationTestBase;
import com.lifeos.support.MutableClock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OQ3 (duyệt 2026-11-15): gauge {@code lifeos.job.last_success{job}} cho recurring / retention / purge / export — cập
 * nhật khi lượt job chạy xong không lỗi, giữ nguyên khi lỗi; nhãn job hữu hạn.
 */
class JobHealthIT extends IntegrationTestBase {

    @Autowired
    MeterRegistry registry;

    @Autowired
    AccountDeletionService deletion;

    @Autowired
    DataExportService exports;

    private double lastSuccess(String job) {
        return registry.get(JobHealth.LAST_SUCCESS).tag("job", job).gauge().value();
    }

    @Test
    void Oq3_eachScheduledJobRecordsItsLastSuccessfulRun() {
        assertThat(registry.find(JobHealth.LAST_SUCCESS).gauges()).extracting(g -> g.getId().getTag("job"))
                .containsExactlyInAnyOrder("recurring", "retention", "purge", "export");
        assertThat(registry.get(JobHealth.EXPECTED_INTERVAL).tag("job", "recurring").gauge().value())
                .isEqualTo(3600.0);
        assertThat(registry.get(JobHealth.EXPECTED_INTERVAL).tag("job", "export").gauge().value()).isEqualTo(30.0);

        clock.advance(Duration.ofSeconds(90));
        double now = clock.instant().getEpochSecond();
        assertThat(lastSuccess("recurring")).isLessThan(now);

        // Gọi đúng phương thức @Scheduled của từng job (job tắt trong test nên gọi trực tiếp).
        ReflectionTestUtils.invokeMethod(context.getBean("recurringTransactionJob"), "run");
        assertThat(lastSuccess("recurring")).isEqualTo(now);
        ReflectionTestUtils.invokeMethod(context.getBean("syncJobs"), "retention");
        assertThat(lastSuccess("retention")).isEqualTo(now);
        deletion.purgeDue();
        assertThat(lastSuccess("purge")).isEqualTo(now);
        exports.runPending();
        assertThat(lastSuccess("export")).isEqualTo(now);
    }

    @Test
    void Oq3_aFailedRunDoesNotMoveTheGaugeAndStartupIsTheInitialValue() {
        Instant start = Instant.parse("2026-11-15T00:00:00Z");
        MutableClock testClock = new MutableClock(start);
        SimpleMeterRegistry simple = new SimpleMeterRegistry();
        JobHealth health = new JobHealth(simple, testClock);
        double initial = simple.get(JobHealth.LAST_SUCCESS).tag("job", "purge").gauge().value();
        assertThat(initial).isEqualTo((double) start.getEpochSecond());

        testClock.advance(Duration.ofHours(2));
        assertThatThrownBy(() -> health.run(JobHealth.Job.PURGE, () -> {
            throw new IllegalStateException("job hỏng");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(simple.get(JobHealth.LAST_SUCCESS).tag("job", "purge").gauge().value()).isEqualTo(initial);

        health.run(JobHealth.Job.PURGE, () -> {
        });
        assertThat(simple.get(JobHealth.LAST_SUCCESS).tag("job", "purge").gauge().value())
                .isEqualTo((double) testClock.instant().getEpochSecond());
        assertThat(simple.get(JobHealth.LAST_SUCCESS).tag("job", "export").gauge().value()).isEqualTo(initial);
    }
}
