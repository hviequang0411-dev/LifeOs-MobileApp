package com.lifeos.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tham số Argon2id (D46). Giá trị mặc định ở application.yml theo mức tối thiểu OWASP. */
@ConfigurationProperties("lifeos.security.password.argon2")
public record PasswordHashProperties(int saltLength, int hashLength, int parallelism, int memoryKib, int iterations) {
}
