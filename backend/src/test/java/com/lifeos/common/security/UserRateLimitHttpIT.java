package com.lifeos.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import com.lifeos.support.MutableClock;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * SEC-06 phần API (SECURITY_RULES §6, RateLimitPolicy.API_USER): giới hạn 600 yêu cầu/phút/user kiểm đầu-cuối qua
 * HTTP THẬT tới Tomcat (HttpClient, không MockMvc), tức đi qua đúng chuỗi filter đã đăng ký trong container. Clock của
 * test đứng yên trong lúc gửi nên cửa sổ không trôi giữa chừng — kết quả xác định.
 */
class UserRateLimitHttpIT extends ApiTestBase {

    private static final String PATH = "/api/v1/projects?limit=1";

    @Value("${local.server.port}")
    int port;

    @Autowired
    Clock clock;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(TestUser user) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + PATH))
                .header("Authorization", "Bearer " + user.token()).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void the601stRequestOfAUserWithinAMinuteIs429WithRetryAfterAndOtherUsersAreUnaffected() throws Exception {
        TestUser busy = newUser();
        TestUser other = newUser();

        List<Integer> unexpected = new ArrayList<>();
        // 600 lấy từ SECURITY_RULES §6, không đọc lại từ hằng số: đổi hằng số mà không đổi tài liệu thì test đỏ.
        for (int i = 1; i <= 600; i++) {
            int status = get(busy).statusCode();
            if (status != 200) {
                unexpected.add(i);
            }
        }
        assertThat(unexpected).as("thứ tự các request trong hạn mức bị từ chối").isEmpty();

        HttpResponse<String> limited = get(busy);
        assertThat(limited.statusCode()).isEqualTo(429);
        assertThat(limited.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
        assertThat(limited.headers().firstValue("Retry-After")).hasValue("60");
        assertThat(limited.body()).contains("\"errorCode\":\"RATE_LIMITED\"");

        assertThat(get(other).statusCode()).as("user khác có bucket riêng").isEqualTo(200);

        ((MutableClock) clock).advance(Duration.ofMinutes(1));
        assertThat(get(busy).statusCode()).as("sang cửa sổ mới thì được gọi lại").isEqualTo(200);
    }
}
