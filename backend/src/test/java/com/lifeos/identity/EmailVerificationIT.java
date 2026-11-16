package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ID-02 Xác minh email và gửi lại email xác minh (FR-ID-02). SEC-07 (phần token xác minh): một lần, hết hạn,
 * token cũ bị vô hiệu khi cấp mới.
 */
class EmailVerificationIT extends IdentityIntegrationTest {

    private static void assertInvalidToken(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString())
                .contains("\"errorCode\":\"AUTH_VERIFICATION_TOKEN_INVALID\"");
    }

    @Test
    void validTokenVerifiesEmailOnce() throws Exception {
        String email = newEmail();
        register(email);
        UUID userId = userId(email).orElseThrow();
        String token = latestVerificationToken(userId);

        MvcResult first = verifyEmail(token);

        assertThat(first.getResponse().getStatus()).isEqualTo(204);
        assertThat(userRow(userId).get("email_verified_at")).isNotNull();
        assertThat(jdbc.sql("SELECT used_at FROM email_verification_tokens WHERE user_id = :u").param("u", userId)
                .query().singleValue()).isNotNull();
        // Dùng lại cùng token → từ chối (một lần).
        assertInvalidToken(verifyEmail(token));
    }

    @Test
    void unknownTokenIsRejected() throws Exception {
        assertInvalidToken(verifyEmail("khong-ton-tai-" + UUID.randomUUID()));
    }

    @Test
    void expiredTokenIsRejectedAndEmailStaysUnverified() throws Exception {
        String email = newEmail();
        register(email);
        UUID userId = userId(email).orElseThrow();
        String token = latestVerificationToken(userId);

        clock.advance(Duration.ofHours(24));

        assertInvalidToken(verifyEmail(token));
        assertThat(userRow(userId).get("email_verified_at")).isNull();
    }

    @Test
    void resendInvalidatesPreviousTokenAndIssuesANewOne() throws Exception {
        String email = newEmail();
        register(email);
        UUID userId = userId(email).orElseThrow();
        String oldToken = latestVerificationToken(userId);

        MvcResult resend = resend(email.toUpperCase());

        assertThat(resend.getResponse().getStatus()).isEqualTo(202);
        assertThat(countVerificationEvents(userId)).isEqualTo(2);
        String newToken = latestVerificationToken(userId);
        assertThat(newToken).isNotEqualTo(oldToken);
        assertInvalidToken(verifyEmail(oldToken));
        assertThat(verifyEmail(newToken).getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    void resendForUnknownOrAlreadyVerifiedEmailSendsNothing() throws Exception {
        String unknown = newEmail();
        assertThat(resend(unknown).getResponse().getStatus()).isEqualTo(202);
        assertThat(userId(unknown)).isEmpty();

        String verified = newEmail();
        UUID userId = registerVerified(verified);
        int before = countVerificationEvents(userId);
        assertThat(resend(verified).getResponse().getStatus()).isEqualTo(202);
        assertThat(countVerificationEvents(userId)).isEqualTo(before);
    }

    @Test
    void registeringAgainWithUnverifiedEmailResendsVerificationButVerifiedSendsNothing() throws Exception {
        String unverified = newEmail();
        register(unverified);
        UUID unverifiedId = userId(unverified).orElseThrow();
        String firstToken = latestVerificationToken(unverifiedId);

        assertThat(register(unverified).getResponse().getStatus()).isEqualTo(202);
        assertThat(countVerificationEvents(unverifiedId)).isEqualTo(2);
        assertInvalidToken(verifyEmail(firstToken));

        String verified = newEmail();
        UUID verifiedId = registerVerified(verified);
        assertThat(register(verified).getResponse().getStatus()).isEqualTo(202);
        assertThat(countVerificationEvents(verifiedId)).isEqualTo(1);
        // Đăng ký lại không đổi mật khẩu/tên của tài khoản có sẵn và không tạo user_settings thứ hai.
        assertThat(countUsers(verified)).isEqualTo(1);
        assertThat(jdbc.sql("SELECT count(*) FROM user_settings WHERE user_id = :u").param("u", verifiedId)
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void perEmailSendLimitIsSilentAndSharedBetweenResendAndRepeatedRegister() throws Exception {
        String email = newEmail();
        register(email); // email đầu tiên do đăng ký mới: không tính vào hạn mức gửi lại
        UUID userId = userId(email).orElseThrow();

        for (int i = 0; i < 4; i++) {
            assertThat(resend(email).getResponse().getStatus()).isEqualTo(202);
        }
        assertThat(register(email).getResponse().getStatus()).isEqualTo(202); // lần gửi lại thứ 5
        assertThat(countVerificationEvents(userId)).isEqualTo(6);

        MvcResult sixth = resend(email);
        assertThat(sixth.getResponse().getStatus()).isEqualTo(202);
        assertThat(sixth.getResponse().getContentAsString()).isEmpty();
        assertThat(countVerificationEvents(userId)).as("vượt 5/giờ: không gửi thêm").isEqualTo(6);

        clock.advance(Duration.ofHours(1));
        resend(email);
        assertThat(countVerificationEvents(userId)).isEqualTo(7);
    }
}
