package com.lifeos.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.support.PostgresTestContainer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

/**
 * BACKEND_MASTER_PLAN §7: migration mới chạy sạch TỪ DB ĐÃ CÓ DỮ LIỆU Giai đoạn 3 (không chỉ từ DB rỗng). Database
 * riêng trong cùng container: migrate tới V4, chèn dữ liệu Phase 1 (user, settings, project, task, change_logs,
 * sync_mutations, outbox), rồi migrate lên bản cuối — dữ liệu cũ còn nguyên, ràng buộc mới có hiệu lực.
 */
class MigrationUpgradeIT {

    private static Connection admin() throws SQLException {
        var pg = PostgresTestContainer.INSTANCE;
        return DriverManager.getConnection(pg.getJdbcUrl(), pg.getUsername(), pg.getPassword());
    }

    private static String urlOf(String database) {
        String url = PostgresTestContainer.INSTANCE.getJdbcUrl();
        int slash = url.lastIndexOf('/');
        int query = url.indexOf('?', slash);
        return url.substring(0, slash + 1) + database + (query >= 0 ? url.substring(query) : "");
    }

    private static void exec(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        }
    }

    @Test
    void latestMigrationsApplyCleanlyOnADatabaseWithPhase1Data() throws Exception {
        String database = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = admin(); Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + database);
        }
        var pg = PostgresTestContainer.INSTANCE;
        Flyway toV4 = Flyway.configure().dataSource(urlOf(database), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration").target("4").load();
        assertThat(toV4.migrate().success).isTrue();

        UUID user = UUID.randomUUID();
        UUID project = UUID.randomUUID();
        UUID task = UUID.randomUUID();
        try (Connection c = DriverManager.getConnection(urlOf(database), pg.getUsername(), pg.getPassword())) {
            exec(c, "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'x', 'Cũ')", user,
                    user + "@example.test");
            exec(c, "INSERT INTO user_settings (id, user_id, timezone) VALUES (?, ?, 'Asia/Ho_Chi_Minh')",
                    UUID.randomUUID(), user);
            exec(c, "INSERT INTO projects (id, user_id, name) VALUES (?, ?, 'Dự án cũ')", project, user);
            exec(c, "INSERT INTO tasks (id, user_id, title, project_id) VALUES (?, ?, 'Việc cũ', ?)", task, user,
                    project);
            exec(c, "INSERT INTO user_sync_state (user_id, last_seq) VALUES (?, 2)", user);
            exec(c, "INSERT INTO change_logs (user_id, seq, entity_type, entity_id, operation, entity_version)"
                    + " VALUES (?, 1, 'project', ?, 'CREATE', 1), (?, 2, 'task', ?, 'CREATE', 1)", user, project,
                    user, task);
            exec(c, "INSERT INTO sync_mutations (user_id, mutation_id, entity_type, entity_id, operation,"
                    + " base_version, status, result) VALUES (?, ?, 'task', ?, 'CREATE', 0, 'APPLIED', '{}'::jsonb)",
                    user, UUID.randomUUID(), task);
            exec(c, "INSERT INTO outbox_events (event_type, payload) VALUES ('PASSWORD_CHANGED', '{}'::jsonb)");
        }

        Flyway latest = Flyway.configure().dataSource(urlOf(database), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration").load();
        MigrateResult result = latest.migrate();
        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isPositive();
        assertThat(latest.info().pending()).isEmpty();

        try (Connection c = DriverManager.getConnection(urlOf(database), pg.getUsername(), pg.getPassword())) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT (SELECT count(*) FROM tasks WHERE id = ? AND project_id = ?),"
                            + " (SELECT count(*) FROM change_logs WHERE user_id = ?),"
                            + " (SELECT count(*) FROM sync_mutations WHERE user_id = ?)")) {
                ps.setObject(1, task);
                ps.setObject(2, project);
                ps.setObject(3, user);
                ps.setObject(4, user);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertThat(rs.getInt(1)).as("dữ liệu Phase 1 còn nguyên").isEqualTo(1);
                    assertThat(rs.getInt(2)).isEqualTo(2);
                    assertThat(rs.getInt(3)).isEqualTo(1);
                }
            }
            // Ràng buộc mới: entity_type mới được nhận, giá trị lạ vẫn bị chặn.
            exec(c, "INSERT INTO change_logs (user_id, seq, entity_type, entity_id, operation, entity_version)"
                    + " VALUES (?, 3, 'habit', ?, 'CREATE', 1)", user, UUID.randomUUID());
            exec(c, "INSERT INTO change_logs (user_id, seq, entity_type, entity_id, operation, entity_version)"
                    + " VALUES (?, 5, 'financial_transaction', ?, 'CREATE', 1)", user, UUID.randomUUID());
            assertThatThrownBy(() -> exec(c, "INSERT INTO change_logs (user_id, seq, entity_type, entity_id,"
                    + " operation, entity_version) VALUES (?, 4, 'unknown', ?, 'CREATE', 1)", user, UUID.randomUUID()))
                    .isInstanceOf(SQLException.class);
        }
        try (Connection c = admin(); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE " + database);
        }
    }

    /** WP4 (PHASE_2 D250, §4 kế hoạch): nâng cấp từ V6 có dữ liệu Phase 2–3 — reminder_time chuyển thành reminders. */
    @Test
    void phase4MigrationsCarryHabitAndRoutineReminderTimesIntoReminders() throws Exception {
        String database = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = admin(); Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + database);
        }
        var pg = PostgresTestContainer.INSTANCE;
        assertThat(Flyway.configure().dataSource(urlOf(database), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration").target("6").load().migrate().success).isTrue();
        UUID user = UUID.randomUUID();
        UUID habit = UUID.randomUUID();
        UUID silentHabit = UUID.randomUUID();
        UUID routine = UUID.randomUUID();
        try (Connection c = DriverManager.getConnection(urlOf(database), pg.getUsername(), pg.getPassword())) {
            exec(c, "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'x', 'Cũ')", user,
                    user + "@example.test");
            exec(c, "INSERT INTO habits (id, user_id, name, schedule_type, start_on, reminder_time) VALUES"
                    + " (?, ?, 'Đọc', 'DAILY', DATE '2026-01-01', TIME '21:00')", habit, user);
            exec(c, "INSERT INTO habits (id, user_id, name, schedule_type, start_on) VALUES"
                    + " (?, ?, 'Im', 'DAILY', DATE '2026-01-01')", silentHabit, user);
            exec(c, "INSERT INTO routines (id, user_id, name, kind, reminder_time) VALUES"
                    + " (?, ?, 'Sáng', 'DAILY', TIME '06:30')", routine, user);
            exec(c, "INSERT INTO financial_accounts (id, user_id, name, account_type, currency) VALUES"
                    + " (?, ?, 'Ví', 'CASH', 'VND')", UUID.randomUUID(), user);
        }
        assertThat(Flyway.configure().dataSource(urlOf(database), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration").load().migrate().success).isTrue();
        try (Connection c = DriverManager.getConnection(urlOf(database), pg.getUsername(), pg.getPassword());
                PreparedStatement ps = c.prepareStatement("SELECT target_type, target_id, mode, local_time::text,"
                        + " status, next_fire_at FROM reminders WHERE user_id = ? ORDER BY target_type")) {
            ps.setObject(1, user);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("HABIT");
                assertThat(rs.getObject(2, UUID.class)).isEqualTo(habit);
                assertThat(rs.getString(3)).isEqualTo("LOCAL_AT");
                assertThat(rs.getString(4)).isEqualTo("21:00:00");
                assertThat(rs.getString(5)).isEqualTo("ACTIVE");
                assertThat(rs.getObject(6)).as("ReminderRunService lập lịch lần đầu").isNull();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString(1)).isEqualTo("ROUTINE");
                assertThat(rs.getObject(2, UUID.class)).isEqualTo(routine);
                assertThat(rs.next()).as("habit không có reminder_time không sinh reminder").isFalse();
            }
        }
        try (Connection c = admin(); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE " + database);
        }
    }

    /**
     * Đợt sửa backend 1 (V13, F5 + F8): nâng cấp từ V12 có dữ liệu — yêu cầu xuất trùng (do đua trước đợt sửa) được
     * gộp còn một yêu cầu đang chạy để tạo được chỉ mục UNIQUE; outbox_events.user_id được điền từ aggregate/payload.
     */
    @Test
    void backendFixMigrationDedupesActiveExportsAndBackfillsOutboxOwners() throws Exception {
        String database = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = admin(); Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + database);
        }
        var pg = PostgresTestContainer.INSTANCE;
        assertThat(Flyway.configure().dataSource(urlOf(database), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration").target("12").load().migrate().success).isTrue();
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID firstExport = UUID.randomUUID();
        UUID duplicateExport = UUID.randomUUID();
        UUID budgetEvent = UUID.randomUUID();
        UUID userEvent = UUID.randomUUID();
        UUID finishedEvent = UUID.randomUUID();
        UUID brokenEvent = UUID.randomUUID();
        try (Connection c = DriverManager.getConnection(urlOf(database), pg.getUsername(), pg.getPassword())) {
            for (UUID u : new UUID[] {user, other}) {
                exec(c, "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'x', 'Cũ')", u,
                        u + "@example.test");
            }
            exec(c, "INSERT INTO data_exports (id, user_id, status, requested_at) VALUES"
                    + " (?, ?, 'RUNNING', now() - interval '2 minutes'), (?, ?, 'PENDING', now() - interval '1 minute'),"
                    + " (?, ?, 'PENDING', now())", firstExport, user, duplicateExport, user, UUID.randomUUID(), other);
            exec(c, "INSERT INTO outbox_events (id, event_type, aggregate_type, aggregate_id, payload) VALUES"
                    + " (?, 'BUDGET_THRESHOLD', 'budget', ?, jsonb_build_object('userId', ?::text))", budgetEvent,
                    UUID.randomUUID(), user);
            exec(c, "INSERT INTO outbox_events (id, event_type, aggregate_type, aggregate_id, payload) VALUES"
                    + " (?, 'PASSWORD_CHANGED', 'user', ?, '{}'::jsonb)", userEvent, user);
            exec(c, "INSERT INTO outbox_events (id, event_type, aggregate_type, aggregate_id, payload, status,"
                    + " processed_at) VALUES (?, 'BUDGET_THRESHOLD', 'budget', ?, '{}'::jsonb, 'SENT', now())",
                    finishedEvent, UUID.randomUUID());
            exec(c, "INSERT INTO outbox_events (id, event_type, payload) VALUES (?, 'BUDGET_THRESHOLD',"
                    + " '{\"userId\": \"không-phải-uuid\"}'::jsonb)", brokenEvent);
        }
        assertThat(Flyway.configure().dataSource(urlOf(database), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration").load().migrate().success).isTrue();
        try (Connection c = DriverManager.getConnection(urlOf(database), pg.getUsername(), pg.getPassword())) {
            try (PreparedStatement ps = c.prepareStatement("SELECT status, error_code FROM data_exports WHERE id = ?")) {
                ps.setObject(1, firstExport);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString(1)).as("yêu cầu sớm nhất được giữ").isEqualTo("RUNNING");
                }
                ps.setObject(1, duplicateExport);
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString(1)).isEqualTo("FAILED");
                    assertThat(rs.getString(2)).isEqualTo("DUPLICATE");
                }
            }
            try (PreparedStatement ps = c.prepareStatement("SELECT user_id FROM outbox_events WHERE id = ?")) {
                Object[][] expected = {{budgetEvent, user}, {userEvent, user}, {finishedEvent, null},
                        {brokenEvent, null}};
                for (Object[] row : expected) {
                    ps.setObject(1, row[0]);
                    try (ResultSet rs = ps.executeQuery()) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getObject(1)).isEqualTo(row[1]);
                    }
                }
            }
            assertThatThrownBy(() -> exec(c, "INSERT INTO data_exports (id, user_id, status, requested_at) VALUES"
                    + " (?, ?, 'PENDING', now())", UUID.randomUUID(), user)).as("chỉ một yêu cầu đang chạy mỗi user")
                    .isInstanceOf(SQLException.class);
        }
        try (Connection c = admin(); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE " + database);
        }
    }

    /** WP6 (PHASE_6 §5): nâng cấp từ V9 có Task cũ — goal_id/milestone_id NULL, change_logs nhận loại mới, FTS chạy. */
    @Test
    void phase6MigrationKeepsExistingTasksAndEnablesAccentInsensitiveSearch() throws Exception {
        String database = "upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection c = admin(); Statement s = c.createStatement()) {
            s.execute("CREATE DATABASE " + database);
        }
        var pg = PostgresTestContainer.INSTANCE;
        assertThat(Flyway.configure().dataSource(urlOf(database), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration").target("9").load().migrate().success).isTrue();
        UUID user = UUID.randomUUID();
        UUID task = UUID.randomUUID();
        try (Connection c = DriverManager.getConnection(urlOf(database), pg.getUsername(), pg.getPassword())) {
            exec(c, "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'x', 'Cũ')", user,
                    user + "@example.test");
            exec(c, "INSERT INTO tasks (id, user_id, title) VALUES (?, ?, 'Việc cũ')", task, user);
        }
        assertThat(Flyway.configure().dataSource(urlOf(database), pg.getUsername(), pg.getPassword())
                .locations("classpath:db/migration").load().migrate().success).isTrue();
        try (Connection c = DriverManager.getConnection(urlOf(database), pg.getUsername(), pg.getPassword())) {
            exec(c, "INSERT INTO notes (id, user_id, title, content) VALUES (?, ?, 'Hóa đơn điện', 'Tháng 11')",
                    UUID.randomUUID(), user);
            exec(c, "INSERT INTO change_logs (user_id, seq, entity_type, entity_id, operation, entity_version)"
                    + " VALUES (?, 1, 'note', ?, 'CREATE', 1)", user, UUID.randomUUID());
            try (PreparedStatement ps = c.prepareStatement("SELECT goal_id, milestone_id, (SELECT count(*) FROM notes"
                    + " WHERE search_vector @@ websearch_to_tsquery('simple', lifeos_unaccent('hoa don dien')))"
                    + " FROM tasks WHERE id = ?")) {
                ps.setObject(1, task);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getObject(1)).isNull();
                    assertThat(rs.getObject(2)).isNull();
                    assertThat(rs.getLong(3)).isEqualTo(1);
                }
            }
        }
        try (Connection c = admin(); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE " + database);
        }
    }
}
