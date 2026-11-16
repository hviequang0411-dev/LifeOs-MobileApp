package com.lifeos.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.lifeos.support.PostgresTestContainer;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đầu-cuối (SYSTEM_ARCHITECTURE §12: "Email xác minh đi qua outbox, worker gửi được sang mailpit"): app thật với
 * scheduler BẬT (worker ~2 giây), SMTP thật tới container mailpit (Testcontainers, cùng image với docker-compose.yml),
 * HTTP thật tới Tomcat. Đăng ký → email xác minh tới mailpit → dùng token trong liên kết → quên mật khẩu → email đặt
 * lại → đặt mật khẩu mới → email "mật khẩu đã thay đổi". Chờ bằng Awaitility (không sleep).
 * <p>
 * Context riêng, đóng ngay sau lớp (DirtiesContext) để worker nền không chạy tiếp khi các test khác dùng DB chung.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "lifeos.scheduling.enabled=true",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "spring.mail.properties.mail.smtp.starttls.required=false",
        "lifeos.mail.from=LifeOS Test <no-reply@lifeos.test>",
        "lifeos.mail.app-base-url=https://app.lifeos.test"})
class MailpitDeliveryIT {

    private static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:latest")
            .withExposedPorts(1025, 8025)
            .waitingFor(Wait.forHttp("/api/v1/messages").forPort(8025).forStatusCode(200));

    static {
        MAILPIT.start();
    }

    private static final Pattern TOKEN = Pattern.compile("#token=([A-Za-z0-9_-]+)");
    private static final String PASSWORD = "mat-khau-mailpit-123";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        var pg = PostgresTestContainer.INSTANCE;
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
        registry.add("spring.mail.host", MAILPIT::getHost);
        registry.add("spring.mail.port", () -> MAILPIT.getMappedPort(1025));
    }

    @Value("${local.server.port}")
    int port;

    @Autowired
    JsonMapper json;

    @Autowired
    JdbcClient jdbc;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> postApi(String path, Map<String, Object> body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode mailpit(String path) throws Exception {
        URI uri = URI.create("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025) + path);
        return json.readTree(http.send(HttpRequest.newBuilder(uri).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body());
    }

    /** Chờ email có tiêu đề cho trước tới hộp thư {@code to}; trả nội dung văn bản. */
    private String awaitMail(String to, String subject) {
        String[] text = new String[1];
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(250)).until(() -> {
            JsonNode found = mailpit("/api/v1/search?query="
                    + URLEncoder.encode("to:\"" + to + "\"", StandardCharsets.UTF_8));
            for (JsonNode message : found.get("messages")) {
                if (subject.equals(message.get("Subject").asString())) {
                    text[0] = mailpit("/api/v1/message/" + message.get("ID").asString()).get("Text").asString();
                    return true;
                }
            }
            return false;
        });
        return text[0];
    }

    private static String tokenIn(String text, String path) {
        assertThat(text).contains("https://app.lifeos.test" + path + "#token=");
        Matcher matcher = TOKEN.matcher(text);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    /**
     * Cô lập khỏi dữ liệu do các lớp test trước để lại trong DB dùng chung: sự kiện outbox còn PENDING và yêu cầu xuất
     * dữ liệu còn chờ được hoãn để worker nền của context này (scheduler BẬT) chỉ xử lý dữ liệu của test này. Không có
     * bước này, tồn đọng (lấy theo created_at) có thể làm email của test tới muộn hơn 30 giây tùy thứ tự chạy (WP8).
     */
    @Autowired
    org.springframework.scheduling.TaskScheduler scheduler;

    @org.junit.jupiter.api.BeforeEach
    void deferLeftoversOfOtherTests() {
        // Hồi quy WP8: scheduler một luồng để job xuất dữ liệu (dài) chặn outbox worker → email trễ > 30 giây.
        assertThat(((org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler) scheduler).getPoolSize())
                .as("scheduler phải nhiều luồng").isGreaterThanOrEqualTo(2);
        jdbc.sql("UPDATE outbox_events SET next_attempt_at = now() + interval '30 days' WHERE status = 'PENDING'")
                .update();
        jdbc.sql("UPDATE data_exports SET status = 'FAILED', error_code = 'TEST_ISOLATION' WHERE status = 'PENDING'")
                .update();
    }

    @Test
    void verificationResetAndChangedEmailsAreDeliveredToMailpitAndTheirLinksWork() throws Exception {
        String email = "mailpit-" + UUID.randomUUID() + "@example.test";

        assertThat(postApi("/api/v1/auth/register", Map.of("email", email, "password", PASSWORD,
                "displayName", "Mailpit")).statusCode()).isEqualTo(202);
        String verification = tokenIn(awaitMail(email, "Xác minh địa chỉ email LifeOS"), "/verify-email");
        assertThat(postApi("/api/v1/auth/verify-email", Map.of("token", verification)).statusCode()).isEqualTo(204);

        assertThat(postApi("/api/v1/auth/forgot-password", Map.of("email", email)).statusCode()).isEqualTo(202);
        String reset = tokenIn(awaitMail(email, "Đặt lại mật khẩu LifeOS"), "/reset-password");
        assertThat(postApi("/api/v1/auth/reset-password", Map.of("token", reset, "newPassword",
                "mat-khau-moi-mailpit-456")).statusCode()).isEqualTo(204);

        assertThat(awaitMail(email, "Mật khẩu LifeOS của bạn đã được thay đổi")).contains("vừa được thay đổi lúc");

        UUID userId = jdbc.sql("SELECT id FROM users WHERE lower(email) = lower(:e)").param("e", email)
                .query(UUID.class).single();
        await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.sql("""
                SELECT count(*) FROM outbox_events WHERE aggregate_id = :u AND status = 'SENT'
                  AND payload = '{}'::jsonb""").param("u", userId).query(Integer.class).single() == 3);
    }
}
