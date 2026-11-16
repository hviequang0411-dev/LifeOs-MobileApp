package com.lifeos.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.lifeos.identity.service.IdentityOutboxEvents;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Nhiều worker chạy song song (JAVA_SPRING_RULES §10.4: FOR UPDATE SKIP LOCKED để chạy nhiều instance; TESTING_RULES
 * §11 "SKIP LOCKED không xử lý trùng với 2 worker"). Hai worker thật khởi động cùng lúc bằng CountDownLatch, timeout
 * cứng, lặp 20 lần; không Thread.sleep.
 */
class OutboxConcurrencyIT extends OutboxTestSupport {

    private static final int EVENTS = 12;

    @Autowired
    DataSource dataSource;

    private ExecutorService pool;

    @BeforeEach
    void startPool() {
        pool = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void stopPool() {
        pool.shutdownNow();
    }

    private UUID passwordChanged(String email) {
        return write(IdentityOutboxEvents.PASSWORD_CHANGED, Map.of("userId", UUID.randomUUID().toString(),
                "email", email, "displayName", "An", "changedAt", clock.instant().toString()));
    }

    private int unfinished(List<UUID> ids) {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE id IN (:ids) AND status <> 'SENT'")
                .param("ids", ids).query(Integer.class).single();
    }

    @RepeatedTest(20)
    void twoConcurrentWorkersDeliverEveryEventExactlyOnce() throws Exception {
        List<String> emails = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < EVENTS; i++) {
            String email = newEmail();
            emails.add(email);
            ids.add(passwordChanged(email));
        }
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> workers = new ArrayList<>();
        for (int w = 0; w < 2; w++) {
            workers.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                int claimed = 0;
                for (int round = 0; round < 200 && unfinished(ids) > 0; round++) {
                    claimed += worker.runOnce();
                }
                return claimed;
            }));
        }
        assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
        go.countDown();
        for (Future<Integer> future : workers) {
            future.get(60, TimeUnit.SECONDS);
        }

        assertThat(unfinished(ids)).isZero();
        for (String email : emails) {
            assertThat(mail.sentTo(email)).as("gửi đúng một lần").hasSize(1);
        }
        assertThat(jdbc.sql("SELECT count(*) FROM outbox_events WHERE id IN (:ids) AND attempts = 0"
                + " AND payload = '{}'::jsonb").param("ids", ids).query(Integer.class).single()).isEqualTo(EVENTS);
    }

    @Test
    void aRowLockedByAnotherTransactionIsSkippedNotWaitedFor() throws Exception {
        drain();
        String lockedEmail = newEmail();
        String freeEmail = newEmail();
        UUID locked = passwordChanged(lockedEmail);
        UUID free = passwordChanged(freeEmail);
        try (Connection other = dataSource.getConnection()) {
            other.setAutoCommit(false);
            try (PreparedStatement ps = other.prepareStatement("SELECT 1 FROM outbox_events WHERE id = ? FOR UPDATE")) {
                ps.setObject(1, locked);
                ps.executeQuery();
            }
            Future<Integer> run = pool.submit(worker::runOnce);
            await().atMost(Duration.ofSeconds(4)).until(run::isDone);
            assertThat(run.get()).isEqualTo(1);
            assertThat(row(free)).containsEntry("status", "SENT");
            assertThat(row(locked)).as("bị khóa thì bỏ qua, không chờ").containsEntry("status", "PENDING");
            other.rollback();
        }
        drain();
        assertThat(row(locked)).containsEntry("status", "SENT");
        assertThat(mail.sentTo(lockedEmail)).hasSize(1);
    }
}
