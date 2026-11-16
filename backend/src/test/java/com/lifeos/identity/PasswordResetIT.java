package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.identity.service.OpaqueTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ID-06 Quên và đặt lại mật khẩu (FR-ID-06, D54, D76). SEC-07 phần token đặt lại: một lần, hạn 1 giờ, token cũ bị
 * vô hiệu khi cấp mới. SEC-06/ENUM-01 phần forgot-password.
 */
class PasswordResetIT extends IdentityIntegrationTest {

    private static final String NEW_PASSWORD = "mat-khau-moi-456";

    private static void assertInvalidResetToken(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"AUTH_RESET_TOKEN_INVALID\"");
    }

    @Test
    void forgotPasswordWritesAResetEmailWithAOneHourTokenStoredOnlyAsHash() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        clock.advance(Duration.ofSeconds(1));
        Instant now = clock.instant();

        MvcResult result = forgot(email.toUpperCase());

        assertThat(result.getResponse().getStatus()).isEqualTo(202);
        assertThat(result.getResponse().getContentAsString()).isEmpty();
        assertThat(countEvents(userId, "PASSWORD_RESET")).isEqualTo(1);
        String raw = activeResetToken(userId);
        Map<String, Object> row = jdbc.sql("SELECT * FROM password_reset_tokens WHERE user_id = :u")
                .param("u", userId).query().singleRow();
        assertThat(row.get("token_hash")).isEqualTo(OpaqueTokens.sha256Hex(raw));
        assertThat(RegistrationIT.instant(row.get("expires_at"))).isEqualTo(now.plus(Duration.ofHours(1)));
    }

    @Test
    void resetChangesPasswordRevokesEveryRefreshTokenAndSendsPasswordChangedEmail() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        Map<String, Object> phone = loginOk(email, null);
        Map<String, Object> laptop = loginOk(email, null);
        forgot(email);

        MvcResult result = resetPassword(activeResetToken(userId), NEW_PASSWORD);

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(login(email, PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(401);
        assertThat(login(email, NEW_PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(200);
        // Mọi refresh token cũ (mọi thiết bị) bị thu hồi.
        assertThat(refresh(str(phone, "refreshToken")).getResponse().getStatus()).isEqualTo(401);
        assertThat(refresh(str(laptop, "refreshToken")).getResponse().getStatus()).isEqualTo(401);
        // Email "mật khẩu đã thay đổi" (D54) không chứa token.
        assertThat(countEvents(userId, "PASSWORD_CHANGED")).isEqualTo(1);
        String payload = jdbc.sql("SELECT payload::text FROM outbox_events WHERE aggregate_id = :u"
                        + " AND event_type = 'PASSWORD_CHANGED'").param("u", userId).query(String.class).single();
        assertThat(payload).contains(email).doesNotContain("\"token\"").doesNotContain(NEW_PASSWORD);
    }

    @Test
    void resetVerifiesAnUnverifiedEmail() throws Exception {
        String email = newEmail();
        register(email);
        UUID userId = userId(email).orElseThrow();

        assertThat(forgot(email).getResponse().getStatus()).isEqualTo(202);
        assertThat(countEvents(userId, "PASSWORD_RESET")).as("D76: vẫn gửi cho tài khoản chưa xác minh").isEqualTo(1);
        assertThat(resetPassword(activeResetToken(userId), NEW_PASSWORD).getResponse().getStatus()).isEqualTo(204);

        assertThat(userRow(userId).get("email_verified_at")).isNotNull();
        assertThat(login(email, NEW_PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void resetTokenIsSingleUseAndExpiresAfterOneHour() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        forgot(email);
        String token = activeResetToken(userId);
        assertThat(resetPassword(token, NEW_PASSWORD).getResponse().getStatus()).isEqualTo(204);
        assertInvalidResetToken(resetPassword(token, "mat-khau-khac-789"));

        forgot(email);
        String second = activeResetToken(userId);
        clock.advance(Duration.ofHours(1));
        assertInvalidResetToken(resetPassword(second, "mat-khau-khac-789"));
        assertThat(login(email, NEW_PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void requestingANewResetInvalidatesTheOldToken() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        forgot(email);
        String first = activeResetToken(userId);
        forgot(email);
        String second = activeResetToken(userId);

        assertThat(second).isNotEqualTo(first);
        assertInvalidResetToken(resetPassword(first, NEW_PASSWORD));
        assertThat(resetPassword(second, NEW_PASSWORD).getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    void weakNewPasswordIsRejectedWithoutConsumingTheToken() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        forgot(email);
        String token = activeResetToken(userId);

        MvcResult weak = resetPassword(token, "ngan");

        assertThat(weak.getResponse().getStatus()).isEqualTo(400);
        assertThat(weak.getResponse().getContentAsString()).contains("\"errorCode\":\"AUTH_PASSWORD_TOO_WEAK\"")
                .contains("\"field\":\"newPassword\"");
        assertThat(resetPassword(token, NEW_PASSWORD).getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    void unknownTokenAndUnknownFieldsAreRejected() throws Exception {
        assertInvalidResetToken(resetPassword("khong-ton-tai-" + UUID.randomUUID(), NEW_PASSWORD));
        MvcResult unknownField = postJson("/api/v1/auth/reset-password",
                body("token", "x", "newPassword", NEW_PASSWORD, "userId", UUID.randomUUID().toString()), ip);
        assertThat(unknownField.getResponse().getContentAsString()).contains("\"errorCode\":\"MALFORMED_REQUEST\"");
        MvcResult forgotUnknownField = postJson("/api/v1/auth/forgot-password",
                body("email", newEmail(), "redirect", "http://evil.test"), ip);
        assertThat(forgotUnknownField.getResponse().getContentAsString())
                .contains("\"errorCode\":\"MALFORMED_REQUEST\"");
    }

    @Test
    void forgotRespondsIdenticallyWhetherEmailExistsOrNot() throws Exception {
        String unverified = newEmail();
        register(unverified);
        String verified = newEmail();
        registerVerified(verified);

        List<MvcResult> results = List.of(forgot(newEmail()), forgot(unverified), forgot(verified));

        for (MvcResult r : results) {
            assertThat(r.getResponse().getStatus()).isEqualTo(202);
            assertThat(r.getResponse().getContentAsString()).isEmpty();
            assertThat(r.getResponse().getContentType()).isEqualTo(results.get(0).getResponse().getContentType());
        }
    }

    @Test
    void perEmailSendLimitIsSharedWithResendVerificationAndIsSilent() throws Exception {
        String email = newEmail();
        register(email); // chưa xác minh: cả resend lẫn forgot đều gửi email
        UUID userId = userId(email).orElseThrow();
        for (int i = 0; i < 3; i++) {
            resend(email);
        }
        forgot(email);
        forgot(email); // lần thứ 5 trong bucket chung theo email
        assertThat(countEvents(userId, "PASSWORD_RESET")).isEqualTo(2);

        MvcResult sixth = forgot(email);
        assertThat(sixth.getResponse().getStatus()).isEqualTo(202);
        assertThat(countEvents(userId, "PASSWORD_RESET")).as("vượt 5/giờ: không gửi").isEqualTo(2);
    }

    @Test
    void forgotSharesTheIpBucketWithRegisterAndResetSharesItWithVerify() throws Exception {
        for (int i = 0; i < 10; i++) {
            register(newEmail());
        }
        for (int i = 0; i < 10; i++) {
            forgot(newEmail());
        }
        assertThat(forgot(newEmail()).getResponse().getStatus()).isEqualTo(429);

        for (int i = 0; i < 10; i++) {
            verifyEmail("sai-" + i);
        }
        for (int i = 0; i < 10; i++) {
            resetPassword("sai-" + i, NEW_PASSWORD);
        }
        MvcResult limited = resetPassword("sai-21", NEW_PASSWORD);
        assertThat(limited.getResponse().getStatus()).isEqualTo(429);
        assertThat(limited.getResponse().getHeader("Retry-After")).isNotNull();
    }
}
