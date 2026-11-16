package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.security.Argon2PasswordHasher;
import com.lifeos.common.security.PasswordHashProperties;
import com.lifeos.identity.service.OpaqueTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ID-03 Đăng nhập (FR-ID-03, FR-ID-09; SECURITY_RULES §3.3, §4; D31, D75). ENUM-02 phần "cùng lỗi" ở đây; phần
 * "có chạy băm giả" ở LoginDummyHashIT.
 */
class LoginIT extends IdentityIntegrationTest {

    @Test
    void successfulLoginReturnsTokensDeviceAndUserAndStoresOnlyTokenHash() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        clock.advance(Duration.ofSeconds(1));
        Instant now = clock.instant();

        MvcResult result = login(email.toUpperCase(), PASSWORD, null, ip);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        Map<String, Object> body = readJson(result);
        assertThat(body).containsOnlyKeys("tokenType", "accessToken", "expiresIn", "refreshToken", "deviceId",
                "user");
        assertThat(body.get("tokenType")).isEqualTo("Bearer");
        assertThat(((Number) body.get("expiresIn")).intValue()).isEqualTo(900);
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) body.get("user");
        assertThat(user).containsEntry("id", userId.toString()).containsEntry("email", email)
                .containsEntry("displayName", "Người Test").hasSize(3);

        UUID deviceId = UUID.fromString(str(body, "deviceId"));
        Map<String, Object> device = jdbc.sql("SELECT * FROM devices WHERE id = :d").param("d", deviceId)
                .query().singleRow();
        assertThat(device.get("user_id")).isEqualTo(userId);
        assertThat(device.get("name")).isEqualTo("Pixel Test");
        assertThat(device.get("platform")).isEqualTo("ANDROID");
        assertThat(device.get("app_version")).isEqualTo("1.0.0");
        assertThat(device.get("revoked_at")).isNull();

        String refresh = str(body, "refreshToken");
        Map<String, Object> token = jdbc.sql("SELECT * FROM refresh_tokens WHERE device_id = :d")
                .param("d", deviceId).query().singleRow();
        assertThat(token.get("token_hash")).isEqualTo(OpaqueTokens.sha256Hex(refresh));
        assertThat(RegistrationIT.instant(token.get("family_started_at"))).isEqualTo(now);
        assertThat(RegistrationIT.instant(token.get("expires_at"))).isEqualTo(now.plus(Duration.ofDays(30)));
        assertThat(token.get("used_at")).isNull();
        assertThat(token.get("revoked_at")).isNull();

        // Access token dùng được cho endpoint cần xác thực.
        assertThat(getDevices(str(body, "accessToken")).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameError() throws Exception {
        String email = newEmail();
        registerVerified(email);

        MvcResult wrongPassword = login(email, "mat-khau-sai-000", null, ip);
        MvcResult unknownEmail = login(newEmail(), "mat-khau-sai-000", null, ip);

        assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
        assertThat(unknownEmail.getResponse().getStatus()).isEqualTo(401);
        assertThat(problemWithoutCorrelationId(wrongPassword)).isEqualTo(problemWithoutCorrelationId(unknownEmail))
                .containsEntry("errorCode", "AUTH_INVALID_CREDENTIALS");
    }

    @Test
    void unverifiedEmailIsReportedOnlyAfterTheCorrectPassword() throws Exception {
        String email = newEmail();
        register(email);

        MvcResult correct = login(email, PASSWORD, null, ip);
        assertThat(correct.getResponse().getStatus()).isEqualTo(401);
        assertThat(readJson(correct)).containsEntry("errorCode", "AUTH_EMAIL_NOT_VERIFIED");

        MvcResult wrong = login(email, "mat-khau-sai-000", null, ip);
        assertThat(readJson(wrong)).containsEntry("errorCode", "AUTH_INVALID_CREDENTIALS");
        assertThat(jdbc.sql("SELECT count(*) FROM devices d JOIN users u ON u.id = d.user_id WHERE u.email = :e")
                .param("e", email).query(Integer.class).single()).isZero();
    }

    @Test
    void disabledAccountCannotLogInAndGetsTheGenericError() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        jdbc.sql("UPDATE users SET status = 'DISABLED' WHERE id = :u").param("u", userId).update();

        MvcResult result = login(email, PASSWORD, null, ip);
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(readJson(result)).containsEntry("errorCode", "AUTH_INVALID_CREDENTIALS");
    }

    @Test
    void invalidRequestShapeIsRejected() throws Exception {
        String email = newEmail();
        MvcResult noDevice = postJson("/api/v1/auth/login", body("email", email, "password", PASSWORD), ip);
        assertThat(noDevice.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(noDevice)).containsEntry("errorCode", "VALIDATION_FAILED");

        MvcResult badPlatform = postJson("/api/v1/auth/login", body("email", email, "password", PASSWORD,
                "device", Map.of("name", "X", "platform", "WINDOWS")), ip);
        assertThat(badPlatform.getResponse().getContentAsString()).contains("\"field\":\"device.platform\"")
                .contains("\"code\":\"PATTERN\"");

        MvcResult unknownNested = postJson("/api/v1/auth/login", body("email", email, "password", PASSWORD,
                "device", Map.of("name", "X", "platform", "IOS", "imei", "123")), ip);
        assertThat(readJson(unknownNested)).containsEntry("errorCode", "MALFORMED_REQUEST");
    }

    @Test
    void reusingOwnDeviceIdUpdatesTheDeviceAndRevokesItsPreviousFamily() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        Map<String, Object> first = loginOk(email, null);
        UUID deviceId = UUID.fromString(str(first, "deviceId"));

        clock.advance(Duration.ofMinutes(5));
        Map<String, Object> second = loginOk(email, deviceId);

        assertThat(second.get("deviceId")).isEqualTo(deviceId.toString());
        assertThat(jdbc.sql("SELECT count(*) FROM devices WHERE user_id = :u").param("u", userId)
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(RegistrationIT.instant(jdbc.sql("SELECT last_seen_at FROM devices WHERE id = :d")
                .param("d", deviceId).query().singleValue())).isEqualTo(clock.instant());
        // D75: mỗi thiết bị một phiên — refresh token của lần đăng nhập trước bị thu hồi.
        assertThat(refresh(str(first, "refreshToken")).getResponse().getStatus()).isEqualTo(401);
        assertThat(refresh(str(second, "refreshToken")).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void foreignOrRevokedDeviceIdGetsANewDevice() throws Exception {
        String otherEmail = newEmail();
        registerVerified(otherEmail);
        UUID foreignDevice = UUID.fromString(str(loginOk(otherEmail, null), "deviceId"));

        String email = newEmail();
        registerVerified(email);
        Map<String, Object> withForeign = loginOk(email, foreignDevice);
        assertThat(withForeign.get("deviceId")).isNotEqualTo(foreignDevice.toString());

        UUID ownDevice = UUID.fromString(str(withForeign, "deviceId"));
        assertThat(deleteDevice(ownDevice, str(withForeign, "accessToken")).getResponse().getStatus())
                .isEqualTo(204);
        Map<String, Object> afterRevoke = loginOk(email, ownDevice);
        assertThat(afterRevoke.get("deviceId")).isNotEqualTo(ownDevice.toString());
        // Thiết bị của user kia không bị đụng tới.
        assertThat(deviceRevoked(foreignDevice)).isFalse();
    }

    @Test
    void outdatedPasswordHashIsUpgradedAfterSuccessfulLogin() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        String weakHash = new Argon2PasswordHasher(new PasswordHashProperties(16, 32, 1, 4096, 1)).hash(PASSWORD);
        jdbc.sql("UPDATE users SET password_hash = :h WHERE id = :u").param("h", weakHash).param("u", userId)
                .update();

        loginOk(email, null);

        String stored = jdbc.sql("SELECT password_hash FROM users WHERE id = :u").param("u", userId)
                .query(String.class).single();
        assertThat(stored).isNotEqualTo(weakHash).contains("m=19456").contains("t=2");
        assertThat(login(email, PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(200);
    }
}
