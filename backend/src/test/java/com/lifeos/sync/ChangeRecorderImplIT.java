package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.common.sync.ChangeOperation;
import com.lifeos.common.sync.ChangeRecorder;
import com.lifeos.support.IntegrationTestBase;
import com.lifeos.support.TestUsers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * ChangeRecorderImpl trên PostgreSQL thật (SYSTEM_ARCHITECTURE §3.6, I1/I2; TESTING_RULES §8.6; DATABASE_DESIGN §12):
 * tạo lười user_sync_state, seq liên tục, rollback không để lỗ, bắt buộc có transaction, đồng thời không trùng seq.
 */
class ChangeRecorderImplIT extends IntegrationTestBase {

    @Autowired
    ChangeRecorder changeRecorder;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcClient jdbc;

    private long record(UUID userId, UUID entityId, ChangeOperation op, long version, UUID mutationId) {
        return tx.execute(status -> changeRecorder.record(userId, "task", entityId, op, version, mutationId));
    }

    private Long lastSeq(UUID userId) {
        return jdbc.sql("SELECT last_seq FROM user_sync_state WHERE user_id = :u").param("u", userId)
                .query(Long.class).optional().orElse(null);
    }

    private List<Long> seqs(UUID userId) {
        return jdbc.sql("SELECT seq FROM change_logs WHERE user_id = :u ORDER BY seq").param("u", userId)
                .query(Long.class).list();
    }

    @Test
    void createsUserSyncStateLazilyOnFirstChange() {
        UUID user = TestUsers.insert(jdbc);
        assertThat(lastSeq(user)).isNull();

        long seq = record(user, UUID.randomUUID(), ChangeOperation.CREATE, 1, null);

        assertThat(seq).isEqualTo(1);
        assertThat(lastSeq(user)).isEqualTo(1);
    }

    @Test
    void writesChangeLogRowWithExactValues() {
        UUID user = TestUsers.insert(jdbc);
        UUID entity = UUID.randomUUID();
        UUID mutation = UUID.randomUUID();

        long seq = record(user, entity, ChangeOperation.UPDATE, 3, mutation);

        Map<String, Object> row = jdbc.sql("SELECT * FROM change_logs WHERE user_id = :u AND seq = :s")
                .param("u", user).param("s", seq).query().singleRow();
        assertThat(row.get("entity_type")).isEqualTo("task");
        assertThat(row.get("entity_id")).isEqualTo(entity);
        assertThat(row.get("operation")).isEqualTo("UPDATE");
        assertThat(row.get("entity_version")).isEqualTo(3L);
        assertThat(row.get("mutation_id")).isEqualTo(mutation);
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    void mutationIdIsNullForRestOrCascadeChanges() {
        UUID user = TestUsers.insert(jdbc);
        long seq = record(user, UUID.randomUUID(), ChangeOperation.CREATE, 1, null);
        Boolean mutationIdIsNull = jdbc.sql(
                        "SELECT mutation_id IS NULL FROM change_logs WHERE user_id = :u AND seq = :s")
                .param("u", user).param("s", seq).query(Boolean.class).single();
        assertThat(mutationIdIsNull).isTrue();
    }

    @Test
    void seqIsContinuousAcrossTransactionsAndWithinOne() {
        UUID user = TestUsers.insert(jdbc);
        record(user, UUID.randomUUID(), ChangeOperation.CREATE, 1, null);
        record(user, UUID.randomUUID(), ChangeOperation.CREATE, 1, null);
        // Một use case đổi nhiều entity (cascade) gọi record nhiều lần trong cùng transaction.
        tx.executeWithoutResult(status -> {
            changeRecorder.record(user, "task", UUID.randomUUID(), ChangeOperation.UPDATE, 2, null);
            changeRecorder.record(user, "project", UUID.randomUUID(), ChangeOperation.DELETE, 2, null);
        });

        assertThat(seqs(user)).containsExactly(1L, 2L, 3L, 4L);
        assertThat(lastSeq(user)).isEqualTo(4);
    }

    @Test
    void rollbackLeavesNoGapAndNoChangeLog() {
        UUID user = TestUsers.insert(jdbc);
        record(user, UUID.randomUUID(), ChangeOperation.CREATE, 1, null);

        tx.executeWithoutResult(status -> {
            changeRecorder.record(user, "task", UUID.randomUUID(), ChangeOperation.CREATE, 1, null);
            status.setRollbackOnly();
        });
        record(user, UUID.randomUUID(), ChangeOperation.CREATE, 1, null);

        assertThat(seqs(user)).containsExactly(1L, 2L);
        assertThat(lastSeq(user)).isEqualTo(2);
    }

    @Test
    void refusesToRunOutsideATransaction() {
        UUID user = TestUsers.insert(jdbc);
        assertThatThrownBy(() -> changeRecorder.record(user, "task", UUID.randomUUID(), ChangeOperation.CREATE, 1,
                null)).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(lastSeq(user)).isNull();
        assertThat(seqs(user)).isEmpty();
    }

    @Test
    void rejectsInvalidVersion() {
        UUID user = TestUsers.insert(jdbc);
        assertThatThrownBy(() -> record(user, UUID.randomUUID(), ChangeOperation.CREATE, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentWritersOfSameUserGetDistinctContinuousSeqs() throws Exception {
        UUID user = TestUsers.insert(jdbc);
        int threads = 8;
        int perThread = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<List<Long>>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                Callable<List<Long>> task = () -> {
                    start.await();
                    List<Long> mine = new ArrayList<>();
                    for (int i = 0; i < perThread; i++) {
                        mine.add(record(user, UUID.randomUUID(), ChangeOperation.CREATE, 1, null));
                    }
                    return mine;
                };
                futures.add(pool.submit(task));
            }
            start.countDown();
            List<Long> all = new ArrayList<>();
            for (Future<List<Long>> f : futures) {
                all.addAll(f.get(30, TimeUnit.SECONDS));
            }
            int total = threads * perThread;
            assertThat(all).doesNotHaveDuplicates().hasSize(total);
            assertThat(seqs(user)).hasSize(total).first().isEqualTo(1L);
            assertThat(seqs(user)).last().isEqualTo((long) total);
            assertThat(lastSeq(user)).isEqualTo(total);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void differentUsersHaveIndependentCounters() {
        UUID a = TestUsers.insert(jdbc);
        UUID b = TestUsers.insert(jdbc);
        record(a, UUID.randomUUID(), ChangeOperation.CREATE, 1, null);
        record(a, UUID.randomUUID(), ChangeOperation.CREATE, 1, null);
        long firstOfB = record(b, UUID.randomUUID(), ChangeOperation.CREATE, 1, null);
        assertThat(firstOfB).isEqualTo(1);
    }
}
