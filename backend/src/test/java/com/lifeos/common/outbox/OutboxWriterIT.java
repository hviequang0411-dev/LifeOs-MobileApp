package com.lifeos.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lifeos.support.IntegrationTestBase;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/** OutboxWriter ghi cùng transaction nghiệp vụ (JAVA_SPRING_RULES §3.3, I7). */
class OutboxWriterIT extends IntegrationTestBase {

    @Autowired
    OutboxWriter outboxWriter;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcClient jdbc;

    private Map<String, Object> row(UUID id) {
        return jdbc.sql("SELECT event_type, aggregate_type, aggregate_id, payload::text AS payload, status, attempts,"
                + " next_attempt_at, locked_until FROM outbox_events WHERE id = :id")
                .param("id", id).query().singleRow();
    }

    private int count(UUID aggregateId) {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE aggregate_id = :a").param("a", aggregateId)
                .query(Integer.class).single();
    }

    @Test
    void writesPendingEventInsideTransaction() {
        UUID aggregate = UUID.randomUUID();
        UUID id = tx.execute(status -> outboxWriter.write("EMAIL_VERIFICATION", "user", aggregate,
                Map.of("email", "a@example.test", "token", "test-token-value")));

        Map<String, Object> row = row(id);
        assertThat(row.get("event_type")).isEqualTo("EMAIL_VERIFICATION");
        assertThat(row.get("aggregate_type")).isEqualTo("user");
        assertThat(row.get("aggregate_id")).isEqualTo(aggregate);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat(row.get("attempts")).isEqualTo(0);
        assertThat(row.get("next_attempt_at")).isNotNull();
        assertThat(row.get("locked_until")).isNull();
        assertThat((String) row.get("payload")).contains("\"email\"").contains("\"token\"");
    }

    @Test
    void rollbackRemovesTheEvent() {
        UUID aggregate = UUID.randomUUID();
        tx.executeWithoutResult(status -> {
            outboxWriter.write("PASSWORD_RESET", "user", aggregate, Map.of("k", "v"));
            status.setRollbackOnly();
        });
        assertThat(count(aggregate)).isZero();
    }

    @Test
    void refusesToWriteOutsideATransaction() {
        UUID aggregate = UUID.randomUUID();
        assertThatThrownBy(() -> outboxWriter.write("PASSWORD_RESET", "user", aggregate, Map.of()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(count(aggregate)).isZero();
    }
}
