package com.lifeos.ai.service;

import com.lifeos.ai.repository.AiStore;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** D269/D295: conversation/context 90 days, usage metadata 365 days, deleted in bounded batches. */
@Component
public class AiRetentionJob {
    private static final Logger log = LoggerFactory.getLogger(AiRetentionJob.class);
    private static final int BATCH = 1_000;
    private static final Duration USAGE_RETENTION = Duration.ofDays(365);
    private final AiStore store;
    private final TransactionTemplate tx;
    private final Clock clock;

    public AiRetentionJob(AiStore store, TransactionTemplate tx, Clock clock) {
        this.store = store;
        this.tx = tx;
        this.clock = clock;
    }

    @Scheduled(cron = "${lifeos.ai.retention-cron:0 40 3 * * *}", zone = "UTC")
    public void runOnce() {
        int conversations = drain(() -> store.deleteExpiredConversations(clock.instant(), BATCH));
        int usage = drain(() -> store.deleteUsageBefore(clock.instant().minus(USAGE_RETENTION), BATCH));
        log.atInfo().addKeyValue("event", "ai_retention")
                .addKeyValue("conversations", conversations).addKeyValue("usage", usage).log("ai_retention");
    }

    private int drain(java.util.function.IntSupplier operation) {
        int total = 0;
        int removed;
        do {
            Integer value = tx.execute(status -> operation.getAsInt());
            removed = value == null ? 0 : value;
            total += removed;
        } while (removed == BATCH);
        return total;
    }
}
