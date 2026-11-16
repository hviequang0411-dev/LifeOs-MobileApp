package com.lifeos.common.outbox;

/**
 * Xử lý một loại sự kiện outbox (SYSTEM_ARCHITECTURE §3.11, D17). Module sở hữu nghiệp vụ cài đặt (Phase 1: identity
 * gửi email). Được gọi NGOÀI mọi transaction DB, nên được phép gọi hệ thống ngoài (SMTP).
 * <ul>
 *   <li>Trả về bình thường = đã giao xong → SENT.</li>
 *   <li>Ném {@link OutboxPermanentFailure} = không bao giờ thành công (payload hỏng, token đã hết hạn…) → FAILED ngay.</li>
 *   <li>Ném lỗi khác = lỗi tạm thời → thử lại theo backoff, FAILED sau số lần tối đa (D23).</li>
 * </ul>
 * Giao ít nhất một lần: handler phải idempotent theo event id ở mức có thể (JAVA_SPRING_RULES §10.3).
 */
public interface OutboxEventHandler {

    /** Giá trị outbox_events.event_type mà handler này xử lý. */
    String eventType();

    void handle(OutboxEvent event) throws Exception;
}
