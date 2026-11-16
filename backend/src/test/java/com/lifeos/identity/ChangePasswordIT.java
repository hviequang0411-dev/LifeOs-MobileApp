package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ID-07 Đổi mật khẩu (FR-ID-08, D54, D74; Q15): cần mật khẩu hiện tại; thành công thu hồi refresh token của các
 * thiết bị KHÁC, giữ thiết bị hiện tại; giới hạn 5 lần nhập sai mật khẩu hiện tại / 15 phút theo user.
 */
class ChangePasswordIT extends IdentityIntegrationTest {

    private static final String PATH = "/api/v1/me/change-password";
    private static final String NEW_PASSWORD = "mat-khau-moi-456";

    private MvcResult change(String accessToken, String current, String next) throws Exception {
        return sendAuthed("POST", PATH, body("currentPassword", current, "newPassword", next), accessToken);
    }

    @Test
    void changeKeepsTheCurrentDeviceAndRevokesTheOthers() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        Map<String, Object> current = loginOk(email, null);
        Map<String, Object> other = loginOk(email, null);

        MvcResult result = change(str(current, "accessToken"), PASSWORD, NEW_PASSWORD);

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(refresh(str(other, "refreshToken")).getResponse().getStatus()).isEqualTo(401);
        assertThat(refresh(str(current, "refreshToken")).getResponse().getStatus()).isEqualTo(200);
        assertThat(getAuthed("/api/v1/me", str(current, "accessToken")).getResponse().getStatus()).isEqualTo(200);
        assertThat(login(email, PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(401);
        assertThat(login(email, NEW_PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(200);
        assertThat(countEvents(userId, "PASSWORD_CHANGED")).isEqualTo(1);
    }

    @Test
    void wrongCurrentPasswordIsRejectedAndNothingChanges() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        Map<String, Object> session = loginOk(email, null);

        MvcResult result = change(str(session, "accessToken"), "mat-khau-sai-000", NEW_PASSWORD);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(result)).containsEntry("errorCode", "AUTH_CURRENT_PASSWORD_WRONG");
        assertThat(login(email, PASSWORD, null, ip).getResponse().getStatus()).isEqualTo(200);
        assertThat(countEvents(userId, "PASSWORD_CHANGED")).isZero();
    }

    @Test
    void weakNewPasswordIsTooWeak() throws Exception {
        String email = newEmail();
        registerVerified(email);
        MvcResult result = change(str(loginOk(email, null), "accessToken"), PASSWORD, "ngan");

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"AUTH_PASSWORD_TOO_WEAK\"")
                .contains("\"field\":\"newPassword\"");
    }

    @Test
    void fiveWrongCurrentPasswordsThrottleTheUserForFifteenMinutes() throws Exception {
        String email = newEmail();
        registerVerified(email);
        String access = str(loginOk(email, null), "accessToken");
        for (int i = 0; i < 5; i++) {
            assertThat(change(access, "mat-khau-sai-000", NEW_PASSWORD).getResponse().getStatus()).isEqualTo(400);
        }

        MvcResult blocked = change(access, PASSWORD, NEW_PASSWORD);
        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        assertThat(blocked.getResponse().getHeader("Retry-After")).isNotNull();

        clock.advance(Duration.ofMinutes(15));
        // Access token cũ đã hết hạn sau 15 phút: đăng nhập lại rồi đổi.
        String fresh = str(loginOk(email, null), "accessToken");
        assertThat(change(fresh, PASSWORD, NEW_PASSWORD).getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    void requiresAuthenticationAndRejectsUnknownFields() throws Exception {
        MvcResult anonymous = postJson(PATH, body("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD), ip);
        assertThat(anonymous.getResponse().getStatus()).isEqualTo(401);

        String email = newEmail();
        registerVerified(email);
        MvcResult unknown = sendAuthed("POST", PATH, body("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD,
                "userId", UUID.randomUUID().toString()), str(loginOk(email, null), "accessToken"));
        assertThat(readJson(unknown)).containsEntry("errorCode", "MALFORMED_REQUEST");
    }
}
