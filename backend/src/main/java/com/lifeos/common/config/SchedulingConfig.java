package com.lifeos.common.config;

import com.lifeos.common.mail.MailProperties;
import com.lifeos.common.outbox.OutboxProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật các job định kỳ (SYSTEM_ARCHITECTURE §3.12: outbox worker ~2 giây, dọn token/outbox hằng ngày; D28: cùng process
 * backend). Tắt được bằng lifeos.scheduling.enabled=false — profile test tắt để test gọi worker một cách xác định.
 */
@Configuration
@EnableConfigurationProperties({OutboxProperties.class, MailProperties.class})
public class SchedulingConfig {

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "lifeos.scheduling.enabled", havingValue = "true", matchIfMissing = true)
    static class Enabled {
    }
}
