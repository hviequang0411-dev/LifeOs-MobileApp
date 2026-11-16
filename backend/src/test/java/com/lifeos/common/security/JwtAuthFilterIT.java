package com.lifeos.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.IntegrationTestBase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * JwtAuthFilter trong chuỗi bảo mật thật (HTTP qua Tomcat): token hợp lệ đi qua lớp bảo mật, token sai/hết hạn/thiết
 * bị bị thu hồi nhận 401 đúng mã lỗi. Dùng một đường dẫn KHÔNG có route nào (mặc định vẫn cần xác thực), nên "đi qua
 * bảo mật" được nhận biết bằng 404 NOT_FOUND của MVC thay vì 401, độc lập với các controller thật (trước đây dùng
 * /api/v1/tasks; đổi ở bước 2c khi route Task xuất hiện, đã được duyệt). Ma trận mọi route thật: AuthorizationMatrixIT.
 */
class JwtAuthFilterIT extends IntegrationTestBase {

    private static final String PROTECTED = "/api/v1/__auth_probe__";

    @Value("${local.server.port}")
    int port;

    @Autowired
    JwtService jwtService;

    @Autowired
    JwtProperties jwtProperties;

    @Autowired
    RevokedDeviceRegistry revokedDevices;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path, String authorization) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postPublic(String path, String authorization) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String validToken() {
        return jwtService.issue(UUID.randomUUID(), UUID.randomUUID()).token();
    }

    @Test
    void validTokenPassesSecurity() throws Exception {
        HttpResponse<String> response = get(PROTECTED, "Bearer " + validToken());
        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"errorCode\":\"NOT_FOUND\"");
    }

    @Test
    void missingTokenIs401TokenInvalid() throws Exception {
        HttpResponse<String> response = get(PROTECTED, null);
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"errorCode\":\"AUTH_TOKEN_INVALID\"");
    }

    @Test
    void expiredTokenIs401TokenExpired() throws Exception {
        Clock past = Clock.offset(clock, Duration.ofMinutes(-20)).withZone(ZoneOffset.UTC);
        String expired = new JwtService(jwtProperties, past).issue(UUID.randomUUID(), UUID.randomUUID()).token();

        HttpResponse<String> response = get(PROTECTED, "Bearer " + expired);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
        assertThat(response.body()).contains("\"errorCode\":\"AUTH_TOKEN_EXPIRED\"");
        assertThat(response.body()).doesNotContain(expired);
    }

    @Test
    void tamperedTokenIs401TokenInvalid() throws Exception {
        String token = validToken();
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");
        HttpResponse<String> response = get(PROTECTED, "Bearer " + tampered);
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"errorCode\":\"AUTH_TOKEN_INVALID\"");
    }

    @Test
    void nonBearerSchemeIsIgnoredAndTreatedAsAnonymous() throws Exception {
        HttpResponse<String> response = get(PROTECTED, "Basic dXNlcjpwYXNz");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"errorCode\":\"AUTH_TOKEN_INVALID\"");
    }

    @Test
    void tokenOfRevokedDeviceIsRejectedImmediately() throws Exception {
        UUID device = UUID.randomUUID();
        String token = jwtService.issue(UUID.randomUUID(), device).token();
        assertThat(get(PROTECTED, "Bearer " + token).statusCode()).isEqualTo(404);

        revokedDevices.revoke(device, clock.instant());

        HttpResponse<String> response = get(PROTECTED, "Bearer " + token);
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).contains("\"errorCode\":\"AUTH_TOKEN_INVALID\"");
    }

    @Test
    void invalidTokenDoesNotBlockPublicEndpoints() throws Exception {
        // Client gửi kèm access token đã hết hạn khi gọi /auth/refresh: không được bị 401 bởi lớp bảo mật.
        HttpResponse<String> response = postPublic("/api/v1/auth/refresh", "Bearer not-a-valid-token");
        assertThat(response.statusCode()).isNotEqualTo(401);
    }

    @Test
    void responsesCarryNoStoreAndNosniffHeaders() throws Exception {
        HttpResponse<String> response = get(PROTECTED, "Bearer " + validToken());
        assertThat(response.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
        assertThat(response.headers().firstValue("Cache-Control").orElse("")).contains("no-store");
    }
}
