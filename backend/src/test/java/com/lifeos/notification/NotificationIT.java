package com.lifeos.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.outbox.OutboxWorker;
import com.lifeos.common.outbox.OutboxWriter;
import com.lifeos.notification.channel.PushSender;
import com.lifeos.notification.domain.NotificationRules;
import com.lifeos.notification.service.DeliveryWorker;
import com.lifeos.notification.service.NotificationOrchestrator;
import com.lifeos.notification.service.UnsubscribeTokens;
import com.lifeos.support.ApiTestBase;
import com.lifeos.support.RecordingEmailSender;
import com.lifeos.support.RecordingPushSender;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/** Orchestrator, giờ yên tĩnh, tùy chọn, gửi/thử lại, email, hủy đăng ký, token, inbox, sự kiện outbox (NTF-03..12). */
class NotificationIT extends ApiTestBase {

    @Autowired
    NotificationOrchestrator orchestrator;

    @Autowired
    DeliveryWorker deliveries;

    @Autowired
    RecordingPushSender push;

    @Autowired
    RecordingEmailSender email;

    @Autowired
    UnsubscribeTokens unsubscribeTokens;

    @Autowired
    OutboxWriter outbox;

    @Autowired
    OutboxWorker outboxWorker;

    @Autowired
    TransactionTemplate tx;

