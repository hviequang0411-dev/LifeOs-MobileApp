package com.lifeos.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.security.RateLimitPolicy;
import com.lifeos.common.security.RateLimiter;
import com.lifeos.support.IntegrationTestBase;
import java.net.http.HttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

/**
 * Đối chứng cho ClientIpIT: khi 127.0.0.1 được khai báo là proxy tin cậy (TRUSTED_PROXIES), IP lấy từ
 * X-Forwarded-For, nên 11 request mang 11 IP khác nhau đều qua. Chứng minh ClientIpIT xanh là nhờ cấu hình, không
 * phải vì RemoteIpValve không hoạt động.
 */
@TestPropertySource(properties = "server.tomcat.remoteip.internal-proxies=127\\.0\\.0\\.1")
class ClientIpTrustedProxyIT extends IntegrationTestBase {

    @Value("${local.server.port}")
    int port;

    @Autowired
    RateLimiter rateLimiter;

    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    @AfterEach
    void resetBuckets() {
        for (int i = 1; i <= 11; i++) {
            rateLimiter.reset(RateLimitPolicy.REGISTER_IP, "198.51.100." + i);
            rateLimiter.reset(RateLimitPolicy.EMAIL_ENDPOINTS_IP, "198.51.100." + i);
        }
        rateLimiter.reset(RateLimitPolicy.REGISTER_IP, ClientIpIT.LOOPBACK);
        rateLimiter.reset(RateLimitPolicy.EMAIL_ENDPOINTS_IP, ClientIpIT.LOOPBACK);
    }

    @Test
    void forwardedForFromTrustedProxyIsUsedAsClientIp() throws Exception {
        for (int i = 1; i <= 11; i++) {
            assertThat(ClientIpIT.registerVia(client, port, "198.51.100." + i).statusCode()).isEqualTo(202);
        }
    }
}
