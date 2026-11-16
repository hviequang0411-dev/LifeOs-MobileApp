package com.lifeos.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.finance.repository.FinanceJdbc;
import com.lifeos.support.ApiTestBase;
import com.lifeos.support.TestUsers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * INT-16 (PHASE_7 D207): EXPLAIN các truy vấn tổng hợp với 100 000 Task và 100 000 giao dịch của user đích (giữa dữ liệu
 * của user khác): không Seq Scan trên bảng lớn; chỉ mục ix_tasks_user_completed (V11) được dùng. EXPLAIN ANALYZE ghi
 * ra target/explain-analytics.txt cho báo cáo (ngưỡng thời gian không chặn CI).
 */
class AnalyticsQueryPlanIT extends ApiTestBase {

    @Test
    void Int_16_aggregateQueriesUseIndexesAtScale() throws Exception {
        UUID user = TestUsers.insert(jdbc);
        UUID other = TestUsers.insert(jdbc);
        for (UUID u : List.of(user, other)) {
            jdbc.sql("""
                    INSERT INTO tasks (id, user_id, title, status, due_date, completed_at, created_at)
                    SELECT gen_random_uuid(), :u, 'T' || g, CASE WHEN g % 3 = 0 THEN 'OPEN' ELSE 'DONE' END,
                           DATE '2025-01-01' + (g % 700),
                           CASE WHEN g % 3 = 0 THEN NULL ELSE TIMESTAMPTZ '2025-01-01' + (g % 700) * interval '1 day' END,
                           TIMESTAMPTZ '2024-12-01' + (g % 700) * interval '1 day'
                    FROM generate_series(1, 100000) g
                    """).param("u", u).update();
        }
        UUID account = UUID.randomUUID();
        jdbc.sql("INSERT INTO financial_accounts (id, user_id, name, account_type, currency) VALUES (:id, :u, 'TK',"
                + " 'BANK', 'VND')").param("id", account).param("u", user).update();
        jdbc.sql("""
                INSERT INTO financial_transactions (id, user_id, account_id, kind, amount, currency, occurred_on)
                SELECT gen_random_uuid(), :u, :a, CASE WHEN g % 4 = 0 THEN 'INCOME' ELSE 'EXPENSE' END,
                       1000 + g % 5000, CASE WHEN g % 10 = 0 THEN 'USD' ELSE 'VND' END, DATE '2024-01-01' + (g % 1000)
                FROM generate_series(1, 100000) g
                """).param("u", user).param("a", account).update();
        jdbc.sql("ANALYZE tasks").update();
        jdbc.sql("ANALYZE financial_transactions").update();

        String u = "'" + user + "'";
        Map<String, String> queries = new LinkedHashMap<>();
        queries.put("Task hoàn thành trong kỳ (TaskStatsApi.completedBetween)",
                "SELECT created_at, completed_at, due_date, due_time, project_id, category_id FROM tasks"
                        + " WHERE user_id = " + u + " AND deleted_at IS NULL AND completed_at >= TIMESTAMPTZ"
                        + " '2025-03-01' AND completed_at < TIMESTAMPTZ '2025-04-01' ORDER BY completed_at LIMIT 200000");
        queries.put("Task quá hạn (TaskStatsApi.overdueOpen)", "SELECT count(*) FROM tasks WHERE user_id = " + u
                + " AND deleted_at IS NULL AND status = 'OPEN' AND due_date < DATE '2026-11-07'");
        queries.put("Tổng theo tiền tệ (FinanceJdbc.CURRENCY_TOTALS)", FinanceJdbc.CURRENCY_TOTALS
                .replace(":userId", u).replace(":from", "DATE '2026-10-01'").replace(":to", "DATE '2026-10-31'"));

        StringBuilder report = new StringBuilder("# EXPLAIN analytics — 100 000 Task + 100 000 giao dịch/user\n");
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, String> q : queries.entrySet()) {
            String plan = String.join("\n", jdbc.sql("EXPLAIN " + q.getValue()).query(String.class).list());
            String analyzed = String.join("\n", jdbc.sql("EXPLAIN (ANALYZE, BUFFERS) " + q.getValue())
                    .query(String.class).list());
            report.append("\n## ").append(q.getKey()).append("\n").append(q.getValue()).append("\n")
                    .append(analyzed).append("\n");
            if (plan.contains("Seq Scan on tasks") || plan.contains("Seq Scan on financial_transactions")) {
                problems.add(q.getKey() + ":\n" + plan);
            }
        }
        Path out = Path.of("target", "explain-analytics.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
        assertThat(problems).as("không Seq Scan trên bảng lớn").isEmpty();
        assertThat(String.join("\n", jdbc.sql("EXPLAIN " + queries.values().iterator().next()).query(String.class)
                .list())).contains("ix_tasks_user_completed");
    }
}
