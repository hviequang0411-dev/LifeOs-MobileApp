package com.lifeos.common.exception;

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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SEC-12 (SECURITY_RULES §10): lỗi 5xx không lộ stack trace, SQL, tên bảng hay tên lớp; chỉ có thông điệp chung kèm
 * correlationId (khớp header X-Correlation-Id để tra log). Controller ném lỗi CHỈ được nạp vào context của lớp này
 * qua @Import — không lọt vào context dùng chung mà AuthorizationMatrixIT quét route.
 */
@Import(ErrorExposureIT.BoomConfig.class)
class ErrorExposureIT extends IntegrationTestBase {

    private static final String SECRET_SQL = "SELECT password_hash FROM users WHERE email = 'victim@example.test'";

    @TestConfiguration(proxyBeanMethods = false)
    static class BoomConfig {
        @Bean
        BoomController boomController() {
            return new BoomController();
        }
    }

    @RestController
    static class BoomController {

        @GetMapping("/api/v1/test-probe/boom")
        String boom() {
            throw new IllegalStateException("Lỗi nội bộ: " + SECRET_SQL,
                    new RuntimeException("org.postgresql.util.PSQLException at users_pkey"));
        }

        @GetMapping("/api/v1/test-probe/db")
        String db() {
            throw new DataIntegrityViolationException(
                    "could not execute statement [" + SECRET_SQL + "]; constraint [ux_users_email_lower]");
        }
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JwtService jwtService;

    private HttpResponse<String> get(String path) throws Exception {
        String token = jwtService.issue(UUID.randomUUID(), UUID.randomUUID()).token();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void assertGeneric500(HttpResponse<String> response) {
        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
        String body = response.body();
        assertThat(body).contains("\"errorCode\":\"INTERNAL_ERROR\"")
                .contains("\"detail\":\"Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.\"");
        String correlationId = response.headers().firstValue("X-Correlation-Id").orElseThrow();
        assertThat(body).contains("\"correlationId\":\"" + correlationId + "\"");
        assertThat(body).doesNotContain("SELECT").doesNotContain("password_hash").doesNotContain("users")
                .doesNotContain("Exception").doesNotContain("at com.").doesNotContain("org.postgresql")
                .doesNotContain("ux_users_email_lower").doesNotContain("trace");
    }

    @Test
    void unexpectedExceptionReturnsGenericProblemWithoutInternals() throws Exception {
        assertGeneric500(get("/api/v1/test-probe/boom"));
    }

    @Test
    void unhandledDatabaseErrorDoesNotLeakSqlOrConstraintNames() throws Exception {
        assertGeneric500(get("/api/v1/test-probe/db"));
    }
}
