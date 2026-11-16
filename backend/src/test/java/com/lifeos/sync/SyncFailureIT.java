package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SYN-46 (SYNC_PROTOCOL §5.1, D85): lỗi KHÔNG dự kiến khi xử lý mutation → 500 INTERNAL_ERROR cho cả request, không lộ
 * chi tiết, KHÔNG lưu kết quả (không biến thành REJECTED); mutation đã APPLIED trước đó trong lô vẫn giữ. Lỗi được
 * inject qua đường thật bằng một trigger PostgreSQL chỉ nổ với đúng entityId của test (không mock, không handler giả),
 * trigger bị gỡ trong finally.
 */
class SyncFailureIT extends SyncTestBase {

    @Test
    void Sync_46_unexpectedErrorIs500StoresNothingAndTheResentMutationIsApplied() throws Exception {
        TestUser user = newUser();
        UUID poisoned = UUID.randomUUID();
        String suffix = poisoned.toString().replace("-", "");
        String function = "sync46_fail_" + suffix;
        String trigger = "sync46_trg_" + suffix;
        Map<String, Object> before = mutation("category", UUID.randomUUID(), "CREATE", 0, payload("name", "Trước"));
        Map<String, Object> failing = mutation("project", poisoned, "CREATE", 0, payload("name", "Nổ"));
        Map<String, Object> after = mutation("category", UUID.randomUUID(), "CREATE", 0, payload("name", "Sau"));

        jdbc.sql("CREATE FUNCTION " + function + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
                + "IF NEW.id = '" + poisoned + "' THEN RAISE EXCEPTION 'inject-sync46'; END IF; RETURN NEW; END $$")
                .update();
        jdbc.sql("CREATE TRIGGER " + trigger + " BEFORE INSERT ON projects FOR EACH ROW EXECUTE FUNCTION "
                + function + "()").update();
        MvcResult result;
        try {
            result = pushResult(user, before, failing, after);
        } finally {
            jdbc.sql("DROP TRIGGER IF EXISTS " + trigger + " ON projects").update();
            jdbc.sql("DROP FUNCTION IF EXISTS " + function + "()").update();
        }

        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"errorCode\":\"INTERNAL_ERROR\"").contains("correlationId")
                .doesNotContain("inject-sync46").doesNotContain("projects").doesNotContain("SQL");
        assertThat(syncMutations(user.id(), mutationIdOf(before))).as("mutation trước đã commit").isEqualTo(1);
        assertThat(syncMutations(user.id(), mutationIdOf(failing))).as("không lưu kết quả lỗi server").isZero();
        assertThat(syncMutations(user.id(), mutationIdOf(after))).as("dừng cả request").isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM projects WHERE id = :id").param("id", poisoned)
                .query(Integer.class).single()).isZero();

        assertThat(push(user, before, failing, after)).extracting(r -> r.get("status"))
                .containsExactly("DUPLICATE", "APPLIED", "APPLIED");
    }
}
