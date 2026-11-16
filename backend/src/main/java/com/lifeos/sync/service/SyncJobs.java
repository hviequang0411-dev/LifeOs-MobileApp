package com.lifeos.sync.service;

import com.lifeos.common.config.JobHealth;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Job của module sync (PHASE_5): retention hằng ngày (D153–D156), kiểm tra nhất quán hằng tuần (D172). */
@Component
class SyncJobs {

    private final SyncRetentionService retention;
    private final SyncConsistencyService consistency;
    private final JobHealth health;

    SyncJobs(SyncRetentionService retention, SyncConsistencyService consistency, JobHealth health) {
        this.health = health;
        this.retention = retention;
        this.consistency = consistency;
    }

    @Scheduled(cron = "${lifeos.sync.retention-cron:0 30 3 * * *}", zone = "UTC")
    void retention() {
        health.run(JobHealth.Job.RETENTION, retention::runAll);
    }

    @Scheduled(cron = "${lifeos.sync.consistency-cron:0 0 4 * * SUN}", zone = "UTC")
    void consistency() {
        consistency.run();
    }
}
