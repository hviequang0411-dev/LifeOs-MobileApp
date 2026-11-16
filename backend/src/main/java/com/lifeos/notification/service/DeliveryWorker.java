package com.lifeos.notification.service;

import com.lifeos.common.mail.EmailMessage;
import com.lifeos.common.mail.EmailSender;
import com.lifeos.identity.api.IdentityDirectory;
import com.lifeos.identity.api.NotificationProfile;
import com.lifeos.notification.channel.PushSender;
import com.lifeos.notification.domain.NotificationRules;
import com.lifeos.notification.repository.NotificationStore;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gửi delivery push/email (PHASE_4 D137, D140): (1) transaction NGẮN chọn việc FOR UPDATE SKIP LOCKED và đánh dấu
 * IN_FLIGHT (hạn khóa 5 phút — tự khôi phục nếu worker chết); (2) gọi mạng NGOÀI mọi transaction; (3) transaction
 * ngắn ghi kết quả (chỉ khi worker còn giữ lease: dòng còn IN_FLIGHT với đúng hạn khóa đã cấp — worker quá hạn không
 * ghi đè kết quả của worker đã lấy lại; gửi vẫn là at-least-once). Lỗi tạm thời: backoff 30s/2m/10m/1h/6h, tối đa 5 lần; lỗi vĩnh viễn:
 * FAILED ngay; UNREGISTERED gỡ token. SECURITY không thử lại quá 24 giờ kể từ tạo. Log/metric không có token, email,
 * nội dung (D147, NTF-13).
 */
@Service
public class DeliveryWorker {

    static final Duration IN_FLIGHT_LEASE = Duration.ofMinutes(5);
    static final Duration SECURITY_MAX_AGE = Duration.ofHours(24);
    private static final int BATCH = 50;
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final NotificationStore store;
    private final PushSender push;
    private final EmailSender email;
    private final IdentityDirectory identity;
    private final UnsubscribeTokens unsubscribe;
    private final TransactionTemplate tx;
    private final JsonMapper json;
    private final MeterRegistry meters;
    private final Clock clock;
    private final String apiBaseUrl;

    public DeliveryWorker(NotificationStore store, PushSender push, EmailSender email, IdentityDirectory identity,
            UnsubscribeTokens unsubscribe, TransactionTemplate tx, JsonMapper json, MeterRegistry meters, Clock clock,
            @Value("${lifeos.notification.api-base-url:${lifeos.mail.app-base-url}}") String apiBaseUrl) {
        this.store = store;
        this.push = push;
        this.email = email;
        this.identity = identity;
        this.unsubscribe = unsubscribe;
        this.tx = tx;
        this.json = json;
        this.meters = meters;
        this.clock = clock;
        this.apiBaseUrl = apiBaseUrl;
    }

    /** Một lượt: xử lý tối đa một lô. @return số delivery đã xử lý */
    public int runOnce() {
        Instant now = clock.instant();
        List<NotificationStore.Claimed> claimed = tx.execute(status -> store.claim(now, now.plus(IN_FLIGHT_LEASE),
                BATCH));
        if (claimed == null) {
            return 0;
        }
        for (NotificationStore.Claimed item : claimed) {
            process(item);
        }
        return claimed.size();
    }

