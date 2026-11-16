package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.common.exception.UnauthorizedException;
import com.lifeos.identity.service.AuthService;
import com.lifeos.identity.service.OpaqueTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ID-04 / UC-ID-05. SEC-03: xoay vòng đúng; dùng lại token cũ thu hồi cả family (và việc thu hồi được COMMIT dù
 * request trả 401); token hết hạn; trần family 90 ngày (D71); thiết bị bị thu hồi. Logout theo D75.
 */
class RefreshTokenIT extends IdentityIntegrationTest {

    @Autowired
    AuthService authService;

    private static void assertInvalidRefresh(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentAsString())
                .contains("\"errorCode\":\"AUTH_REFRESH_TOKEN_INVALID\"");
    }

    private Map<String, Object> tokenRow(String rawRefresh) {
        return jdbc.sql("SELECT * FROM refresh_tokens WHERE token_hash = :h")
                .param("h", OpaqueTokens.sha256Hex(rawRefresh)).query().singleRow();
    }

    private List<Map<String, Object>> family(UUID familyId) {
        return jdbc.sql("SELECT * FROM refresh_tokens WHERE family_id = :f").param("f", familyId).query()
                .listOfRows();
    }

    @Test
    void refreshRotatesWithinTheSameFamily() throws Exception {
        String email = newEmail();
        registerVerified(email);
        Map<String, Object> session = loginOk(email, null);
        String oldRefresh = str(session, "refreshToken");
        clock.advance(Duration.ofMinutes(20));
        Instant now = clock.instant();

        MvcResult result = refresh(oldRefresh);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        Map<String, Object> body = readJson(result);
        assertThat(body).containsOnlyKeys("tokenType", "accessToken", "expiresIn", "refreshToken", "deviceId");
        assertThat(body.get("deviceId")).isEqualTo(session.get("deviceId"));
        String newRefresh = str(body, "refreshToken");
        assertThat(newRefresh).isNotEqualTo(oldRefresh);

        Map<String, Object> old = tokenRow(oldRefresh);
        Map<String, Object> fresh = tokenRow(newRefresh);
        assertThat(RegistrationIT.instant(old.get("used_at"))).isEqualTo(now);
        assertThat(old.get("replaced_by_id")).isEqualTo(fresh.get("id"));
        assertThat(fresh.get("family_id")).isEqualTo(old.get("family_id"));
        assertThat(fresh.get("family_started_at")).isEqualTo(old.get("family_started_at"));
        assertThat(RegistrationIT.instant(fresh.get("expires_at"))).isEqualTo(now.plus(Duration.ofDays(30)));
        assertThat(getDevices(str(body, "accessToken")).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void reusingARotatedTokenRevokesTheWholeFamilyAndTheRevocationIsCommitted() throws Exception {
        String email = newEmail();
        registerVerified(email);
        String first = str(loginOk(email, null), "refreshToken");
        String second = str(readJson(refresh(first)), "refreshToken");
        UUID familyId = (UUID) tokenRow(first).get("family_id");

        assertInvalidRefresh(refresh(first)); // trình lại token đã dùng

        // Đọc bằng kết nối riêng SAU khi request 401 kết thúc: thu hồi đã được commit, không bị rollback cùng lỗi.
        assertThat(family(familyId)).hasSize(2)
                .allSatisfy(row -> assertThat(row.get("revoked_at")).as("revoked_at").isNotNull());
        assertInvalidRefresh(refresh(second));
    }

    @Test
    void familyRevocationOnReuseSurvivesTheExceptionAtServiceLevel() throws Exception {
        String email = newEmail();
        registerVerified(email);
        String first = str(loginOk(email, null), "refreshToken");
        String second = authService.refresh(first, ip).refreshToken();

        assertThatThrownBy(() -> authService.refresh(first, ip)).isInstanceOf(UnauthorizedException.class);

        assertThat(tokenRow(second).get("revoked_at")).isNotNull();
        assertThatThrownBy(() -> authService.refresh(second, ip)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void expiredRefreshTokenIsRejected() throws Exception {
        String email = newEmail();
        registerVerified(email);
        String refreshToken = str(loginOk(email, null), "refreshToken");

        clock.advance(Duration.ofDays(30));

        assertInvalidRefresh(refresh(refreshToken));
        assertThat(tokenRow(refreshToken).get("used_at")).isNull();
    }

    @Test
    void familyHasAnAbsoluteNinetyDayCap() throws Exception {
        String email = newEmail();
        registerVerified(email);
        String token = str(loginOk(email, null), "refreshToken");
        Instant familyStart = RegistrationIT.instant(tokenRow(token).get("family_started_at"));

        for (int i = 0; i < 3; i++) { // ngày 29, 58, 87
            clock.advance(Duration.ofDays(29));
            MvcResult result = refresh(token);
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            token = str(readJson(result), "refreshToken");
        }
        // Lần xoay ở ngày 87: hạn = min(87 + 30, 90) = đúng mốc 90 ngày.
        assertThat(RegistrationIT.instant(tokenRow(token).get("expires_at")))
                .isEqualTo(familyStart.plus(Duration.ofDays(90)));

        clock.advance(Duration.ofDays(3));
        assertInvalidRefresh(refresh(token));
    }

    @Test
    void unknownOrMalformedTokenIsRejected() throws Exception {
        assertInvalidRefresh(refresh("khong-ton-tai-" + UUID.randomUUID()));
        MvcResult blank = refresh("");
        assertThat(blank.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void refreshIsRejectedForDisabledUser() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        String token = str(loginOk(email, null), "refreshToken");
        jdbc.sql("UPDATE users SET status = 'DISABLED' WHERE id = :u").param("u", userId).update();

        assertInvalidRefresh(refresh(token));
    }

    // ---------- Logout (UC-ID-05, D75)

    @Test
    void logoutRevokesTheFamilyButNotTheDevice() throws Exception {
        String email = newEmail();
        registerVerified(email);
        Map<String, Object> session = loginOk(email, null);
        String token = str(session, "refreshToken");

        MvcResult result = logout(token);

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertInvalidRefresh(refresh(token));
        assertThat(deviceRevoked(UUID.fromString(str(session, "deviceId")))).isFalse();
    }

    @Test
    void logoutDoesNotNeedAnAccessTokenAndIgnoresAnExpiredOne() throws Exception {
        String email = newEmail();
        registerVerified(email);
        Map<String, Object> session = loginOk(email, null);
        clock.advance(Duration.ofMinutes(20)); // access token đã hết hạn

        MvcResult result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/logout").with(fromIp(ip))
                        .header("Authorization", "Bearer " + str(session, "accessToken"))
                        .contentType("application/json")
                        .content(body("refreshToken", str(session, "refreshToken"))))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertInvalidRefresh(refresh(str(session, "refreshToken")));
    }

    @Test
    void logoutWithUsedExpiredOrUnknownTokenIs204AndDoesNotTriggerReuseDetection() throws Exception {
        String email = newEmail();
        registerVerified(email);
        String first = str(loginOk(email, null), "refreshToken");
        String second = str(readJson(refresh(first)), "refreshToken");

        assertThat(logout(first).getResponse().getStatus()).isEqualTo(204); // token đã dùng
        assertThat(logout("khong-ton-tai").getResponse().getStatus()).isEqualTo(204);
        assertThat(logout(first).getResponse().getContentAsString()).isEmpty();

        // Không coi là dùng lại: token hiện hành của family vẫn dùng được.
        assertThat(refresh(second).getResponse().getStatus()).isEqualTo(200);
    }
}