    private UUID device(TestUser user) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO devices (id, user_id, name, platform) VALUES (:id, :u, 'Pixel', 'ANDROID')")
                .param("id", id).param("u", user.id()).update();
        return id;
    }

    private String registerToken(TestUser user, UUID device) throws Exception {
        String token = "fcm-" + UUID.randomUUID();
        MvcResult r = call(user, org.springframework.http.HttpMethod.PUT, "/api/v1/devices/" + device + "/push-token",
                body("token", token, "platform", "ANDROID"));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(204);
        return token;
    }

    /** Giờ yên tĩnh đặt qua REST (đường JPA như production — cột time đi qua cấu hình JDBC của Hibernate). */
    private void settings(TestUser user, String zone, String quietStart, String quietEnd) throws Exception {
        jdbc.sql("INSERT INTO user_settings (id, user_id, timezone) VALUES (:id, :u, :z) ON CONFLICT (user_id) DO"
                + " NOTHING").param("id", UUID.randomUUID()).param("u", user.id()).param("z", zone).update();
        long version = version(readJson(get(user, "/api/v1/me/settings")));
        MvcResult r = patch(user, "/api/v1/me/settings", body("version", version, "quietStart", quietStart,
                "quietEnd", quietEnd));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
    }

    private UUID notify(TestUser user, String category, boolean urgent) {
        return orchestrator.create(new NotificationOrchestrator.Request(user.id(), category,
                "REMINDER".equals(category) ? "reminder.generic" : "finance.budget.threshold.80",
                Map.of("threshold", 80, "month", "2026-11"), "test:" + UUID.randomUUID(), Map.of("type", "test"),
                urgent, null)).orElseThrow();
    }

    private List<Map<String, Object>> deliveriesOf(UUID notificationId) {
        return jdbc.sql("SELECT * FROM notification_deliveries WHERE notification_id = :n ORDER BY channel")
                .param("n", notificationId).query().listOfRows();
    }

    private Instant scheduled(Map<String, Object> delivery) {
        return instant(delivery.get("scheduled_for"));
    }

    private static Instant instant(Object value) {
        return value instanceof java.sql.Timestamp t ? t.toInstant() : ((java.time.OffsetDateTime) value).toInstant();
    }

    private void verifyEmail(TestUser user) {
        jdbc.sql("UPDATE users SET email_verified_at = now() WHERE id = :u").param("u", user.id()).update();
    }

    private String emailOf(TestUser user) {
        return jdbc.sql("SELECT email FROM users WHERE id = :u").param("u", user.id()).query(String.class).single();
    }

    @Test
    void Ntf_03_quietHoursAcrossMidnightDelayPushButNotInAppSecurityOrUrgent() throws Exception {
        TestUser user = newUser();
        registerToken(user, device(user));
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalTime now = LocalTime.ofInstant(clock.instant(), zone);
        // Khung yên tĩnh chứa "bây giờ", kết thúc sau 2 giờ và có thể qua nửa đêm.
        LocalTime start = now.minusHours(1);
        LocalTime end = now.plusHours(2);
        LocalTime quietStart = start.withSecond(0).withNano(0);
        LocalTime quietEnd = end.withSecond(0).withNano(0);
        settings(user, zone.getId(), quietStart.toString(), quietEnd.toString());
        UUID normal = notify(user, "REMINDER", false);
        Instant expected = NotificationRules.notBefore(clock.instant(), zone, quietStart, quietEnd);
        assertThat(expected).isAfter(clock.instant());
        assertThat(scheduled(deliveriesOf(normal).get(0))).isEqualTo(expected);
        assertThat(readJson(get(user, "/api/v1/notifications/unread-count")).get("unread")).isEqualTo(1);
        UUID urgent = notify(user, "REMINDER", true);
        assertThat(scheduled(deliveriesOf(urgent).get(0))).isEqualTo(clock.instant());
        UUID security = orchestrator.create(new NotificationOrchestrator.Request(user.id(), "SECURITY",
                "security.generic", Map.of(), "test:" + UUID.randomUUID(), Map.of(), false, null)).orElseThrow();
        assertThat(scheduled(deliveriesOf(security).get(0))).as("SECURITY bỏ qua giờ yên tĩnh")
                .isEqualTo(clock.instant());

        assertThat(NotificationRules.notBefore(Instant.parse("2026-11-07T16:30:00Z"), zone, LocalTime.of(22, 0),
                LocalTime.of(7, 0))).as("23:30 trong khung 22:00–07:00 → 07:00 hôm sau")
                .isEqualTo(Instant.parse("2026-11-08T00:00:00Z"));
        assertThat(NotificationRules.notBefore(Instant.parse("2026-11-07T22:30:00Z"), zone, LocalTime.of(22, 0),
                LocalTime.of(7, 0))).as("05:30 sáng → 07:00 cùng ngày").isEqualTo(Instant.parse("2026-11-08T00:00:00Z"));
        assertThat(NotificationRules.notBefore(Instant.parse("2026-11-07T05:00:00Z"), zone, LocalTime.of(22, 0),
                LocalTime.of(7, 0))).as("12:00 ngoài khung").isEqualTo(Instant.parse("2026-11-07T05:00:00Z"));
    }

    @Test
    void Ntf_04_preferencesDefaultsLockedSecurityAndMissingRows() throws Exception {
        TestUser user = newUser();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) readJson(get(user,
                "/api/v1/notifications/preferences")).get("items");
        assertThat(items).hasSize(18);
        assertThat(items).filteredOn(i -> "EMAIL".equals(i.get("channel")) && Boolean.TRUE.equals(i.get("enabled")))
                .extracting(i -> i.get("category")).containsExactly("SECURITY");
        assertThat(items).filteredOn(i -> "PUSH".equals(i.get("channel")) && Boolean.TRUE.equals(i.get("enabled")))
                .extracting(i -> i.get("category")).containsExactlyInAnyOrder("SECURITY", "REMINDER", "TASK", "HABIT");
        MvcResult locked = call(user, org.springframework.http.HttpMethod.PUT, "/api/v1/notifications/preferences",
                json.writeValueAsString(Map.of("items", List.of(Map.of("category", "SECURITY", "channel", "EMAIL",
                        "enabled", false)))));
        assertThat(locked.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(locked).get("errorCode")).isEqualTo("NOTIFICATION_PREFERENCE_LOCKED");
        MvcResult ok = call(user, org.springframework.http.HttpMethod.PUT, "/api/v1/notifications/preferences",
                json.writeValueAsString(Map.of("items", List.of(Map.of("category", "REMINDER", "channel", "PUSH",
                        "enabled", false), Map.of("category", "FINANCE", "channel", "IN_APP", "enabled", false)))));
        assertThat(ok.getResponse().getStatus()).isEqualTo(200);
        registerToken(user, device(user));
        UUID reminder = notify(user, "REMINDER", false);
        assertThat(deliveriesOf(reminder)).as("push REMINDER đã tắt").isEmpty();
        UUID finance = notify(user, "FINANCE", false);
        assertThat(items(get(user, "/api/v1/notifications"))).extracting(n -> n.get("id"))
                .contains(reminder.toString()).doesNotContain(finance.toString());
    }

    @Test
    void Ntf_05_retryBackoffPermanentFailureUnregisteredTokenAndInFlightRecovery() throws Exception {
        TestUser user = newUser();
        String transientToken = registerToken(user, device(user));
        String deadToken = registerToken(user, device(user));
        push.respond(transientToken, PushSender.Outcome.TRANSIENT);
        push.respond(deadToken, PushSender.Outcome.UNREGISTERED);
        UUID n = notify(user, "REMINDER", true);
        deliveries.runOnce();
        Map<String, Object> retry = deliveriesOf(n).stream().filter(d -> d.get("status").equals("PENDING")).findFirst()
                .orElseThrow();
        assertThat(((Number) retry.get("attempt_count")).intValue()).isEqualTo(1);
        Instant next = instant(retry.get("next_attempt_at"));
        assertThat(Duration.between(clock.instant(), next)).isBetween(Duration.ofSeconds(23), Duration.ofSeconds(37));
        assertThat(deliveriesOf(n)).filteredOn(d -> d.get("status").equals("FAILED"))
                .extracting(d -> d.get("last_error_code")).containsExactly("TEST_UNREGISTERED");
        assertThat(jdbc.sql("SELECT count(*) FROM device_push_tokens WHERE token = :t").param("t", deadToken)
                .query(Integer.class).single()).as("token chết bị gỡ").isZero();
        for (int attempt = 2; attempt <= 5; attempt++) {
            jdbc.sql("UPDATE notification_deliveries SET next_attempt_at = now() - interval '1 second' WHERE id = :id")
                    .param("id", retry.get("id")).update();
            deliveries.runOnce();
        }
        Map<String, Object> exhausted = jdbc.sql("SELECT * FROM notification_deliveries WHERE id = :id")
                .param("id", retry.get("id")).query().singleRow();
        assertThat(exhausted.get("status")).isEqualTo("FAILED");
        assertThat(((Number) exhausted.get("attempt_count")).intValue()).isEqualTo(5);

        push.respond(transientToken, PushSender.Outcome.SENT);
        UUID stuck = notify(user, "REMINDER", true);
        jdbc.sql("UPDATE notification_deliveries SET status = 'IN_FLIGHT', next_attempt_at = :past WHERE"
                + " notification_id = :n").param("n", stuck)
                .param("past", clock.instant().minusSeconds(1).atOffset(ZoneOffset.UTC)).update();
        deliveries.runOnce();
        assertThat(deliveriesOf(stuck)).extracting(d -> d.get("status")).containsExactly("SENT");
        assertThat(push.sentTo(transientToken)).isNotEmpty();
        assertThat(push.sentTo(transientToken).get(0).data()).containsKey("dedupeKey");
    }

    @Test
    void Ntf_06_networkCallsHappenOutsideDatabaseTransactions() throws Exception {
        TestUser user = newUser();
        registerToken(user, device(user));
        notify(user, "REMINDER", true);
        deliveries.runOnce();
        assertThat(push.calledInsideTransaction()).isEmpty();
    }

    @Test
    void Ntf_07_emailUsesTemplatesWithoutSensitiveDataAndAddsAnUnsubscribeLink() throws Exception {
        TestUser user = newUser();
        verifyEmail(user);
        call(user, org.springframework.http.HttpMethod.PUT, "/api/v1/notifications/preferences",
                json.writeValueAsString(Map.of("items", List.of(Map.of("category", "FINANCE", "channel", "EMAIL",
                        "enabled", true)))));
        UUID n = orchestrator.create(new NotificationOrchestrator.Request(user.id(), "FINANCE",
                "finance.budget.threshold.80", Map.of("threshold", 80, "month", "2026-11\r\nBcc: x@evil.test",
                        "amount", "123456789"), "test:" + UUID.randomUUID(), Map.of(), false, null)).orElseThrow();
        deliveries.runOnce();
        assertThat(deliveriesOf(n)).extracting(d -> d.get("status")).containsExactly("SENT");
        var sent = email.sentTo(emailOf(user));
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).subject()).doesNotContain("\r").doesNotContain("\n");
        assertThat(sent.get(0).text()).doesNotContain("123456789").doesNotContain("\r\nBcc")
                .contains("/api/v1/notifications/unsubscribe?token=");
    }

    @Test
    void Ntf_08_unsubscribeTokensDisableOneCategoryAndRejectForgedExpiredOrSecurity() throws Exception {
        TestUser user = newUser();
        String token = unsubscribeTokens.issue(user.id(), "FINANCE");
        assertThat(get(null, "/api/v1/notifications/unsubscribe?token=" + token).getResponse().getStatus())
                .isEqualTo(204);
        assertThat(get(null, "/api/v1/notifications/unsubscribe?token=" + token).getResponse().getStatus())
                .as("idempotent").isEqualTo(204);
        assertThat(jdbc.sql("SELECT enabled FROM notification_preferences WHERE user_id = :u AND category ="
                + " 'FINANCE' AND channel = 'EMAIL'").param("u", user.id()).query(Boolean.class).single()).isFalse();
        assertThat(get(null, "/api/v1/notifications/unsubscribe?token=" + token.substring(0, token.length() - 2)
                + "AA").getResponse().getStatus()).isEqualTo(400);
        assertThat(get(null, "/api/v1/notifications/unsubscribe?token=" + unsubscribeTokens.issue(user.id(),
                "SECURITY")).getResponse().getStatus()).isEqualTo(400);
        String expired = unsubscribeTokens.issue(user.id(), "TASK");
        clock.advance(Duration.ofDays(31));
        try {
            assertThat(get(null, "/api/v1/notifications/unsubscribe?token=" + expired).getResponse().getStatus())
                    .isEqualTo(400);
        } finally {
            clock.advance(Duration.ofSeconds(1));
        }
    }

    @Test
    void Ntf_09_pushTokensMoveBetweenOwnersAreCappedAtTenAndShowOnlySixCharacters() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        UUID aliceDevice = device(alice);
        String token = registerToken(alice, aliceDevice);
        UUID bobDevice = device(bob);
        assertThat(call(bob, org.springframework.http.HttpMethod.PUT, "/api/v1/devices/" + bobDevice + "/push-token",
                body("token", token, "platform", "ANDROID")).getResponse().getStatus()).isEqualTo(204);
        assertThat(jdbc.sql("SELECT user_id FROM device_push_tokens WHERE token = :t").param("t", token)
                .query(UUID.class).single()).isEqualTo(bob.id());
        assertThat(call(bob, org.springframework.http.HttpMethod.PUT, "/api/v1/devices/" + aliceDevice
                + "/push-token", body("token", "x" + token, "platform", "ANDROID")).getResponse().getStatus())
                .as("thiết bị của người khác").isEqualTo(404);
        for (int i = 0; i < 11; i++) {
            registerToken(alice, device(alice));
        }
        assertThat(jdbc.sql("SELECT count(*) FROM device_push_tokens WHERE user_id = :u").param("u", alice.id())
                .query(Integer.class).single()).isEqualTo(10);
        Map<String, Object> list = readJson(get(bob, "/api/v1/devices"));
        assertThat(list.toString()).doesNotContain(token);
        assertThat(items(get(bob, "/api/v1/devices"))).extracting(d -> d.get("pushTokenSuffix"))
                .contains(token.substring(token.length() - 6));
        assertThat(delete(bob, "/api/v1/devices/" + bobDevice).getResponse().getStatus()).isEqualTo(204);
        assertThat(jdbc.sql("SELECT count(*) FROM device_push_tokens WHERE device_id = :d").param("d", bobDevice)
                .query(Integer.class).single()).as("thu hồi thiết bị gỡ token").isZero();
    }

    @Test
    void Ntf_10_inboxCursorReadIsIdempotentAndExpiredRowsAreCleanedInBatches() throws Exception {
        TestUser user = newUser();
        for (int i = 0; i < 5; i++) {
            notify(user, "REMINDER", false);
            clock.advance(Duration.ofMillis(1));
        }
        Map<String, Object> first = readJson(get(user, "/api/v1/notifications?limit=2"));
        String cursor = (String) first.get("nextCursor");
        Map<String, Object> second = readJson(get(user, "/api/v1/notifications?limit=2&cursor=" + cursor));
        assertThat((List<?>) second.get("items")).hasSize(2);
        String id = (String) ((Map<?, ?>) ((List<?>) first.get("items")).get(0)).get("id");
        assertThat(patch(user, "/api/v1/notifications/" + id + "/read", null).getResponse().getStatus())
                .isEqualTo(200);
        Object readAt = readJson(get(user, "/api/v1/notifications?limit=1")).get("items");
        assertThat(patch(user, "/api/v1/notifications/" + id + "/read", null).getResponse().getStatus())
                .isEqualTo(200);
        assertThat(readJson(get(user, "/api/v1/notifications?limit=1")).get("items")).isEqualTo(readAt);
        assertThat(readJson(get(user, "/api/v1/notifications/unread-count")).get("unread")).isEqualTo(4);
        assertThat(post(user, "/api/v1/notifications/read-all", null).getResponse().getStatus()).isEqualTo(204);
        assertThat(readJson(get(user, "/api/v1/notifications/unread-count")).get("unread")).isEqualTo(0);
        jdbc.sql("UPDATE notifications SET expires_at = now() - interval '1 day' WHERE user_id = :u")
                .param("u", user.id()).update();
        var store = context.getBean(com.lifeos.notification.repository.NotificationStore.class);
        int total = 0;
        Integer removed;
        do {
            removed = tx.execute(s -> store.deleteExpired(Instant.now(), 3));
            total += removed;
        } while (removed == 3);
        assertThat(total).isGreaterThanOrEqualTo(5);
        assertThat(items(get(user, "/api/v1/notifications"))).isEmpty();
    }

    @Test
    void Ntf_12_budgetEventsBecomeNotificationsAndBadPayloadsFailWithoutBlocking() throws Exception {
        TestUser user = newUser();
        UUID good = tx.execute(s -> outbox.write("BUDGET_THRESHOLD", "budget", UUID.randomUUID(),
                payload(user.id(), "FINANCE", "finance.budget.threshold.100")));
        UUID bad = tx.execute(s -> outbox.write("BUDGET_THRESHOLD", "budget", UUID.randomUUID(),
                payload(user.id(), "FINANCE", "no.such.template")));
        UUID unknown = tx.execute(s -> outbox.write("HABIT_STREAK_AT_RISK", "habit", UUID.randomUUID(),
                payload(user.id(), "HABIT", "x")));
        for (int i = 0; i < 5; i++) {
            outboxWorker.runOnce();
        }
        assertThat(status(good)).isEqualTo("SENT");
        assertThat(status(bad)).isEqualTo("FAILED");
        assertThat(status(unknown)).isEqualTo("FAILED");
        List<Map<String, Object>> inbox = items(get(user, "/api/v1/notifications"));
        assertThat(inbox).hasSize(1);
        assertThat((String) inbox.get(0).get("body")).contains("100%").contains("2026-11");
        assertThat(jdbc.sql("SELECT dedupe_key FROM notifications WHERE user_id = :u").param("u", user.id())
                .query(String.class).single()).isEqualTo("outbox:" + good);
    }

    private Map<String, Object> payload(UUID userId, String category, String templateKey) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("budgetId", UUID.randomUUID().toString());
        variables.put("month", "2026-11");
        variables.put("threshold", 100);
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("userId", userId.toString());
        p.put("category", category);
        p.put("templateKey", templateKey);
        p.put("variables", variables);
        return p;
    }

    private String status(UUID outboxId) {
        return jdbc.sql("SELECT status FROM outbox_events WHERE id = :id").param("id", outboxId).query(String.class)
                .single();
    }
}
