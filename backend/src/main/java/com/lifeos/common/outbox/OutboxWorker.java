package com.lifeos.common.outbox;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Outbox worker (SYSTEM_ARCHITECTURE §3.11, D23, D28; JAVA_SPRING_RULES §10). Mỗi lượt:
 * <ol>
 *   <li>Thu hồi sự kiện PROCESSING quá lease (worker chết giữa chừng) — tính là một lần thử.</li>
 *   <li>Câu lệnh ngắn: nhận tối đa batchSize sự kiện đến hạn bằng FOR UPDATE SKIP LOCKED, đặt PROCESSING + lease.</li>
 *   <li>NGOÀI transaction: gọi {@link OutboxEventHandler} (gửi email…).</li>
 *   <li>Câu lệnh ngắn: SENT (payload ghi đè {}), hoặc PENDING với attempts+1 và backoff mũ, hoặc FAILED (payload {})
 *       khi lỗi vĩnh viễn / hết số lần thử. Chỉ ghi khi còn giữ lease.</li>
 * </ol>
 * Không có transaction Spring nào bao quanh lời gọi handler, nên không có kết nối DB nào bị giữ trong lúc gọi mạng.
 * Log và metric chỉ có id, loại sự kiện, kết quả, số lần thử, tên lớp lỗi — không email, token, nội dung thư
 * (SECURITY_RULES §11). Giao ít nhất một lần.
 */
@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);

    static final String NO_HANDLER = "NO_HANDLER";

    private final OutboxStore store;
    private final Map<String, OutboxEventHandler> handlers;
    private final OutboxProperties properties;
    private final Clock clock;
    private final MeterRegistry meters;

    public OutboxWorker(OutboxStore store, List<OutboxEventHandler> handlers, OutboxProperties properties,
            Clock clock, MeterRegistry meters) {
        this.store = store;
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(OutboxEventHandler::eventType,
                Function.identity()));
        this.properties = properties;
        this.clock = clock;
        this.meters = meters;
        Gauge.builder("lifeos.outbox.pending", store, OutboxStore::countPending)
                .description("Số sự kiện outbox đang chờ gửi (PENDING)")
                .register(meters);
    }

    /** Chu kỳ ~2 giây (§3.11); chỉ chạy khi lifeos.scheduling.enabled (SchedulingConfig). */
    @Scheduled(fixedDelayString = "${lifeos.outbox.poll-interval}")
    public void poll() {
        try {
            runOnce();
        } catch (RuntimeException e) {
            // Lỗi hạ tầng (DB tạm mất…): lượt sau thử lại; không lộ chi tiết dữ liệu.
            log.atWarn().addKeyValue("event", "outbox_poll_failed")
                    .addKeyValue("error", e.getClass().getSimpleName()).log("outbox_poll_failed");
        }
    }

    /** Một lượt xử lý. @return số sự kiện đã nhận trong lượt này */
    public int runOnce() {
        Instant now = clock.instant();
        int reclaimed = store.reclaimExpired(now, properties.maxAttempts());
        if (reclaimed > 0) {
            log.atWarn().addKeyValue("event", "outbox_lease_expired").addKeyValue("count", reclaimed)
                    .log("outbox_lease_expired");
        }
        List<OutboxStore.Claimed> claimed = store.claim(now, now.plus(properties.lease()), properties.batchSize());
        for (OutboxStore.Claimed item : claimed) {
            process(item);
        }
        return claimed.size();
    }

    private void process(OutboxStore.Claimed item) {
        OutboxEvent event = item.event();
        OutboxEventHandler handler = handlers.get(event.eventType());
        String outcome;
        String error = null;
        boolean recorded;
        try {
            if (handler == null) {
                throw new OutboxPermanentFailure(NO_HANDLER);
            }
            handler.handle(event);
            recorded = store.markSent(item, clock.instant());
            outcome = "sent";
        } catch (OutboxPermanentFailure e) {
            error = e.reasonCode();
            recorded = store.markFailed(item, clock.instant(), error);
            outcome = "failed";
        } catch (Exception e) {
            // Chỉ tên lớp: thông điệp lỗi SMTP có thể chứa địa chỉ email người nhận.
            error = e.getClass().getSimpleName();
            int attempts = event.attempts() + 1;
            if (attempts >= properties.maxAttempts()) {
                recorded = store.markFailed(item, clock.instant(), error);
                outcome = "failed";
            } else {
                recorded = store.markRetry(item, clock.instant().plus(properties.backoffAfter(attempts)), error);
                outcome = "retry";
            }
        }
        if (!recorded) {
            outcome = "lease_lost";
        }
        Counter.builder("lifeos.outbox.events")
                .description("Số sự kiện outbox đã xử lý theo loại và kết quả")
                .tag("type", event.eventType())
                .tag("outcome", outcome)
                .register(meters)
                .increment();
        log.atInfo()
                .addKeyValue("event", "outbox_event")
                .addKeyValue("eventId", event.id())
                .addKeyValue("eventType", event.eventType())
                .addKeyValue("outcome", outcome)
                .addKeyValue("attempt", event.attempts() + 1)
                .addKeyValue("error", error)
                .log("outbox_event");
    }
}
