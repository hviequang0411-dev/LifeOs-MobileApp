package com.lifeos.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.security.JwtService;
import com.lifeos.support.IntegrationTestBase;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** OpenAPI tắt ngoài dev/test (D63): ở profile prod, /v3/api-docs không tồn tại kể cả khi có access token. */
@ActiveProfiles(value = "prod", inheritProfiles = false)
@TestPropertySource(properties = {"lifeos.security.jwt.secret=qtM6wn1AKtfsura+Mfd+qELSjTPMeSpp5bsj7B66+uJwIwlulE3EQqRFGhKYAF96",
        // WP4: prod bắt buộc NOTIFICATION_UNSUBSCRIBE_SECRET (D143), test cấp giá trị riêng như JWT secret.
        "lifeos.notification.unsubscribe-secret=test-only-unsubscribe-hmac-key-9c1f4b7e2a6d8053",
        // Đợt sửa F9: prod bắt buộc SNAPSHOT_HMAC_SECRET, test cấp giá trị riêng.
        "lifeos.sync.snapshot-hmac-secret=test-only-snapshot-hmac-key-4e7a1d9c05b3f862",
        // DWP2: production runtime tắt Flyway; riêng IT bật lại để dựng PostgreSQL cô lập của Testcontainers.
        "spring.flyway.enabled=true",
        "spring.mail.username=test-only", "spring.mail.password=test-only",
        "server.tomcat.remoteip.internal-proxies=127\\.0\\.0\\.1",
        "lifeos.export.dir=${java.io.tmpdir}/lifeos-prod-profile-it"})
class ApiDocsProdProfileIT extends IntegrationTestBase {

    @Value("${local.server.port}")
    int port;

    @Autowired
    JwtService jwtService;

    @Test
    void apiDocsAreNotServedInProdProfile() throws Exception {
        String token = jwtService.issue(UUID.randomUUID(), UUID.randomUUID()).token();

        HttpResponse<String> response = ApiDocsDevProfileIT.getApiDocs(port, token);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).doesNotContain("\"openapi\"");
    }
}