    private void process(NotificationStore.Claimed item) {
        Optional<NotificationStore.NotificationRow> notification = store.notification(item.notificationId());
        Instant now = clock.instant();
        if (notification.isEmpty()) {
            finish(item, "SKIPPED", item.attempts(), "NOTIFICATION_GONE", null);
            return;
        }
        NotificationStore.NotificationRow n = notification.get();
        if (NotificationRules.SECURITY.equals(n.category())
                && item.createdAt().plus(SECURITY_MAX_AGE).isBefore(now)) {
            finish(item, "FAILED", item.attempts(), "EXPIRED", null);
            return;
        }
        PushSender.PushResult result = send(item, n);
        int attempts = item.attempts() + 1;
        switch (result.outcome()) {
            case SENT -> finish(item, "SENT", attempts, null, clock.instant());
            case UNREGISTERED -> {
                tx.executeWithoutResult(status -> {
                    if (item.tokenId() != null) {
                        store.deleteTokenById(item.tokenId());
                    }
                });
                finish(item, "FAILED", attempts, result.errorCode(), null);
            }
            case PERMANENT -> finish(item, "FAILED", attempts, result.errorCode(), null);
            default -> {
                if (attempts >= NotificationRules.MAX_ATTEMPTS) {
                    finish(item, "FAILED", attempts, result.errorCode(), null);
                } else {
                    Instant retry = NotificationRules.retryAt(clock.instant(), attempts,
                            ThreadLocalRandom.current().nextDouble());
                    Boolean owned = tx.execute(status -> store.finish(item, "PENDING", attempts, retry,
                            result.errorCode(), null));
                    meters.counter("lifeos.notification.delivery", "channel", item.channel(), "result",
                            Boolean.TRUE.equals(owned) ? "retry" : "lease_lost").increment();
                }
            }
        }
    }

    private PushSender.PushResult send(NotificationStore.Claimed item, NotificationStore.NotificationRow n) {
        try {
            return "PUSH".equals(item.channel()) ? sendPush(item, n) : sendEmail(item, n);
        } catch (RuntimeException e) {
            return new PushSender.PushResult(PushSender.Outcome.TRANSIENT, e.getClass().getSimpleName());
        }
    }

    private PushSender.PushResult sendPush(NotificationStore.Claimed item, NotificationStore.NotificationRow n) {
        Optional<NotificationStore.PushToken> token = item.tokenId() == null ? Optional.empty()
                : store.token(item.tokenId());
        if (token.isEmpty()) {
            return new PushSender.PushResult(PushSender.Outcome.PERMANENT, "TOKEN_GONE");
        }
        Map<String, String> data = new java.util.LinkedHashMap<>();
        json.readValue(n.data(), MAP).forEach((k, v) -> data.put(k, String.valueOf(v)));
        data.put("notificationId", n.id().toString());
        return push.send(new PushSender.PushMessage(token.get().token(), token.get().platform(), n.title(), n.body(),
                data));
    }

    private PushSender.PushResult sendEmail(NotificationStore.Claimed item, NotificationStore.NotificationRow n)
            throws RuntimeException {
        Optional<NotificationProfile> profile = identity.notificationProfile(item.userId());
        if (profile.isEmpty() || !profile.get().emailVerified()) {
            return new PushSender.PushResult(PushSender.Outcome.PERMANENT, "NO_RECIPIENT");
        }
        String text = n.body() + "\n\n— LifeOS\n";
        if (!NotificationRules.SECURITY.equals(n.category())) {
            text += "\nHủy nhận email loại này: " + apiBaseUrl + "/api/v1/notifications/unsubscribe?token="
                    + unsubscribe.issue(item.userId(), n.category()) + "\n";
        }
        try {
            email.send(new EmailMessage(item.id(), profile.get().email(), n.title(), text));
            return PushSender.PushResult.sent();
        } catch (IllegalArgumentException e) {
            return new PushSender.PushResult(PushSender.Outcome.PERMANENT, "INVALID_MESSAGE");
        } catch (Exception e) {
            return new PushSender.PushResult(PushSender.Outcome.TRANSIENT, e.getClass().getSimpleName());
        }
    }

    private void finish(NotificationStore.Claimed item, String status, int attempts, String error, Instant sentAt) {
        Boolean owned = tx.execute(s -> store.finish(item, status, attempts, clock.instant(), error, sentAt));
        if (!Boolean.TRUE.equals(owned)) {
            // Lease đã bị worker khác lấy lại: kết quả thuộc về chủ mới, không ghi đè (đợt sửa F3).
            meters.counter("lifeos.notification.delivery", "channel", item.channel(), "result", "lease_lost")
                    .increment();
            return;
        }
        meters.counter("lifeos.notification.delivery", "channel", item.channel(), "result",
                status.toLowerCase()).increment();
        if (sentAt != null && item.scheduledFor() != null) {
            meters.timer("lifeos.notification.delivery.lag").record(Duration.between(item.scheduledFor(), sentAt));
        }
    }
}
