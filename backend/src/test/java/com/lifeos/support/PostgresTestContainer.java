package com.lifeos.support;

import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL thật dùng chung cho mọi test tích hợp trong một lần chạy (singleton container).
 * Cùng phiên bản với docker-compose.yml. Container được Testcontainers (Ryuk) dọn khi JVM kết thúc.
 * Không dùng H2 (TESTING_RULES.md §1.6).
 */
public final class PostgresTestContainer {

    public static final PostgreSQLContainer INSTANCE = new PostgreSQLContainer("postgres:17-alpine");

    static {
        INSTANCE.start();
    }

    private PostgresTestContainer() {
    }
}
