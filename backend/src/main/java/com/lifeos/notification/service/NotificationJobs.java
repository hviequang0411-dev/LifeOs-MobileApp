package com.lifeos.notification.service;

import com.lifeos.notification.repository.NotificationStore;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Job của module notification (PHASE_4 D131, D137, D141, D142): reminder mỗi 30 giây, delivery mỗi 5 giây, dọn inbox
 * hết hạn (theo lô) và token push cũ hằng ngày. Chỉ chạy khi bật scheduling (test gọi trực tiếp các service).
 */
@Component
class NotificationJobs {

    private static final Logger log = LoggerFactory.getLogger(NotificationJobs.class);
    private static final int CLEANUP_BATCH = 1000;

    private final ReminderRunService reminders;
    private final DeliveryWorker deliveries;
    private final PushTokenService tokens;
    private final NotificationStore store;
    private final TransactionTemplate tx;
    private final Clock clock;

    NotificationJobs(ReminderRunService reminders, DeliveryWorker deliveries, PushTokenService tokens,
            NotificationStore store, TransactionTemplate tx, Clock clock) {
        this.reminders = reminders;
        this.deliveries = deliveries;
        this.tokens = tokens;
        this.store = store;
        this.tx = tx;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${lifeos.notification.reminder-delay:30s}")
    void reminders() {
        reminders.runOnce();
    }

    @Scheduled(fixedDelayString = "${lifeos.notification.delivery-delay:5s}")
    void deliveries() {
        deliveries.runOnce();
    }

    @Scheduled(cron = "${lifeos.cleanup.cron}", zone = "UTC")
    void cleanup() {
        int removed = 0;
        Integer batch;
        do {
            batch = tx.execute(s -> store.deleteExpired(clock.instant(), CLEANUP_BATCH));
            removed += batch == null ? 0 : batch;
        } while (batch != null && batch == CLEANUP_BATCH);
        int staleTokens = tokens.removeStale();
        log.atInfo().addKeyValue("event", "notification_cleanup").addKeyValue("notifications", removed)
                .addKeyValue("pushTokens", staleTokens).log("notification_cleanup");
    }
}
