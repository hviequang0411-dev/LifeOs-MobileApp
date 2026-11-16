package com.lifeos.identity.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Chạy job bù dữ liệu mặc định một lần khi ứng dụng sẵn sàng (PHASE_3 §5.1: "job khởi động bù idempotent"). Tắt cùng
 * các job định kỳ (lifeos.scheduling.enabled=false — profile test gọi trực tiếp {@link UserProvisioningService#backfill}).
 */
@Component
@ConditionalOnProperty(name = "lifeos.scheduling.enabled", havingValue = "true", matchIfMissing = true)
class UserProvisioningBackfillJob {

    private final UserProvisioningService provisioning;

    UserProvisioningBackfillJob(UserProvisioningService provisioning) {
        this.provisioning = provisioning;
    }

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        provisioning.backfill();
    }
}
