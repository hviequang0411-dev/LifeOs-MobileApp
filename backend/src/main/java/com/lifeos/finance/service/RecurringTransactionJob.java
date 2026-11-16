package com.lifeos.finance.service;

import com.lifeos.common.config.JobHealth;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RecurringTransactionJob (PHASE_3 §7): chạy mỗi giờ để chịu được lần chạy trễ và nhiều múi giờ. Lượt chạy xong không
 * lỗi cập nhật gauge {@code lifeos.job.last_success{job="recurring"}}.
 */
@Component
class RecurringTransactionJob {

    private final RecurringRunService runs;
    private final JobHealth health;

    RecurringTransactionJob(RecurringRunService runs, JobHealth health) {
        this.runs = runs;
        this.health = health;
    }

    @Scheduled(cron = "${lifeos.finance.recurring-cron:0 5 * * * *}", zone = "UTC")
    void run() {
        health.run(JobHealth.Job.RECURRING, runs::runOnce);
    }
}
