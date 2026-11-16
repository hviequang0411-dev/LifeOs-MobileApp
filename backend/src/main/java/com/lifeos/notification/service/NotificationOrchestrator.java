package com.lifeos.notification.service;

import com.lifeos.identity.api.IdentityDirectory;
import com.lifeos.identity.api.NotificationProfile;
import com.lifeos.notification.domain.NotificationRules;
import com.lifeos.notification.domain.Templates;
import com.lifeos.notification.repository.NotificationStore;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Điểm hội tụ duy nhất tạo thông báo (PHASE_4 D121): từ outbox (sự kiện nghiệp vụ) và từ reminder đến hạn. Trong MỘT
 * transaction: thông báo in-app (idempotent theo dedupe_key, D122) + một delivery cho mỗi kênh/thiết bị được bật (D136).
 * Push/email trong giờ yên tĩnh được hẹn tới hết khung (D135) trừ SECURITY và reminder khẩn. Nội dung từ mẫu, không dữ
 * liệu nhạy cảm (D138); tiêu đề Task/Event chỉ khi người dùng bật hiện chi tiết.
 * <p>
 * Không gọi ChangeRecorder (thông báo không đồng bộ); khi chạy trong transaction của reminder, các ghi ở đây là ghi
 * nghiệp vụ đứng trước change_logs của reminder (D86).
 */
@Service
public class NotificationOrchestrator {

    /** D142: in-app giữ 90 ngày. */
    static final Duration RETENTION = Duration.ofDays(90);

    /** Yêu cầu tạo thông báo; {@code detailTitle} là nội dung người dùng chỉ dùng khi bật hiện chi tiết. */
    public record Request(UUID userId, String category, String templateKey, Map<String, Object> variables,
            String dedupeKey, Map<String, Object> data, boolean urgent, String detailTitle) {
        @Override
        public String toString() {
            return "NotificationRequest[userId=" + userId + ", category=" + category + "]";
        }
    }

    private final NotificationStore store;
    private final IdentityDirectory identity;
    private final PreferenceService preferences;
    private final JsonMapper json;
    private final MeterRegistry meters;
    private final Clock clock;

    public NotificationOrchestrator(NotificationStore store, IdentityDirectory identity, PreferenceService preferences,
            JsonMapper json, MeterRegistry meters, Clock clock) {
        this.store = store;
        this.identity = identity;
        this.preferences = preferences;
        this.json = json;
        this.meters = meters;
        this.clock = clock;
    }

    /** Transaction riêng (gọi từ handler outbox, ngoài transaction). @return id thông báo nếu mới tạo */
    @Transactional
    public Optional<UUID> create(Request request) {
        return createInCurrentTransaction(request);
    }

    /** Tham gia transaction đang mở (ReminderRunService). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<UUID> createInCurrentTransaction(Request request) {
        Optional<NotificationProfile> found = identity.notificationProfile(request.userId());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        NotificationProfile profile = found.get();
        Map<String, Boolean> prefs = preferences.effective(request.userId());
        Instant now = clock.instant();
        Map<String, Object> variables = new java.util.HashMap<>(request.variables());
        String templateKey = request.templateKey();
        if (request.detailTitle() != null && profile.showDetailsOnLockScreen()) {
            variables.put("title", request.detailTitle());
            templateKey = templateKey.replace(".generic", ".detailed");
        }
        Templates.Rendered rendered = Templates.render(request.category(), profile.locale(), templateKey, variables);
        boolean inApp = prefs.get(request.category() + ":IN_APP");
        // D132: khóa khử trùng đi kèm push để client không hiện lại thông báo local đã nổ cùng khóa.
        Map<String, Object> data = new java.util.LinkedHashMap<>(request.data());
        data.put("dedupeKey", request.dedupeKey());
        UUID id = UUID.randomUUID();
        boolean inserted = store.insertNotification(new NotificationStore.NotificationRow(id, request.userId(),
                request.category(), templateKey, rendered.title(), rendered.body(), json.writeValueAsString(data),
                null, now, inApp ? now.plus(RETENTION) : now), request.dedupeKey());
        if (!inserted) {
            return Optional.empty();
        }
        meters.counter("lifeos.notification.created", "category", request.category()).increment();
        boolean bypassQuiet = NotificationRules.SECURITY.equals(request.category()) || request.urgent();
        Instant scheduled = bypassQuiet ? now : NotificationRules.notBefore(now, ZoneId.of(profile.timezone()),
                profile.quietStart(), profile.quietEnd());
        if (prefs.get(request.category() + ":PUSH")) {
            for (NotificationStore.PushToken token : store.tokensOf(request.userId())) {
                store.insertDelivery(UUID.randomUUID(), request.userId(), id, "PUSH", token.id(), scheduled, now);
            }
        }
        if (prefs.get(request.category() + ":EMAIL") && profile.emailVerified()) {
            store.insertDelivery(UUID.randomUUID(), request.userId(), id, "EMAIL", null, scheduled, now);
        }
        return Optional.of(id);
    }
}
