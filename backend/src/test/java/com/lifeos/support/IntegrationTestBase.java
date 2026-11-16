package com.lifeos.support;

import com.lifeos.common.web.CorrelationIdFilter;
import com.lifeos.common.web.RequestSizeLimitFilter;
import jakarta.servlet.Filter;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Nền chung cho test tích hợp có Spring context: profile "test" (application-test.yml), PostgreSQL thật dùng chung
 * (PostgresTestContainer) và Clock điều khiển được (TestClockConfig). Mọi lớp kế thừa dùng chung một context đã cache.
 * Test KHÔNG được giả định DB trống: mỗi test tự tạo user/dữ liệu bằng UUID mới (TESTING_RULES §4.3).
 * <p>
 * {@link #mvc} là MockMvc dựng từ context thật kèm đúng chuỗi filter của ứng dụng (correlationId, giới hạn body,
 * Spring Security). MockMvc không đi qua Tomcat nên địa chỉ client đặt được bằng {@link #fromIp}: mỗi test dùng IP
 * riêng để bucket rate limit theo IP không ảnh hưởng nhau. Kiểm tra RemoteIpValve phải dùng HTTP thật.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestClockConfig.class, LockOrderGuardConfig.class, TestMailConfig.class})
@TestPropertySource(properties = {
        // Job định kỳ tắt: test gọi OutboxWorker.runOnce() để xác định; tránh worker nền xử lý sự kiện của test khác
        // (kể cả trong context dùng profile dev/prod).
        "lifeos.scheduling.enabled=false",
        "spring.mail.host=localhost", "spring.mail.port=2525",
        "lifeos.mail.from=LifeOS Test <no-reply@lifeos.test>",
        "lifeos.mail.app-base-url=https://app.lifeos.test"})
public abstract class IntegrationTestBase {

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected MutableClock clock;

    protected MockMvc mvc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        var pg = PostgresTestContainer.INSTANCE;
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
    }

    @BeforeEach
    void buildMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(CorrelationIdFilter.class),
                        context.getBean(RequestSizeLimitFilter.class),
                        context.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    /** Địa chỉ IP ngẫu nhiên trong dải riêng 10.0.0.0/8. */
    protected static String randomIp() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return "10." + r.nextInt(256) + "." + r.nextInt(256) + "." + (1 + r.nextInt(254));
    }

    protected static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }
}
