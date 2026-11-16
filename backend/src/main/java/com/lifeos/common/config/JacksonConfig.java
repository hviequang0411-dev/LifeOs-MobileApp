package com.lifeos.common.config;

import com.lifeos.common.web.StrictJson;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Cấu hình Jackson dùng chung. Spring Boot mặc định bỏ qua trường lạ; với DTO {@link StrictJson} trường lạ bị từ chối
 * (Spring chuyển thành HttpMessageNotReadableException → 400 MALFORMED_REQUEST).
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer strictJsonForSensitiveRequests() {
        return builder -> builder.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p,
                    ValueDeserializer<?> deserializer, Object beanOrClass, String propertyName)
                    throws JacksonException {
                Class<?> type = beanOrClass instanceof Class<?> c ? c : beanOrClass.getClass();
                if (StrictJson.class.isAssignableFrom(type)) {
                    throw UnrecognizedPropertyException.from(p, beanOrClass, propertyName, null);
                }
                return false; // không xử lý: Jackson áp cấu hình mặc định (bỏ qua trường lạ)
            }
        });
    }
}
