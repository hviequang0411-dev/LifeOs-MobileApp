package com.lifeos.identity;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.lifeos.support.IntegrationTestBase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hỗ trợ chung cho test tích hợp module identity: gọi endpoint auth qua MockMvc (IP riêng mỗi test) và đọc trạng
 * thái cuối từ DB. Token thô của email chỉ đọc được từ payload outbox (D37), đúng như worker sẽ đọc ở Giai đoạn 4.
 */
public abstract class IdentityIntegrationTest extends IntegrationTestBase {

    protected static final String PASSWORD = "mat-khau-test-123";

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    protected JsonMapper json;

    /** IP riêng của test hiện tại (mỗi instance test một IP). */
    protected final String ip = randomIp();

    protected static String newEmail() {
        return "u-" + UUID.randomUUID() + "@example.test";
    }

    protected String body(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return json.writeValueAsString(map);
    }

    protected MvcResult postJson(String path, String jsonBody, String fromIp) throws Exception {
        return mvc.perform(post(path).with(fromIp(fromIp)).contentType(MediaType.APPLICATION_JSON).content(jsonBody))
                .andReturn();
    }

    protected MvcResult register(String email, String password, String timezone) throws Exception {
        return postJson("/api/v1/auth/register",
                body("email", email, "password", password, "displayName", "Người Test", "timezone", timezone), ip);
    }

    protected MvcResult register(String email) throws Exception {
        return register(email, PASSWORD, null);
    }

    protected MvcResult verifyEmail(String token) throws Exception {
        return postJson("/api/v1/auth/verify-email", body("token", token), ip);
    }

    protected MvcResult resend(String email) throws Exception {
        return postJson("/api/v1/auth/resend-verification", body("email", email), ip);
    }

    protected Optional<UUID> userId(String email) {
        return jdbc.sql("SELECT id FROM users WHERE lower(email) = lower(:e)").param("e", email.trim())
                .query(UUID.class).optional();
    }

    protected Map<String, Object> userRow(UUID userId) {
        return jdbc.sql("SELECT * FROM users WHERE id = :id").param("id", userId).query().singleRow();
    }

    protected int countUsers(String email) {
        return jdbc.sql("SELECT count(*) FROM users WHERE lower(email) = lower(:e)").param("e", email.trim())
                .query(Integer.class).single();
    }

    /** Token thô trong các sự kiện EMAIL_VERIFICATION của user (không theo thứ tự). */
    protected List<String> verificationTokensFromOutbox(UUID userId) {
        return jdbc.sql("SELECT payload ->> 'token' FROM outbox_events WHERE event_type = 'EMAIL_VERIFICATION'"
                        + " AND aggregate_id = :u")
                .param("u", userId).query(String.class).list();
    }

    /**
     * Token thô của token xác minh CÒN HIỆU LỰC duy nhất của user: khớp SHA-256 của token trong outbox với
     * token_hash chưa dùng trong DB (không dựa vào thứ tự thời gian, vì clock của test có thể đứng yên).
     */
    protected String latestVerificationToken(UUID userId) {
        return jdbc.sql("""
                        SELECT o.payload ->> 'token'
                        FROM outbox_events o
                        JOIN email_verification_tokens t
                          ON t.token_hash = encode(sha256(convert_to(o.payload ->> 'token', 'UTF8')), 'hex')
                        WHERE o.event_type = 'EMAIL_VERIFICATION' AND o.aggregate_id = :u AND t.used_at IS NULL
                        """)
                .param("u", userId).query(String.class).single();
    }

    protected int countVerificationEvents(UUID userId) {
        return verificationTokensFromOutbox(userId).size();
    }

    // ---------- Đăng nhập / token / thiết bị (bước 1c)

    protected MvcResult login(String email, String password, UUID deviceId, String fromIp) throws Exception {
        Map<String, Object> device = new LinkedHashMap<>();
        device.put("name", "Pixel Test");
        device.put("platform", "ANDROID");
        device.put("appVersion", "1.0.0");
        return postJson("/api/v1/auth/login", body("email", email, "password", password,
                "deviceId", deviceId == null ? null : deviceId.toString(), "device", device), fromIp);
    }

