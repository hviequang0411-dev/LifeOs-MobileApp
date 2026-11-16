package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * D87, D94: ở context chính (cấu hình mặc định của ứng dụng), mọi kết nối từ pool có lock_timeout = 5 giây, đặt bằng
 * tham số JDBC {@code options}. SYN-17/45 chạy với 1 giây trong context riêng (LockTimeoutIT); test này khẳng định
 * giá trị mặc định không bị hạ.
 */
class LockTimeoutDefaultIT extends IntegrationTestBase {

    @Autowired
    JdbcClient jdbc;

    @Test
    void pooledConnectionsUseA5SecondLockTimeout() {
        assertThat(jdbc.sql("SHOW lock_timeout").query(String.class).single()).isEqualTo("5s");
    }
}
