package com.lifeos.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.security.JwtService;
import com.lifeos.support.IntegrationTestBase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * OpenAPI (D63) bật ở profile dev: chạy với ĐÚNG profile dev (không kèm profile test), datasource trỏ vào container
 * test. /v3/api-docs vẫn cần access token (không nằm trong danh sách công khai).
 */
@ActiveProfiles(value = "dev", inheritProfiles = false)
@TestPropertySource(properties = "lifeos.security.jwt.secret=qtM6wn1AKtfsura+Mfd+qELSjTPMeSpp5bsj7B66+uJwIwlulE3EQqRFGhKYAF96")
class ApiDocsDevProfileIT extends IntegrationTestBase {

    @Value("${local.server.port}")
    int port;

    @Autowired
    JwtService jwtService;

    static HttpResponse<String> getApiDocs(int port, String bearer) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs"))
                .GET();
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void apiDocsAreServedInDevProfileWithAccessToken() throws Exception {
        String token = jwtService.issue(UUID.randomUUID(), UUID.randomUUID()).token();

        HttpResponse<String> response = getApiDocs(port, token);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"openapi\"")
                .contains("/api/v1/auth/register")
                .contains("/api/v1/auth/verify-email")
                .contains("/api/v1/auth/resend-verification");
    }

    @Test
    void apiDocsRequireAuthentication() throws Exception {
        HttpResponse<String> response = getApiDocs(port, null);
        assertThat(response.statusCode()).isEqualTo(401);
    }
}
