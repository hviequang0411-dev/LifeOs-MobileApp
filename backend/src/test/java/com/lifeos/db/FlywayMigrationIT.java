package com.lifeos.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.support.PostgresTestContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Kiểm tra V1–V4 trên PostgreSQL thật: migration chạy được và các ràng buộc quan trọng do DB bảo vệ
 * (không chỉ dựa vào validation ứng dụng). Xem TESTING_RULES.md §8.
 */
class FlywayMigrationIT {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String CHECK_VIOLATION = "23514";
    private static final String FK_VIOLATION = "23503";

    private static Flyway flyway;
    private static MigrateResult result;

    /**
     * Container dùng chung với các test tích hợp khác (ví dụ ApplicationStartupIT) nên DB có thể đã được Spring
     * migrate trước. migrate() là idempotent: chạy trên DB trống thì áp dụng V1–V4, trên DB đã migrate thì không
     * làm gì. Vì vậy test chỉ kiểm TRẠNG THÁI schema, không đếm số bản vừa chạy.
     */
    @BeforeAll
    static void migrate() {
        var pg = PostgresTestContainer.INSTANCE;
        flyway = Flyway.configure()
                .dataSource(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load();
        result = flyway.migrate();
    }

    private static Connection connection() throws SQLException {
        var pg = PostgresTestContainer.INSTANCE;
        return DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
    }

    private static void exec(String sql, Object... params) throws SQLException {
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }

    private static UUID newUser() throws SQLException {
        UUID id = UUID.randomUUID();
        exec("INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'x', 'Test')",
                id, id + "@example.com");
        return id;
    }

    private static void assertSqlState(String expected, ThrowingSql action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(SQLException.class)
                .satisfies(e -> assertThat(((SQLException) e).getSQLState()).isEqualTo(expected));
    }

    @FunctionalInterface
    interface ThrowingSql {
        void run() throws SQLException;
    }

    /**
     * Mọi migration trong classpath (V1… bản cuối) đã áp dụng, đúng thứ tự, thành công. Danh sách mong đợi lấy từ chính
     * các tệp db/migration/V*__*.sql để thêm migration mới (từ V5, Phase 2 trở đi) không phải sửa test này — vẫn chặt:
     * thiếu, thừa hay đổi thứ tự đều đỏ.
     */
    @Test
    void allMigrationsAreApplied() throws Exception {
        assertThat(result.success).isTrue();

        List<String> expected = new java.util.ArrayList<>();
        var resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        for (var resource : resolver.getResources("classpath:db/migration/V*__*.sql")) {
            String name = resource.getFilename();
            expected.add(name.substring(1, name.indexOf("__")));
        }
        expected.sort(java.util.Comparator.comparingInt(Integer::parseInt));
        assertThat(expected).as("V1–V4 của Phase 1 vẫn còn").startsWith("1", "2", "3", "4");

        MigrationInfoService info = flyway.info();
        List<MigrationInfo> applied = Arrays.stream(info.applied())
                .filter(MigrationInfo::isVersioned)
                .toList();
        assertThat(applied)
                .extracting(m -> m.getVersion().getVersion())
                .containsExactlyElementsOf(expected);
        assertThat(applied).allSatisfy(m -> assertThat(m.getState()).isEqualTo(MigrationState.SUCCESS));
        assertThat(info.current().getVersion().getVersion()).isEqualTo(expected.get(expected.size() - 1));
        assertThat(info.pending()).isEmpty();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();

        // Gọi lại migrate() trên DB đã migrate phải an toàn: thành công và không áp dụng thêm bản nào.
        MigrateResult again = flyway.migrate();
        assertThat(again.success).isTrue();
        assertThat(again.migrationsExecuted).isZero();
    }

    @Test
    void expectedTablesExist() throws SQLException {
        Set<String> tables = new HashSet<>();
        try (Connection c = connection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        assertThat(tables).contains("users", "user_settings", "devices", "refresh_tokens",
                "email_verification_tokens", "password_reset_tokens", "user_sync_state", "change_logs",
                "sync_mutations", "categories", "projects", "tasks", "outbox_events", "flyway_schema_history",
                "habits", "habit_checkins", "routines", "routine_steps", "routine_logs", "routine_step_logs",
                "financial_accounts", "financial_categories", "financial_transactions", "account_transfers",
                "budgets", "budget_alerts", "recurring_transactions", "saving_goals", "saving_contributions",
                "calendar_events", "event_exceptions", "time_blocks", "reminders", "notifications",
                "notification_deliveries", "notification_preferences", "device_push_tokens");
    }

    @Test
    void emailIsUniqueCaseInsensitively() throws SQLException {
        String email = "Dup-" + UUID.randomUUID() + "@Example.com";
        exec("INSERT INTO users (email, password_hash, display_name) VALUES (?, 'x', 'A')", email);
        assertSqlState(UNIQUE_VIOLATION, () -> exec(
                "INSERT INTO users (email, password_hash, display_name) VALUES (?, 'x', 'B')", email.toLowerCase()));
    }

    @Test
    void invalidUserStatusIsRejected() {
        assertSqlState(CHECK_VIOLATION, () -> exec(
                "INSERT INTO users (email, password_hash, display_name, status) VALUES (?, 'x', 'A', 'BANNED')",
                UUID.randomUUID() + "@example.com"));
    }

    @Test
    void categoryNameIsUniquePerUserIgnoringCaseAndSoftDeleted() throws SQLException {
        UUID user = newUser();
        UUID first = UUID.randomUUID();
        exec("INSERT INTO categories (id, user_id, name) VALUES (?, ?, 'Work')", first, user);
        assertSqlState(UNIQUE_VIOLATION, () -> exec(
                "INSERT INTO categories (id, user_id, name) VALUES (?, ?, 'work')", UUID.randomUUID(), user));
        // Sau khi xóa mềm, tên được dùng lại.
        exec("UPDATE categories SET deleted_at = now() WHERE id = ?", first);
        exec("INSERT INTO categories (id, user_id, name) VALUES (?, ?, 'work')", UUID.randomUUID(), user);
        // User khác dùng cùng tên được.
        exec("INSERT INTO categories (id, user_id, name) VALUES (?, ?, 'Work')", UUID.randomUUID(), newUser());
    }

    @Test
    void taskRulesAreEnforcedByDatabase() throws SQLException {
        UUID user = newUser();
        // title rỗng
        assertSqlState(CHECK_VIOLATION, () -> exec(
                "INSERT INTO tasks (id, user_id, title) VALUES (?, ?, '   ')", UUID.randomUUID(), user));
        // due_time cần due_date
        assertSqlState(CHECK_VIOLATION, () -> exec(
                "INSERT INTO tasks (id, user_id, title, due_time) VALUES (?, ?, 'T', '08:00')",
                UUID.randomUUID(), user));
        // DONE phải có completed_at
        assertSqlState(CHECK_VIOLATION, () -> exec(
                "INSERT INTO tasks (id, user_id, title, status) VALUES (?, ?, 'T', 'DONE')",
                UUID.randomUUID(), user));
        // OPEN không được có completed_at
        assertSqlState(CHECK_VIOLATION, () -> exec(
                "INSERT INTO tasks (id, user_id, title, completed_at) VALUES (?, ?, 'T', now())",
                UUID.randomUUID(), user));
        // priority không hợp lệ
        assertSqlState(CHECK_VIOLATION, () -> exec(
                "INSERT INTO tasks (id, user_id, title, priority) VALUES (?, ?, 'T', 'URGENT')",
                UUID.randomUUID(), user));
        // tham chiếu project không tồn tại
        assertSqlState(FK_VIOLATION, () -> exec(
                "INSERT INTO tasks (id, user_id, title, project_id) VALUES (?, ?, 'T', ?)",
                UUID.randomUUID(), user, UUID.randomUUID()));
        // hợp lệ
        exec("INSERT INTO tasks (id, user_id, title, due_date, due_time) VALUES (?, ?, 'T', current_date, '08:00')",
                UUID.randomUUID(), user);
    }

    @Test
    void syncTablesEnforceKeysAndAllowedValues() throws SQLException {
        UUID user = newUser();
        exec("INSERT INTO user_sync_state (user_id, last_seq) VALUES (?, 0)", user);
        exec("INSERT INTO change_logs (user_id, seq, entity_type, entity_id, operation, entity_version)"
                + " VALUES (?, 1, 'task', ?, 'CREATE', 1)", user, UUID.randomUUID());
        // (user_id, seq) trùng
        assertSqlState(UNIQUE_VIOLATION, () -> exec(
                "INSERT INTO change_logs (user_id, seq, entity_type, entity_id, operation, entity_version)"
                        + " VALUES (?, 1, 'task', ?, 'UPDATE', 2)", user, UUID.randomUUID()));
        // entity_type lạ
        assertSqlState(CHECK_VIOLATION, () -> exec(
                "INSERT INTO change_logs (user_id, seq, entity_type, entity_id, operation, entity_version)"
                        + " VALUES (?, 2, 'wallet', ?, 'CREATE', 1)", user, UUID.randomUUID()));
        // (user_id, mutation_id) trùng = bảo đảm idempotency
        UUID mutationId = UUID.randomUUID();
        String insert = "INSERT INTO sync_mutations (user_id, mutation_id, entity_type, entity_id, operation,"
                + " base_version, status, result) VALUES (?, ?, 'task', ?, 'CREATE', 0, 'APPLIED', '{}'::jsonb)";
        exec(insert, user, mutationId, UUID.randomUUID());
        assertSqlState(UNIQUE_VIOLATION, () -> exec(insert, user, mutationId, UUID.randomUUID()));
        // Cùng mutation_id nhưng user khác thì được (khóa theo user)
        exec(insert, newUser(), mutationId, UUID.randomUUID());
        // DUPLICATE không được lưu
        assertSqlState(CHECK_VIOLATION, () -> exec(
                "INSERT INTO sync_mutations (user_id, mutation_id, entity_type, entity_id, operation, base_version,"
                        + " status, result) VALUES (?, ?, 'task', ?, 'CREATE', 0, 'DUPLICATE', '{}'::jsonb)",
                user, UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void refreshTokenHasFamilyStartColumnAndUniqueHash() throws SQLException {
        UUID user = newUser();
        UUID device = UUID.randomUUID();
        exec("INSERT INTO devices (id, user_id, name, platform) VALUES (?, ?, 'Pixel', 'ANDROID')", device, user);
        String insert = "INSERT INTO refresh_tokens (user_id, device_id, family_id, token_hash, family_started_at,"
                + " expires_at) VALUES (?, ?, ?, ?, now(), now() + interval '30 days')";
        // Hash ngẫu nhiên (64 ký tự hex) để không đụng dữ liệu của test khác trên DB dùng chung.
        String hash = (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "");
        exec(insert, user, device, UUID.randomUUID(), hash);
        assertSqlState(UNIQUE_VIOLATION, () -> exec(insert, user, device, UUID.randomUUID(), hash));
    }

    @Test
    void outboxRejectsUnknownStatus() {
        assertSqlState(CHECK_VIOLATION, () -> exec(
                "INSERT INTO outbox_events (event_type, payload, status) VALUES ('EMAIL_VERIFICATION', '{}'::jsonb,"
                        + " 'WEIRD')"));
    }
}
