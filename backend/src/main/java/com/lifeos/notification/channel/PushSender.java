package com.lifeos.notification.channel;

import java.util.Map;

/**
 * Cổng gửi push (FCM, PHASE_4 D137). Gọi NGOÀI transaction DB. Không ném lỗi cho kết quả biết trước — trả
 * {@link PushResult}; lỗi bất ngờ (ném) được coi là tạm thời.
 */
public interface PushSender {

    PushResult send(PushMessage message);

    /** Thông điệp push: token là dữ liệu nhạy cảm (D147) — toString không in token/nội dung. */
    record PushMessage(String token, String platform, String title, String body, Map<String, String> data) {
        @Override
        public String toString() {
            return "PushMessage[platform=" + platform + "]";
        }
    }

    /** SENT; TRANSIENT (5xx, 429, timeout → thử lại); PERMANENT (cấu hình/4xx); UNREGISTERED (gỡ token). */
    enum Outcome {
        SENT, TRANSIENT, PERMANENT, UNREGISTERED
    }

    record PushResult(Outcome outcome, String errorCode) {
        public static PushResult sent() {
            return new PushResult(Outcome.SENT, null);
        }
    }
}
