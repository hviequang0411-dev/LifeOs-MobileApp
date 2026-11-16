package com.lifeos.common.outbox;

import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job dọn outbox (SYSTEM_ARCHITECTURE §3.12, DATABASE_DESIGN §7): xóa sự kiện SENT/FAILED đã hoàn tất quá 7 ngày.
 * Idempotent, chịu được chạy lặp/chạy trễ (một câu DELETE có điều kiện).
 */
@Component
public class OutboxCleanupJob {

    /** DATABASE_DESIGN §7: "Xóa sau 7 ngày" [CONFIRMED]. */
    public static final Duration RETENTION = Duration.ofDays(7);

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupJob.class);

    private final OutboxStore store;
    private final Clock clock;

    public OutboxCleanupJob(OutboxStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Scheduled(cron = "${lifeos.cleanup.cron}", zone = "UTC")
    public int cleanup() {
        int deleted = store.deleteFinishedBefore(clock.instant().minus(RETENTION));
        log.atInfo().addKeyValue("event", "outbox_cleanup").addKeyValue("deleted", deleted).log("outbox_cleanup");
        return deleted;
    }
}
