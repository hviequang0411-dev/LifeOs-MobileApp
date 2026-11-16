package com.lifeos.finance;

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
 * FIN-23: EXPLAIN các truy vấn số dư, mức dùng ngân sách, báo cáo và lọc/tìm kiếm với 100.000 giao dịch của user đích
 * (giữa 300.000 giao dịch của 30 user khác). Dùng CHÍNH các hằng SQL của FinanceJdbc (tham số thay bằng hằng). Không
 * được Seq Scan trên financial_transactions / account_transfers. Kế hoạch ghi ra target/explain-finance.txt.
 */
class FinanceQueryPlanIT extends ApiTestBase {

    private static final int TARGET = 100_000;

    @Test
    void Fin_23_financeQueriesUseIndexesAt100kTransactions() throws Exception {
        UUID user = TestUsers.insert(jdbc);
        UUID account = seedAccount(user);
        UUID category = UUID.randomUUID();
        jdbc.sql("INSERT INTO financial_categories (id, user_id, name, kind) VALUES (:c, :u, 'Ăn', 'EXPENSE')")
                .param("c", category).param("u", user).update();
        seedTransactions(user, account, category, TARGET);
        for (int i = 0; i < 30; i++) {
            UUID other = TestUsers.insert(jdbc);
            seedTransactions(other, seedAccount(other), null, 10_000);
        }
        UUID other = seedAccount(user);
        jdbc.sql("""
                INSERT INTO account_transfers (id, user_id, from_account_id, to_account_id, amount, currency,
                                               occurred_on)
                SELECT gen_random_uuid(), :u, :a, :b, 1000, 'VND', DATE '2024-01-01' + (g % 900)
                FROM generate_series(1, 5000) g
                """).param("u", user).param("a", account).param("b", other).update();
        jdbc.sql("ANALYZE financial_transactions").update();
        jdbc.sql("ANALYZE account_transfers").update();
        jdbc.sql("ANALYZE financial_accounts").update();
        jdbc.sql("ANALYZE financial_categories").update();

        Map<String, String> params = new LinkedHashMap<>();
        params.put(":userId", "'" + user + "'");
        params.put("(:ids)", "('" + account + "')");
        params.put(":asOf", "DATE '2026-11-07'");
        params.put(":currency", "'VND'");
        params.put(":categoryId", "'" + category + "'");
        params.put(":from", "DATE '2026-10-01'");
        params.put(":until", "DATE '2026-11-01'");
        params.put(":to", "DATE '2026-10-31'");

        Map<String, String> queries = new LinkedHashMap<>();
        queries.put("số dư (BALANCES)", bind(FinanceJdbc.BALANCES, params));
        queries.put("mức dùng ngân sách theo danh mục (SPENT)", bind(FinanceJdbc.SPENT, params));
        queries.put("mức dùng ngân sách tổng (SPENT, categoryId NULL)",
                bind(FinanceJdbc.SPENT, Map.of(":categoryId", "NULL")).replace(":userId", "'" + user + "'")
                        .replace(":currency", "'VND'").replace(":from", "DATE '2026-10-01'")
                        .replace(":until", "DATE '2026-11-01'"));
        queries.put("báo cáo tháng (REPORT_ROWS)", bind(FinanceJdbc.REPORT_ROWS, params));
        String u = "'" + user + "'";
        queries.put("lọc theo tài khoản + khoảng ngày (keyset)",
                "SELECT t.id FROM financial_transactions t WHERE t.user_id = " + u + " AND t.deleted_at IS NULL"
                        + " AND t.account_id = '" + account + "' AND t.occurred_on >= DATE '2026-01-01'"
                        + " AND t.occurred_on <= DATE '2026-03-31' ORDER BY t.occurred_on DESC, t.id DESC LIMIT 51");
        queries.put("danh sách mặc định (keyset)",
                "SELECT t.id FROM financial_transactions t WHERE t.user_id = " + u + " AND t.deleted_at IS NULL"
                        + " ORDER BY t.occurred_on DESC, t.id DESC LIMIT 51");
        queries.put("tìm q trong note",
                "SELECT t.id FROM financial_transactions t WHERE t.user_id = " + u + " AND t.deleted_at IS NULL"
                        + " AND t.note ILIKE '%cafe%' ESCAPE '\\' ORDER BY t.occurred_on DESC, t.id DESC LIMIT 51");
        queries.put("lọc danh mục", "SELECT t.id FROM financial_transactions t WHERE t.user_id = " + u
                + " AND t.deleted_at IS NULL AND t.category_id = '" + category + "'"
                + " ORDER BY t.occurred_on DESC, t.id DESC LIMIT 51");

        StringBuilder report = new StringBuilder("# EXPLAIN Finance — user đích " + TARGET
                + " giao dịch, 20 user khác × 5.000\n");
        List<String> seqScans = new ArrayList<>();
        for (Map.Entry<String, String> q : queries.entrySet()) {
            String plan = String.join("\n", jdbc.sql("EXPLAIN " + q.getValue()).query(String.class).list());
            report.append("\n## ").append(q.getKey()).append("\n").append(q.getValue()).append("\n").append(plan)
                    .append("\n");
            if (plan.contains("Seq Scan on financial_transactions") || plan.contains("Seq Scan on account_transfers")) {
                seqScans.add(q.getKey() + ":\n" + plan);
            }
        }
        Path out = Path.of("target", "explain-finance.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);
        assertThat(seqScans).as("không Seq Scan trên bảng lớn").isEmpty();
    }

    private static String bind(String sql, Map<String, String> params) {
        String out = sql;
        for (Map.Entry<String, String> p : params.entrySet()) {
            out = out.replace(p.getKey(), p.getValue());
        }
        return out;
    }

    private UUID seedAccount(UUID user) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO financial_accounts (id, user_id, name, account_type, currency) VALUES (:id, :u, :n,"
                + " 'BANK', 'VND')").param("id", id).param("u", user).param("n", "TK " + id).update();
        return id;
    }

    private void seedTransactions(UUID user, UUID account, UUID category, int count) {
        jdbc.sql("""
                INSERT INTO financial_transactions (id, user_id, account_id, category_id, kind, amount, currency,
                                                    occurred_on, note, deleted_at)
                SELECT gen_random_uuid(), :u, :a,
                       CASE WHEN CAST(:c AS uuid) IS NOT NULL AND g % 20 = 0 THEN CAST(:c AS uuid) ELSE NULL END,
                       CASE WHEN g % 4 = 0 THEN 'INCOME' ELSE 'EXPENSE' END, 1000 + g % 50000, 'VND',
                       DATE '2024-01-01' + (g % 1040), CASE WHEN g % 7 = 0 THEN 'cafe ' || g ELSE NULL END,
                       CASE WHEN g % 50 = 0 THEN now() ELSE NULL END
                FROM generate_series(1, :n) g
                """).param("u", user).param("a", account).param("c", category, java.sql.Types.OTHER)
                .param("n", count).update();
    }
}
