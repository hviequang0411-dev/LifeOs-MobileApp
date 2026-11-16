package com.lifeos;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.IntegrationTestBase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * Context khởi động với PostgreSQL thật + Flyway + Hibernate ddl-auto=validate, và khung bảo mật hoạt động:
 * health mở, mọi endpoint khác mặc định cần xác thực, endpoint công khai không bị chặn bởi bảo mật.
 */
class ApplicationStartupIT extends IntegrationTestBase {

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> send(String method, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthIsUpWithoutAuthentication() throws Exception {
        HttpResponse<String> response = send("GET", "/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void protectedEndpointsRequireAuthenticationAndReturnProblemJson() throws Exception {
        HttpResponse<String> response = send("GET", "/api/v1/tasks");
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
        assertThat(response.headers().firstValue("X-Correlation-Id")).isPresent();
        assertThat(response.body()).contains("\"errorCode\":\"AUTH_TOKEN_INVALID\"");
    }

    @Test
    void publicEndpointsAreNotBlockedBySecurity() throws Exception {
        // Chưa có controller nên là 404 từ MVC, quan trọng là KHÔNG phải 401.
        HttpResponse<String> response = send("POST", "/api/v1/auth/login");
        assertThat(response.statusCode()).isNotEqualTo(401);
    }
}
