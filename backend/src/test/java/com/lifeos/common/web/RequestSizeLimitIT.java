package com.lifeos.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.IntegrationTestBase;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * SEC-10 phần body (SECURITY_RULES §9.3, D55) qua Tomcat thật: body > 1 MB bị từ chối 413 PAYLOAD_TOO_LARGE, cả khi
 * có Content-Length lẫn khi gửi chunked (không khai báo độ dài). Phần payload mutation 64 KB thuộc Giai đoạn 3.
 */
class RequestSizeLimitIT extends IntegrationTestBase {

    private static final int ONE_MB = 1_048_576;

    @Value("${local.server.port}")
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    /** JSON hợp lệ về cú pháp, dài đúng `size` byte. */
    private static byte[] jsonOfSize(int size) {
        String prefix = "{\"email\":\"a@example.test\",\"password\":\"";
        String suffix = "\"}";
        byte[] body = new byte[size];
        Arrays.fill(body, (byte) 'x');
        byte[] p = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] s = suffix.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(p, 0, body, 0, p.length);
        System.arraycopy(s, 0, body, size - s.length, s.length);
        return body;
    }

    private HttpResponse<String> post(HttpRequest.BodyPublisher publisher) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(publisher)
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void declaredBodyOverOneMegabyteIs413() throws Exception {
        HttpResponse<String> response = post(HttpRequest.BodyPublishers.ofByteArray(jsonOfSize(ONE_MB + 1)));

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("application/problem+json");
        assertThat(response.body()).contains("\"errorCode\":\"PAYLOAD_TOO_LARGE\"");
    }

    @Test
    void chunkedBodyOverOneMegabyteIs413() throws Exception {
        byte[] body = jsonOfSize(ONE_MB + 4096);
        // ofInputStream không biết độ dài → HttpClient gửi Transfer-Encoding: chunked (không có Content-Length).
        HttpResponse<String> response = post(HttpRequest.BodyPublishers.ofInputStream(
                () -> new ByteArrayInputStream(body)));

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains("\"errorCode\":\"PAYLOAD_TOO_LARGE\"");
    }

    @Test
    void bodyJustUnderTheLimitReachesTheApplication() throws Exception {
        HttpResponse<String> response = post(HttpRequest.BodyPublishers.ofByteArray(jsonOfSize(ONE_MB - 1)));

        // Đi qua bộ lọc kích thước; bị từ chối ở validation (thiếu device), không phải 413.
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"errorCode\":\"VALIDATION_FAILED\"");
    }
}