    /** Đăng nhập thành công; trả phản hồi JSON đã parse. */
    protected Map<String, Object> loginOk(String email, UUID deviceId) throws Exception {
        MvcResult result = login(email, PASSWORD, deviceId, ip);
        if (result.getResponse().getStatus() != 200) {
            throw new AssertionError("login thất bại: " + result.getResponse().getStatus() + " "
                    + result.getResponse().getContentAsString());
        }
        return readJson(result);
    }

    protected MvcResult refresh(String refreshToken) throws Exception {
        return postJson("/api/v1/auth/refresh", body("refreshToken", refreshToken), ip);
    }

    protected MvcResult logout(String refreshToken) throws Exception {
        return postJson("/api/v1/auth/logout", body("refreshToken", refreshToken), ip);
    }

    protected MvcResult getDevices(String accessToken) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/devices").with(fromIp(ip)).header("Authorization", "Bearer " + accessToken))
                .andReturn();
    }

    protected MvcResult deleteDevice(UUID deviceId, String accessToken) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/devices/" + deviceId).with(fromIp(ip))
                .header("Authorization", "Bearer " + accessToken))
                .andReturn();
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> readJson(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsString(), Map.class);
    }

    /**
     * Thân lỗi RFC 9457 bỏ hai trường phụ thuộc request để so sánh "giống hệt": correlationId (sinh mới mỗi request) và
     * instance (Spring điền đúng đường dẫn client đã gọi, nên chỉ lặp lại id client gửi, không lộ thông tin).
     */
    protected Map<String, Object> problemWithoutCorrelationId(MvcResult result) throws Exception {
        Map<String, Object> problem = new LinkedHashMap<>(readJson(result));
        problem.remove("correlationId");
        problem.remove("instance");
        return problem;
    }

    protected boolean deviceRevoked(UUID deviceId) {
        return jdbc.sql("SELECT revoked_at IS NOT NULL FROM devices WHERE id = :d").param("d", deviceId)
                .query(Boolean.class).single();
    }

    protected static String str(Map<String, Object> map, String key) {
        return (String) map.get(key);
    }

    // ---------- Mật khẩu / hồ sơ / cài đặt (bước 1d)

    protected MvcResult forgot(String email) throws Exception {
        return postJson("/api/v1/auth/forgot-password", body("email", email), ip);
    }

    protected MvcResult resetPassword(String token, String newPassword) throws Exception {
        return postJson("/api/v1/auth/reset-password", body("token", token, "newPassword", newPassword), ip);
    }

    protected int countEvents(UUID userId, String eventType) {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE aggregate_id = :u AND event_type = :t")
                .param("u", userId).param("t", eventType).query(Integer.class).single();
    }

    /** Token thô của token đặt lại mật khẩu CÒN HIỆU LỰC duy nhất (khớp hash outbox với DB, không dựa vào thứ tự). */
    protected String activeResetToken(UUID userId) {
        return jdbc.sql("""
                        SELECT o.payload ->> 'token'
                        FROM outbox_events o
                        JOIN password_reset_tokens t
                          ON t.token_hash = encode(sha256(convert_to(o.payload ->> 'token', 'UTF8')), 'hex')
                        WHERE o.event_type = 'PASSWORD_RESET' AND o.aggregate_id = :u AND t.used_at IS NULL
                        """)
                .param("u", userId).query(String.class).single();
    }

    protected MvcResult authed(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
            String accessToken) throws Exception {
        return mvc.perform(request.with(fromIp(ip)).header("Authorization", "Bearer " + accessToken)).andReturn();
    }

    protected MvcResult getAuthed(String path, String accessToken) throws Exception {
        return authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path), accessToken);
    }

    protected MvcResult sendAuthed(String method, String path, String jsonBody, String accessToken)
            throws Exception {
        return authed(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .request(org.springframework.http.HttpMethod.valueOf(method), path)
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody), accessToken);
    }

    /** Đăng ký rồi xác minh email; trả userId. */
    protected UUID registerVerified(String email) throws Exception {
        register(email);
        UUID userId = userId(email).orElseThrow();
        verifyEmail(latestVerificationToken(userId));
        return userId;
    }
}
