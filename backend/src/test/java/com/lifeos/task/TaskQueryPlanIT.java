package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Kiểm tra kế hoạch thực thi (DATABASE_DESIGN §6, §12; quyết định Q20 Giai đoạn 2): sinh dữ liệu mẫu (một user 5.000
 * Task giữa 50.000 Task của 50 user khác), ANALYZE, rồi EXPLAIN các truy vấn Q1, Q5, Q6, Q7 ở dạng SQL tương đương câu
 * do ứng dụng sinh. Kế hoạch được ghi ra target/explain-plans.txt để đưa vào báo cáo.
 * <p>
 * Assert: các truy vấn đã có chỉ mục theo thiết kế (Q1 pull theo PK change_logs, Q6 tiến độ và Q7 cascade theo
 * ix_tasks_project/ix_tasks_category) KHÔNG Seq Scan. Q5 (danh sách theo các kiểu sắp xếp) chỉ được ghi lại — chưa có
 * chỉ mục cho mọi thứ tự sắp xếp và Q20 quyết định chưa thêm V5; kết quả dùng để quyết định ở báo cáo.
 */
class TaskQueryPlanIT extends ApiTestBase {

    private static final int TARGET_TASKS = 5_000;
    private static final int OTHER_USERS = 50;
    private static final int TASKS_PER_OTHER_USER = 1_000;

    private String plan(String sql) {
        return String.join("\n", jdbc.sql("EXPLAIN " + sql).query(String.class).list());
    }

    @Test
    void indexedQueriesAvoidSequentialScansAndPlansAreRecorded() throws Exception {
        UUID user = TestUsers.insert(jdbc);
        UUID project = UUID.randomUUID();
        UUID category = UUID.randomUUID();
        jdbc.sql("INSERT INTO projects (id, user_id, name) VALUES (:p, :u, 'Mẫu')")
                .param("p", project).param("u", user).update();
        jdbc.sql("INSERT INTO categories (id, user_id, name) VALUES (:c, :u, 'Mẫu')")
                .param("c", category).param("u", user).update();
        seedTasks(user, TARGET_TASKS, project, category);
        for (int i = 0; i < OTHER_USERS; i++) {
            seedTasks(TestUsers.insert(jdbc), TASKS_PER_OTHER_USER, null, null);
        }
        jdbc.sql("INSERT INTO change_logs (user_id, seq, entity_type, entity_id, operation, entity_version)"
                + " SELECT :u, g, 'task', gen_random_uuid(), 'CREATE', 1 FROM generate_series(1, 5000) g")
                .param("u", user).update();
        jdbc.sql("ANALYZE tasks").update();
        jdbc.sql("ANALYZE change_logs").update();

        String u = "'" + user + "'";
        String live = "user_id = " + u + " AND deleted_at IS NULL";
        Map<String, String> queries = new LinkedHashMap<>();
        queries.put("Q1 pull (change_logs theo seq)",
                "SELECT * FROM change_logs WHERE user_id = " + u + " AND seq > 1000 ORDER BY seq LIMIT 201");
        queries.put("Q5a danh sách mặc định (createdAt:desc)",
                "SELECT * FROM tasks WHERE " + live + " ORDER BY created_at DESC, id DESC LIMIT 51");
        queries.put("Q5b lọc status + khoảng hạn, sắp dueDate:asc",
                "SELECT * FROM tasks WHERE " + live + " AND status = 'OPEN' AND due_date >= '2026-11-01'"
                        + " AND due_date <= '2026-11-30' ORDER BY (due_date IS NULL), due_date, id LIMIT 51");
        queries.put("Q5c sắp priority:desc",
                "SELECT * FROM tasks WHERE " + live + " ORDER BY CASE priority WHEN 'NONE' THEN 0 WHEN 'LOW' THEN 1"
                        + " WHEN 'MEDIUM' THEN 2 WHEN 'HIGH' THEN 3 END DESC, id DESC LIMIT 51");
        queries.put("Q5d lọc projectId",
                "SELECT * FROM tasks WHERE " + live + " AND project_id = '" + project + "'"
                        + " ORDER BY created_at DESC, id DESC LIMIT 51");
        queries.put("Q6 tiến độ Project",
                "SELECT count(*), sum(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END) FROM tasks WHERE " + live
                        + " AND project_id = '" + project + "'");
        queries.put("Q7 cascade xóa Project",
                "SELECT * FROM tasks WHERE " + live + " AND project_id = '" + project + "' ORDER BY id");
        queries.put("Q7 cascade xóa Category",
                "SELECT * FROM tasks WHERE " + live + " AND category_id = '" + category + "' ORDER BY id");

        StringBuilder report = new StringBuilder("# EXPLAIN — dữ liệu mẫu: user đích " + TARGET_TASKS
                + " Task, " + OTHER_USERS + " user khác × " + TASKS_PER_OTHER_USER + " Task\n");
        Map<String, String> plans = new LinkedHashMap<>();
        for (Map.Entry<String, String> q : queries.entrySet()) {
            String plan = plan(q.getValue());
            plans.put(q.getKey(), plan);
            report.append("\n## ").append(q.getKey()).append("\n").append(q.getValue()).append("\n")
                    .append(plan).append("\n");
        }
        Path out = Path.of("target", "explain-plans.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString(), StandardCharsets.UTF_8);

        List<String> mustUseIndex = List.of("Q1 pull (change_logs theo seq)", "Q6 tiến độ Project",
                "Q7 cascade xóa Project", "Q7 cascade xóa Category");
        List<String> seqScans = new ArrayList<>();
        for (String name : mustUseIndex) {
            if (plans.get(name).contains("Seq Scan")) {
                seqScans.add(name + ":\n" + plans.get(name));
            }
        }
        assertThat(seqScans).as("truy vấn có chỉ mục theo thiết kế không được Seq Scan").isEmpty();
    }

    private void seedTasks(UUID userId, int count, UUID project, UUID category) {
        jdbc.sql("""
                INSERT INTO tasks (id, user_id, title, status, priority, due_date, completed_at, project_id,
                                   category_id, created_at, updated_at)
                SELECT gen_random_uuid(), :u, 'Việc ' || g,
                       CASE WHEN g % 3 = 0 THEN 'DONE' ELSE 'OPEN' END,
                       (ARRAY['NONE','LOW','MEDIUM','HIGH'])[1 + g % 4],
                       CASE WHEN g % 5 = 0 THEN NULL ELSE DATE '2026-11-01' + (g % 60) END,
                       CASE WHEN g % 3 = 0 THEN now() ELSE NULL END,
                       CASE WHEN CAST(:p AS uuid) IS NOT NULL AND g % 10 = 0 THEN CAST(:p AS uuid) ELSE NULL END,
                       CASE WHEN CAST(:c AS uuid) IS NOT NULL AND g % 25 = 0 THEN CAST(:c AS uuid) ELSE NULL END,
                       now() - (g || ' seconds')::interval, now() - (g || ' seconds')::interval
                FROM generate_series(1, :n) g
                """)
                .param("u", userId).param("n", count)
                .param("p", project, java.sql.Types.OTHER).param("c", category, java.sql.Types.OTHER)
                .update();
    }
}
