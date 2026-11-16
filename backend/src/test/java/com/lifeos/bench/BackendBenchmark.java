package com.lifeos.bench;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.privacy.UserDataExporter;
import com.lifeos.support.ApiTestBase;
import com.lifeos.support.TestUsers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Benchmark WP8 (BACKEND_MASTER_PLAN §8.2; PHASE_5 D173, PHASE_7 D207, INT-13) — KHÔNG chặn CI: tên không khớp mẫu
 * {@code *IT} nên {@code verify} mặc định không chạy; chạy bằng
 * {@code mvn -f backend/pom.xml verify -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false -Dit.test=BackendBenchmark}.
 * Số đo (median/p95 trên máy phát triển, PostgreSQL Testcontainers) ghi ra {@code target/benchmark.md}.
 */
class BackendBenchmark extends ApiTestBase {

    @Autowired
    UserDataExporter exporter;

    private final Map<String, String> results = new LinkedHashMap<>();

    private record Stats(long medianMs, long p95Ms, long maxMs) {
        @Override
        public String toString() {
            return "median " + medianMs + " ms · p95 " + p95Ms + " ms · max " + maxMs + " ms";
        }
    }

    private Stats measure(int runs, Callable<MvcResult> call) throws Exception {
        List<Long> ms = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            MvcResult r = call.call();
            ms.add((System.nanoTime() - start) / 1_000_000);
            assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isIn(200, 201);
        }
        Collections.sort(ms);
        return new Stats(ms.get(ms.size() / 2), ms.get(Math.min(ms.size() - 1, (int) Math.ceil(ms.size() * 0.95) - 1)),
                ms.get(ms.size() - 1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void benchmark() throws Exception {
        // ---- Sync push 50 / pull 500
        TestUser syncUser = newUser();
        List<Long> push = new ArrayList<>();
        for (int b = 0; b < 12; b++) {
            List<Map<String, Object>> mutations = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                mutations.add(Map.of("mutationId", UUID.randomUUID().toString(), "entityType", "task", "entityId",
                        UUID.randomUUID().toString(), "operation", "CREATE", "baseVersion", 0, "payload",
                        Map.of("title", "Việc " + b + "-" + i, "priority", "HIGH")));
            }
            String body = json.writeValueAsString(Map.of("mutations", mutations));
            long start = System.nanoTime();
            MvcResult r = post(syncUser, "/api/v1/sync/push", body);
            push.add((System.nanoTime() - start) / 1_000_000);
            assertThat(r.getResponse().getStatus()).isEqualTo(200);
        }
        Collections.sort(push);
        results.put("Sync push 50 mutation (task CREATE), 12 lô", "median " + push.get(6) + " ms · max "
                + push.get(11) + " ms");
        results.put("Sync pull 500 thay đổi (một trang)", measure(10, () -> get(syncUser,
                "/api/v1/sync/pull?limit=500")).toString());

        // ---- Snapshot 50k
        UUID snapUser = TestUsers.insert(jdbc);
        jdbc.sql("INSERT INTO tasks (id, user_id, title) SELECT gen_random_uuid(), :u, 'S' || g"
                + " FROM generate_series(1, 50000) g").param("u", snapUser).update();
        TestUser snap = new TestUser(snapUser, jwtService.issue(snapUser, UUID.randomUUID()).token());
        long start = System.nanoTime();
        String token = null;
        String next = null;
        int items = 0;
        int pages = 0;
        do {
            Map<String, Object> req = new LinkedHashMap<>();
            if (token != null) {
                req.put("snapshotToken", token);
                req.put("next", next);
            }
            MvcResult r = post(snap, "/api/v1/sync/snapshot", json.writeValueAsString(req));
            assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(200);
            Map<String, Object> page = readJson(r);
            token = (String) page.get("snapshotToken");
            next = (String) page.get("next");
            for (Map<String, Object> e : (List<Map<String, Object>>) page.get("entities")) {
                items += ((List<?>) e.get("items")).size();
            }
            pages++;
        } while (next != null);
        results.put("Snapshot 50 000 Task (" + pages + " trang, " + items + " bản ghi)",
                (System.nanoTime() - start) / 1_000_000 + " ms tổng");

        // ---- D207: 100k Task, 200k giao dịch, 50k ghi chú
        UUID bigUser = TestUsers.insert(jdbc);
        jdbc.sql("""
                INSERT INTO tasks (id, user_id, title, status, due_date, completed_at, created_at)
                SELECT gen_random_uuid(), :u, 'T' || g, CASE WHEN g % 3 = 0 THEN 'OPEN' ELSE 'DONE' END,
                       CURRENT_DATE - (g % 400),
                       CASE WHEN g % 3 = 0 THEN NULL ELSE now() - (g % 400) * interval '1 day' END,
                       now() - (g % 400 + 3) * interval '1 day'
                FROM generate_series(1, 100000) g
                """).param("u", bigUser).update();
        UUID account = UUID.randomUUID();
        jdbc.sql("INSERT INTO financial_accounts (id, user_id, name, account_type, currency) VALUES (:a, :u, 'TK',"
                + " 'BANK', 'VND')").param("a", account).param("u", bigUser).update();
        jdbc.sql("""
                INSERT INTO financial_transactions (id, user_id, account_id, kind, amount, currency, occurred_on, note)
                SELECT gen_random_uuid(), :u, :a, CASE WHEN g % 4 = 0 THEN 'INCOME' ELSE 'EXPENSE' END,
                       1000 + g % 5000, 'VND', CURRENT_DATE - (g % 700), CASE WHEN g % 5 = 0 THEN '=ghi chú ' || g END
                FROM generate_series(1, 200000) g
                """).param("u", bigUser).param("a", account).update();
        jdbc.sql("INSERT INTO notes (id, user_id, title, content) SELECT gen_random_uuid(), :u, 'N' || g,"
                + " 'Nội dung ghi chú số ' || g FROM generate_series(1, 50000) g").param("u", bigUser).update();
        jdbc.sql("ANALYZE").update();
        TestUser big = new TestUser(bigUser, jwtService.issue(bigUser, UUID.randomUUID()).token());
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
        results.put("GET /dashboard (ngưỡng p95 < 800 ms)", measure(20, () -> get(big, "/api/v1/dashboard"))
                .toString());
        results.put("GET /reviews/daily (ngưỡng < 500 ms)", measure(20, () -> get(big, "/api/v1/reviews/daily"))
                .toString());
        results.put("GET /reviews/monthly (ngưỡng < 500 ms)", measure(20, () -> get(big, "/api/v1/reviews/monthly"))
                .toString());
        results.put("GET /analytics/tasks 12 tháng theo tuần (ngưỡng < 1 s)", measure(10, () -> get(big,
                "/api/v1/analytics/tasks?from=" + today.minusMonths(12) + "&to=" + today + "&granularity=week"))
                .toString());
        results.put("GET /analytics/finance 12 tháng theo tháng (ngưỡng < 1 s)", measure(10, () -> get(big,
                "/api/v1/analytics/finance?currency=VND&from=" + today.minusMonths(12) + "&to=" + today))
                .toString());
        results.put("GET /notes?q=ghi chu 12345 trên 50 000 ghi chú (khớp 1)", measure(20, () -> get(big, "/api/v1/notes?q=ghi chu 12345"))
                .toString());

        // ---- INT-13: xuất dữ liệu với 200k giao dịch + 100k Task + 50k ghi chú
        Runtime rt = Runtime.getRuntime();
        System.gc();
        long before = rt.totalMemory() - rt.freeMemory();
        Path file = Files.createTempDirectory("lifeos-bench").resolve("export.zip");
        start = System.nanoTime();
        long size = exporter.export(bigUser, file, 524_288_000L, Instant.now());
        long elapsed = (System.nanoTime() - start) / 1_000_000;
        long after = rt.totalMemory() - rt.freeMemory();
        results.put("Xuất dữ liệu (INT-13): 200k giao dịch + 100k Task + 50k ghi chú", elapsed + " ms, zip "
                + size / 1024 / 1024 + " MB, heap chênh ≈ " + Math.max(0, after - before) / 1024 / 1024 + " MB");
        Files.deleteIfExists(file);

        StringBuilder md = new StringBuilder("# Benchmark backend (WP8)\n\n| Kịch bản | Kết quả |\n|---|---|\n");
        results.forEach((k, v) -> md.append("| ").append(k).append(" | ").append(v).append(" |\n"));
        Path out = Path.of("target", "benchmark.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, md.toString(), StandardCharsets.UTF_8);
        System.out.println(md);
    }
}
