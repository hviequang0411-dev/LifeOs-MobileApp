package com.lifeos.support;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Clock điều khiển được cho test tích hợp: thay bean Clock UTC của ứng dụng (@Primary). Bắt đầu từ giờ thật (cắt ở
 * micro giây, đúng độ chính xác của TIMESTAMPTZ) để so sánh trực tiếp với giá trị đọc từ DB. Test chỉ được tiến clock
 * về phía trước để không ảnh hưởng test khác dùng chung context.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(Instant.now().truncatedTo(ChronoUnit.MICROS));
    }
}
