package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.outbox.OutboxWriter;
import com.lifeos.common.privacy.UserDataPurger;
import com.lifeos.identity.service.DataExportService;
import com.lifeos.support.ApiTestBase;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đợt sửa backend 1 (BACKEND_FIX_REPORT): F5 — giới hạn xuất dữ liệu (1 yêu cầu đang chạy, 2 lần/24 giờ) là giới hạn
 * cứng, đúng cả khi gọi đồng thời (D204); F8 — xóa tài khoản xóa cả sự kiện outbox còn mang userId (D205).
 */
class ExportPurgeFixIT extends ApiTestBase {

    private static final int THREADS = 6;

    @Autowired
    DataExportService exports;

    @Autowired
    UserDataPurger purger;

    @Autowired
    OutboxWriter outbox;

    @Autowired
    TransactionTemplate tx;

    private record Answer(int status, String id) {
    }

    /** THREADS request POST /account/export cùng lúc (barrier), trả mã trạng thái và id từng phản hồi. */
    private List<Answer> requestConcurrently(TestUser user) throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(THREADS);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<Answer>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    barrier.await(30, TimeUnit.SECONDS);
                    MvcResult r = post(user, "/api/v1/account/export", null);
                    // 503 = hết lock_timeout 5 giây khi chờ khóa dòng users (D87: lỗi tạm thời kèm Retry-After, client
                    // thử lại). Hiếm gặp khi DB của máy test khựng I/O; thử lại như client để khẳng định bên dưới vẫn
                    // chặt (đúng một 202, không dòng thừa) thay vì đỏ vì môi trường.
                    for (int retry = 0; retry < 3 && r.getResponse().getStatus() == 503; retry++) {
                        r = post(user, "/api/v1/account/export", null);
                    }
                    int status = r.getResponse().getStatus();
                    return new Answer(status, status < 300 ? (String) readJson(r).get("id") : null);
                }));
            }
            List<Answer> answers = new ArrayList<>();
            for (Future<Answer> f : futures) {
                answers.add(f.get(60, TimeUnit.SECONDS));
            }
            return answers;
        } finally {
            pool.shutdownNow();
        }
    }

    private int exportRows(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM data_exports WHERE user_id = :u").param("u", userId)
                .query(Integer.class).single();
    }

    private void finishActive(UUID userId) {
        jdbc.sql("UPDATE data_exports SET status = 'FAILED', error_code = 'TEST', finished_at = now()"
                + " WHERE user_id = :u AND status IN ('PENDING', 'RUNNING')").param("u", userId).update();
    }

    // ------------------------------------------------------------------------------------------------ F5

    @RepeatedTest(20)
    void F5_concurrentExportRequestsNeverExceedOneActiveAndTwoPerDay() throws Exception {
        TestUser user = newUser();

        List<Answer> first = requestConcurrently(user);
        assertThat(first).extracting(Answer::status).as("đúng một yêu cầu được tạo, còn lại trả yêu cầu đang chờ")
                .containsExactlyInAnyOrder(202, 200, 200, 200, 200, 200);
        Set<String> ids = new HashSet<>();
        first.forEach(a -> ids.add(a.id()));
        assertThat(ids).hasSize(1);
        assertThat(exportRows(user.id())).isEqualTo(1);

        finishActive(user.id());
        List<Answer> second = requestConcurrently(user);
        assertThat(second).extracting(Answer::status).containsExactlyInAnyOrder(202, 200, 200, 200, 200, 200);
        assertThat(exportRows(user.id())).as("lần thứ hai trong 24 giờ").isEqualTo(2);

        finishActive(user.id());
        List<Answer> third = requestConcurrently(user);
        assertThat(third).extracting(Answer::status).as("2 lần/24 giờ là giới hạn cứng").containsOnly(429);
        assertThat(exportRows(user.id())).isEqualTo(2);
    }

    @Test
    void F5_theDatabaseAllowsAtMostOneActiveExportPerUser() {
        TestUser user = newUser();
        jdbc.sql("INSERT INTO data_exports (id, user_id, status, requested_at) VALUES (:id, :u, 'RUNNING', now())")
                .param("id", UUID.randomUUID()).param("u", user.id()).update();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.sql("INSERT INTO data_exports (id, user_id,"
                        + " status, requested_at) VALUES (:id, :u, 'PENDING', now())")
                .param("id", UUID.randomUUID()).param("u", user.id()).update())
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        jdbc.sql("INSERT INTO data_exports (id, user_id, status, requested_at) VALUES (:id, :u, 'READY', now())")
                .param("id", UUID.randomUUID()).param("u", user.id()).update();
    }

    // ------------------------------------------------------------------------------------------------ F8

    private Map<String, Object> payload(UUID userId) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("userId", userId.toString());
        p.put("category", "FINANCE");
        p.put("templateKey", "finance.budget.threshold.80");
        p.put("variables", Map.of("budgetId", UUID.randomUUID().toString(), "month", "2026-11", "threshold", 80));
        return p;
    }

    private int eventsCarrying(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE payload->>'userId' = :u OR aggregate_id = :id")
                .param("u", userId.toString()).param("id", userId).query(Integer.class).single();
    }

    @Test
    void F8_purgingAUserRemovesPendingOutboxEventsThatCarryTheUserId() {
        TestUser user = newUser();
        TestUser bystander = newUser();
        // Worker outbox dừng (job tắt trong test): sự kiện còn PENDING khi tài khoản bị xóa cứng.
        UUID budgetEvent = tx.execute(s -> outbox.write("BUDGET_THRESHOLD", "budget", UUID.randomUUID(),
                payload(user.id())));
        tx.execute(s -> outbox.write("SYSTEM_NOTIFICATION", "user", user.id(), Map.of("userId",
                user.id().toString(), "category", "SYSTEM", "templateKey", "export.ready")));
        UUID kept = tx.execute(s -> outbox.write("BUDGET_THRESHOLD", "budget", UUID.randomUUID(),
                payload(bystander.id())));
        assertThat(eventsCarrying(user.id())).isEqualTo(2);

        purger.purge(user.id());

        assertThat(eventsCarrying(user.id())).as("không còn sự kiện outbox mang userId đã xóa").isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE id = :id").param("id", budgetEvent)
                .query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE id = :id").param("id", kept)
                .query(Integer.class).single()).as("sự kiện của người khác còn nguyên").isEqualTo(1);
    }

    @Test
    void F8_outboxWriterRecordsTheOwningUserInItsOwnColumn() {
        TestUser user = newUser();
        UUID fromPayload = tx.execute(s -> outbox.write("BUDGET_THRESHOLD", "budget", UUID.randomUUID(),
                payload(user.id())));
        UUID fromAggregate = tx.execute(s -> outbox.write("PASSWORD_CHANGED", "user", user.id(), Map.of()));
        UUID anonymous = tx.execute(s -> outbox.write("PASSWORD_CHANGED", null, null, Map.of("userId", "không-phải-uuid")));
        assertThat(ownerOf(fromPayload)).isEqualTo(user.id());
        assertThat(ownerOf(fromAggregate)).isEqualTo(user.id());
        assertThat(ownerOf(anonymous)).isNull();
    }

    private UUID ownerOf(UUID eventId) {
        return jdbc.sql("SELECT user_id FROM outbox_events WHERE id = :id").param("id", eventId)
                .query((rs, n) -> rs.getObject(1, UUID.class)).list().get(0);
    }
}
