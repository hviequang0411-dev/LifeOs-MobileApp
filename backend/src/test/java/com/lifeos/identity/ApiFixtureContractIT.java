package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.identity.dto.LoginRequest;
import com.lifeos.identity.dto.LoginResponse;
import com.lifeos.identity.dto.RefreshTokenRequest;
import com.lifeos.identity.dto.TokenResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hợp đồng backend ↔ Flutter bằng fixture JSON dùng chung (D68, TESTING_RULES §10) — phạm vi Giai đoạn 2: login và
 * refresh. So sánh JSON CÓ CẤU TRÚC (cây JsonNode), không so chuỗi.
 * <ul>
 *   <li>JsonMapper của context (cùng cấu hình StrictJson với ứng dụng) đọc fixture ra DTO và ghi lại được đúng fixture.</li>
 *   <li>Endpoint thật nhận đúng request fixture (chỉ thay email/mật khẩu/refreshToken bằng giá trị của user test) và
 *       trả phản hồi cùng hình dạng với response fixture: cùng tập trường, cùng kiểu JSON từng trường.</li>
 * </ul>
 */
class ApiFixtureContractIT extends IdentityIntegrationTest {

    /** Thư mục làm việc của surefire/failsafe là backend/. */
    private static final Path FIXTURES = Path.of("..", "docs", "api", "fixtures");

    private JsonNode fixture(String name) throws Exception {
        return json.readTree(Files.readString(FIXTURES.resolve(name)));
    }

    /** Hình dạng: object → {tên trường → hình dạng}, giá trị → kiểu JSON (STRING, NUMBER, ...). */
    private static Object shape(JsonNode node) {
        if (node.isObject()) {
            Map<String, Object> fields = new TreeMap<>();
            node.properties().forEach(e -> fields.put(e.getKey(), shape(e.getValue())));
            return fields;
        }
        return node.getNodeType().name();
    }

    /** Ghi ra chuỗi JSON thật rồi parse lại (valueToTree giữ kiểu Java long ≠ int — khác biệt không có trong JSON). */
    private JsonNode roundTrip(JsonNode fixture, Class<?> type) {
        Object dto = json.treeToValue(fixture, type);
        return json.readTree(json.writeValueAsString(dto));
    }

    @Test
    void loginAndRefreshFixturesDeserializeAndSerializeBackIdentically() throws Exception {
        for (Map.Entry<String, Class<?>> entry : Map.<String, Class<?>>of(
                "auth-login.request.json", LoginRequest.class,
                "auth-login.response.json", LoginResponse.class,
                "auth-refresh.request.json", RefreshTokenRequest.class,
                "auth-refresh.response.json", TokenResponse.class).entrySet()) {
            JsonNode fixture = fixture(entry.getKey());
            assertThat(roundTrip(fixture, entry.getValue())).as(entry.getKey()).isEqualTo(fixture);
        }
    }

    @Test
    void realLoginAndRefreshAcceptTheRequestFixturesAndAnswerInTheFixtureShape() throws Exception {
        String email = newEmail();
        registerVerified(email);

        ObjectNode loginRequest = (ObjectNode) fixture("auth-login.request.json");
        loginRequest.put("email", email);
        loginRequest.put("password", PASSWORD);
        MvcResult login = postJson("/api/v1/auth/login", json.writeValueAsString(loginRequest), ip);
        assertThat(login.getResponse().getStatus()).as(login.getResponse().getContentAsString()).isEqualTo(200);
        JsonNode loginResponse = json.readTree(login.getResponse().getContentAsString());
        assertThat(shape(loginResponse)).isEqualTo(shape(fixture("auth-login.response.json")));

        ObjectNode refreshRequest = (ObjectNode) fixture("auth-refresh.request.json");
        refreshRequest.put("refreshToken", loginResponse.get("refreshToken").asString());
        MvcResult refresh = postJson("/api/v1/auth/refresh", json.writeValueAsString(refreshRequest), ip);
        assertThat(refresh.getResponse().getStatus()).as(refresh.getResponse().getContentAsString()).isEqualTo(200);
        assertThat(shape(json.readTree(refresh.getResponse().getContentAsString())))
                .isEqualTo(shape(fixture("auth-refresh.response.json")));
    }
}
