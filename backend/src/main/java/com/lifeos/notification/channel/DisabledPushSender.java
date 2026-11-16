package com.lifeos.notification.channel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Push tắt (mặc định khi chưa cấu hình FCM, lifeos.fcm.enabled=false): mọi delivery PUSH kết thúc FAILED
 * {@code PUSH_DISABLED} ngay — không thử lại, không chặn kênh khác.
 */
@Component
@ConditionalOnProperty(name = "lifeos.fcm.enabled", havingValue = "false", matchIfMissing = true)
class DisabledPushSender implements PushSender {

    @Override
    public PushResult send(PushMessage message) {
        return new PushResult(Outcome.PERMANENT, "PUSH_DISABLED");
    }
}
