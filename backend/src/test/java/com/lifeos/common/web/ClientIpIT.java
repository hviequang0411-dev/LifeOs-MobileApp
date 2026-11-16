package com.lifeos.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.support.IntegrationTestBase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * SECURITY_RULES §6: IP lấy từ X-Forwarded-For CHỈ khi qua reverse proxy tin cậy. Cấu hình mặc định
 * (TRUSTED_PROXIES rỗng) không tin proxy nào: X-Forwarded-For giả không giúp client né rate limit theo IP.
 * Phải đi qua Tomcat thật (RemoteIpValve), nên dùng HTTP tới 127.0.0.1. Đối chứng ở ClientIpTrustedProxyIT.
 */
class ClientIpIT extends IntegrationTestBase {

    static final String LOOPBACK = "127.0.0.1";

    @Value("${local.server.port}")
    int port;

    @Autowired
    RateLimiter rateLimiter;

    private final HttpClient client = HttpClient.newHttpClient();

    /** Bucket của 127.0.0.1 dùng chung với mọi test HTTP thật: đặt lại trước và sau để không phụ thuộc thứ tự. */
    @BeforeEach
    @AfterEach
    void resetLoopbackBuckets() {
        rateLimiter.reset(RateLimitPolicy.REGISTER_IP, LOOPBACK);
        rateLimiter.reset(RateLimitPolicy.EMAIL_ENDPOINTS_IP, LOOPBACK);
    }

    static HttpResponse<String> registerVia(HttpClient client, int port, String spoofedIp) throws Exception {
        String body = "{\"email\":\"u-" + UUID.randomUUID() + "@example.test\",\"password\":\"mat-khau-test-123\","
                + "\"displayName\":\"Proxy\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + LOOPBACK + ":" + port
                        + "/api/v1/auth/register"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", spoofedIp)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void spoofedForwardedForDoesNotBypassPerIpRateLimit() throws Exception {
        for (int i = 1; i <= 10; i++) {
            assertThat(registerVia(client, port, "203.0.113." + i).statusCode()).isEqualTo(202);
        }
        // Nếu X-Forwarded-For được tin, mỗi request là một IP khác và request thứ 11 vẫn qua.
        HttpResponse<String> eleventh = registerVia(client, port, "203.0.113.11");
        assertThat(eleventh.statusCode()).isEqualTo(429);
        assertThat(eleventh.body()).contains("\"errorCode\":\"RATE_LIMITED\"");
    }
}
