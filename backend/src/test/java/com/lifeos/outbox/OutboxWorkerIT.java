package com.lifeos.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.mail.EmailMessage;
import com.lifeos.common.outbox.OutboxProperties;
import com.lifeos.common.outbox.OutboxStore;
import com.lifeos.identity.service.IdentityOutboxEvents;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Outbox worker + handler email của identity (SYSTEM_ARCHITECTURE §3.11, D23, D37; SECURITY_RULES §3.4, §9.5–9.6;
 * TESTING_RULES LOG-02). Email qua stub ghi lại lời gọi; thời gian điều khiển bằng MutableClock (không sleep).
 */
class OutboxWorkerIT extends OutboxTestSupport {

    @Autowired
    OutboxProperties properties;

    @Autowired
    OutboxStore store;

    @Autowired
    MeterRegistry meters;

    private static EmailMessage only(List<EmailMessage> messages) {
        assertThat(messages).hasSize(1);
        return messages.get(0);
    }

    private Map<String, Object> payload(String email, String token, Instant expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", UUID.randomUUID().toString());
        payload.put("email", email);
        payload.put("displayName", "Người nhận");
        payload.put("token", token);
        payload.put("expiresAt", expiresAt.toString());
        return payload;
    }

    @Test
    void verificationEmailIsSentFromAppBaseUrlNotTheHostHeaderAndThePayloadIsScrubbed() throws Exception {
        String email = newEmail();
        MvcResult registered = mvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/register").with(fromIp(ip))
                .header("Host", "evil.example").header("X-Forwarded-Host", "evil.example")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("email", email, "password", PASSWORD, "displayName", "An"))).andReturn();
        assertThat(registered.getResponse().getStatus()).isEqualTo(202);
        UUID userId = userId(email).orElseThrow();
        UUID event = eventOf(userId, IdentityOutboxEvents.EMAIL_VERIFICATION);
        String token = latestVerificationToken(userId);
        assertThat(row(event)).containsEntry("status", "PENDING");

        drain();

        EmailMessage sent = only(mail.sentTo(email));
        assertThat(sent.idempotencyKey()).isEqualTo(event);
        assertThat(sent.subject()).isEqualTo("Xác minh địa chỉ email LifeOS").doesNotContain(token);
        assertThat(sent.text()).contains("Chào An,")
                .contains("https://app.lifeos.test/verify-email#token=" + token).doesNotContain("evil.example");
        Map<String, Object> done = row(event);
        assertThat(done).containsEntry("status", "SENT").containsEntry("payload", "{}").containsEntry("attempts", 0)
                .containsEntry("locked_until", null).containsEntry("last_error", null);
        assertThat(done.get("processed_at")).isNotNull();
        assertThat(verifyEmail(token).getResponse().getStatus()).as("liên kết trong email dùng được").isEqualTo(204);

        drain();
        assertThat(mail.sentTo(email)).as("không gửi lại sự kiện đã SENT").hasSize(1);
    }

    @Test
    void passwordResetAndPasswordChangedEmailsAreSent() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        drain();
        assertThat(forgot(email).getResponse().getStatus()).isEqualTo(202);
        String resetToken = activeResetToken(userId);

        drain();

        EmailMessage reset = mail.sentTo(email).stream()
                .filter(m -> m.subject().equals("Đặt lại mật khẩu LifeOS")).findFirst().orElseThrow();
        assertThat(reset.text()).contains("https://app.lifeos.test/reset-password#token=" + resetToken)
                .contains("1 giờ");
        assertThat(row(eventOf(userId, IdentityOutboxEvents.PASSWORD_RESET))).containsEntry("payload", "{}");

        assertThat(resetPassword(resetToken, "mat-khau-moi-456").getResponse().getStatus()).isEqualTo(204);
        drain();
        EmailMessage changed = mail.sentTo(email).stream()
                .filter(m -> m.subject().equals("Mật khẩu LifeOS của bạn đã được thay đổi")).findFirst().orElseThrow();
        assertThat(changed.text()).contains("vừa được thay đổi lúc").doesNotContain("token=");
    }

    @Test
    void transientFailuresAreRetriedWithExponentialBackoffAndKeepThePayloadUntilSent() {
        String email = newEmail();
        UUID event = write(IdentityOutboxEvents.EMAIL_VERIFICATION,
                payload(email, "tok-" + UUID.randomUUID(), clock.instant().plus(Duration.ofHours(24))));
        mail.failNext(email, 2);

        drain();
        Map<String, Object> first = row(event);
        assertThat(first).containsEntry("status", "PENDING").containsEntry("attempts", 1)
                .containsEntry("last_error", "SimulatedSmtpFailure");
        assertThat(((java.sql.Timestamp) first.get("next_attempt_at")).toInstant())
                .isEqualTo(clock.instant().plus(properties.backoffBase()));
        assertThat((String) first.get("payload")).as("chưa gửi thì còn payload").contains("token");

        drain();
        assertThat(row(event)).as("chưa đến hạn thì không thử lại").containsEntry("attempts", 1);

        clock.advance(properties.backoffBase());
        drain();
        Map<String, Object> second = row(event);
        assertThat(second).containsEntry("status", "PENDING").containsEntry("attempts", 2);
        assertThat(((java.sql.Timestamp) second.get("next_attempt_at")).toInstant())
                .as("backoff nhân đôi").isEqualTo(clock.instant().plus(properties.backoffBase().multipliedBy(2)));

        clock.advance(properties.backoffBase().multipliedBy(2));
        drain();
        assertThat(row(event)).containsEntry("status", "SENT").containsEntry("attempts", 2)
                .containsEntry("payload", "{}").containsEntry("last_error", null);
        assertThat(mail.sentTo(email)).hasSize(1);
    }

    @Test
    void deliveryThatKeepsFailingIsFailedAfterMaxAttemptsAndScrubbed() {
        String email = newEmail();
        UUID event = write(IdentityOutboxEvents.PASSWORD_CHANGED, Map.of("userId", UUID.randomUUID().toString(),
                "email", email, "displayName", "An", "changedAt", clock.instant().toString()));
        mail.failNext(email, 1_000);

        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            drain();
            assertThat(row(event)).containsEntry("attempts", attempt);
            clock.advance(properties.backoffMax());
        }

        Map<String, Object> failed = row(event);
        assertThat(failed).containsEntry("status", "FAILED").containsEntry("payload", "{}")
                .containsEntry("last_error", "SimulatedSmtpFailure");
        assertThat(failed.get("processed_at")).isNotNull();
        assertThat(mail.sentTo(email)).isEmpty();
        drain();
        assertThat(row(event)).as("FAILED không bị xử lý lại").containsEntry("attempts", properties.maxAttempts());
    }

    @Test
    void permanentFailuresAreFailedImmediatelyWithoutSending() {
        String expiredEmail = newEmail();
        UUID expired = write(IdentityOutboxEvents.PASSWORD_RESET,
                payload(expiredEmail, "tok-" + UUID.randomUUID(), clock.instant().minusSeconds(1)));
        UUID unknown = write("SOMETHING_ELSE", Map.of("email", newEmail()));
        UUID invalid = write(IdentityOutboxEvents.EMAIL_VERIFICATION, Map.of("displayName", "Thiếu email"));

        drain();

        assertThat(row(expired)).containsEntry("status", "FAILED").containsEntry("last_error", "TOKEN_EXPIRED")
                .containsEntry("payload", "{}").containsEntry("attempts", 1);
        assertThat(row(unknown)).containsEntry("status", "FAILED").containsEntry("last_error", "NO_HANDLER");
        assertThat(row(invalid)).containsEntry("status", "FAILED").containsEntry("last_error", "INVALID_PAYLOAD");
        assertThat(mail.sentTo(expiredEmail)).isEmpty();
    }

    @Test
    void anExpiredLeaseIsReclaimedCountsAsAnAttemptAndTheOldOwnerCannotOverwrite() {
        String email = newEmail();
        UUID event = write(IdentityOutboxEvents.EMAIL_VERIFICATION,
                payload(email, "tok-" + UUID.randomUUID(), clock.instant().plus(Duration.ofHours(24))));
        drainOthersExcept(event);
        // Một worker "chết" sau khi nhận việc: sự kiện kẹt ở PROCESSING.
        Instant now = clock.instant();
        List<OutboxStore.Claimed> crashed = store.claim(now, now.plus(properties.lease()), 1_000).stream()
                .filter(c -> c.event().id().equals(event)).toList();
        assertThat(crashed).hasSize(1);
        assertThat(worker.runOnce()).as("còn trong lease: không ai nhận").isZero();
        assertThat(row(event)).containsEntry("status", "PROCESSING");

        clock.advance(properties.lease().plusSeconds(1));
        drain();

        assertThat(row(event)).containsEntry("status", "SENT").containsEntry("attempts", 1);
        assertThat(mail.sentTo(email)).hasSize(1);
        assertThat(store.markSent(crashed.get(0), clock.instant())).as("worker cũ mất lease").isFalse();
        assertThat(store.markRetry(crashed.get(0), clock.instant(), "X")).isFalse();
    }

    /** Xử lý hết sự kiện tồn đọng của test khác để lần nhận kế tiếp chỉ còn sự kiện của test này. */
    private void drainOthersExcept(UUID keep) {
        jdbc.sql("UPDATE outbox_events SET next_attempt_at = next_attempt_at + interval '1 day'"
                + " WHERE id = :id").param("id", keep).update();
        drain();
        jdbc.sql("UPDATE outbox_events SET next_attempt_at = next_attempt_at - interval '1 day'"
                + " WHERE id = :id").param("id", keep).update();
    }

    @Test
    void controlCharactersInTheDisplayNameCannotInjectHeadersOrLines() {
        String email = newEmail();
        Map<String, Object> payload = payload(email, "tok-" + UUID.randomUUID(),
                clock.instant().plus(Duration.ofHours(1)));
        payload.put("displayName", "An\r\nBcc: attacker@evil.example\r\n");
        write(IdentityOutboxEvents.PASSWORD_RESET, payload);

        drain();

        EmailMessage sent = only(mail.sentTo(email));
        assertThat(sent.subject()).isEqualTo("Đặt lại mật khẩu LifeOS");
        assertThat(sent.text()).startsWith("Chào An  Bcc: attacker@evil.example,").doesNotContain("\r");
    }

    @Test
    void metricsCarryOnlyTypeAndOutcomeTags() {
        write(IdentityOutboxEvents.PASSWORD_CHANGED, Map.of("userId", UUID.randomUUID().toString(),
                "email", newEmail(), "displayName", "An", "changedAt", clock.instant().toString()));
        drain();

        List<Meter> outboxMeters = meters.getMeters().stream()
                .filter(m -> m.getId().getName().startsWith("lifeos.outbox")).toList();
        assertThat(outboxMeters).extracting(m -> m.getId().getName())
                .contains("lifeos.outbox.events", "lifeos.outbox.pending");
        assertThat(outboxMeters).allSatisfy(m -> m.getId().getTags().forEach(tag -> {
            assertThat(tag.getKey()).isIn("type", "outcome");
            assertThat(tag.getValue()).doesNotContain("@").doesNotContain("tok-");
        }));
    }
}
