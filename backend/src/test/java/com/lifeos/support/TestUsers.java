package com.lifeos.support;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Tạo user tối thiểu bằng SQL cho test hạ tầng (khi module identity chưa có use case đăng ký). Mỗi lần gọi một UUID
 * và email mới, nên không đụng dữ liệu của test khác.
 */
public final class TestUsers {

    private TestUsers() {
    }

    public static UUID insert(JdbcClient jdbc) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO users (id, email, password_hash, display_name) VALUES (:id, :email, 'x', 'Test')")
                .param("id", id)
                .param("email", id + "@example.test")
                .update();
        return id;
    }
}
