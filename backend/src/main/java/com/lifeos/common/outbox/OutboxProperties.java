package com.lifeos.common.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tham số worker outbox (D23). Tài liệu chỉ chốt cơ chế (lease, backoff mũ, FAILED sau số lần tối đa) và chu kỳ ~2 giây;
 * các con số còn lại là [PROPOSED] Giai đoạn 4 (xem báo cáo BACKEND_PHASE4_OUTBOX_REPORT.md).
 *
 * @param pollInterval  chu kỳ quét (SYSTEM_ARCHITECTURE §3.11: ~2 giây)
 * @param batchSize     số sự kiện nhận mỗi lần quét
 * @param lease         thời hạn giữ sự kiện PROCESSING; quá hạn → trả về PENDING (worker chết giữa chừng)
 * @param maxAttempts   số lần thử tối đa; đạt tới → FAILED
 * @param backoffBase   chờ sau lần thất bại đầu tiên; nhân đôi mỗi lần
 * @param backoffMax    trần thời gian chờ
 */
@ConfigurationProperties(prefix = "lifeos.outbox")
public record OutboxProperties(Duration pollInterval, int batchSize, Duration lease, int maxAttempts,
        Duration backoffBase, Duration backoffMax) {

    public OutboxProperties {
        if (batchSize < 1 || maxAttempts < 1 || lease.isNegative() || lease.isZero()
                || backoffBase.isNegative() || backoffMax.compareTo(backoffBase) < 0) {
            throw new IllegalArgumentException("Cấu hình lifeos.outbox không hợp lệ");
        }
    }

    /** Thời gian chờ trước lần thử thứ {@code attemptsSoFar + 1}: base × 2^(attemptsSoFar − 1), tối đa backoffMax. */
    public Duration backoffAfter(int attemptsSoFar) {
        Duration delay = backoffBase;
        for (int i = 1; i < attemptsSoFar && delay.compareTo(backoffMax) < 0; i++) {
            delay = delay.multipliedBy(2);
        }
        return delay.compareTo(backoffMax) > 0 ? backoffMax : delay;
    }
}
