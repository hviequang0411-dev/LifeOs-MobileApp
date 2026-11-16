package com.lifeos.support;

import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Gắn {@link LockOrderGuard} quanh DataSource của context test (chỉ cấu hình test, D86 / SYN-48). */
@TestConfiguration(proxyBeanMethods = false)
public class LockOrderGuardConfig {

    @Bean
    static BeanPostProcessor lockOrderGuardPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof LockOrderGuard)) {
                    return new LockOrderGuard(dataSource);
                }
                return bean;
            }
        };
    }
}
