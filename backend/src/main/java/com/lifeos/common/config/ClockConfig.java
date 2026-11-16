package com.lifeos.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean Clock UTC duy nhất. Mọi nơi cần "bây giờ" phải tiêm Clock (không gọi Instant.now()) để test được.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
