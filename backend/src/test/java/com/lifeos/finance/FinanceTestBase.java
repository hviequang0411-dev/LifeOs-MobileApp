package com.lifeos.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.web.servlet.MvcResult;

/** Hỗ trợ test Finance: tạo tài khoản/danh mục/giao dịch qua REST, đọc DB, hôm nay theo múi giờ người dùng. */
abstract class FinanceTestBase extends ApiTestBase {

    static final String F = "/api/v1/finance";

    UUID account(TestUser user, String currency, Object... extra) throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> body = map("id", id.toString(), "name", "TK " + id.toString().substring(0, 8),
                "accountType", "BANK", "currency", currency);
        put(body, extra);
        expect(post(user, F + "/accounts", json.writeValueAsString(body)), 201);
        return id;
    }

    UUID category(TestUser user, String kind) throws Exception {
        UUID id = UUID.randomUUID();
        expect(post(user, F + "/categories", body("id", id.toString(), "name", "DM " + id.toString().substring(0, 8),
                "kind", kind)), 201);
        return id;
    }

    MvcResult createTx(TestUser user, UUID id, UUID accountId, String kind, String amount, LocalDate day,
            Object... extra) throws Exception {
        Map<String, Object> body = map("id", id.toString(), "accountId", accountId.toString(), "kind", kind,
                "amount", amount, "occurredOn", day.toString());
        put(body, extra);
        return post(user, F + "/transactions", json.writeValueAsString(body));
    }

    UUID tx(TestUser user, UUID accountId, String kind, String amount, LocalDate day, Object... extra)
            throws Exception {
        UUID id = UUID.randomUUID();
        expect(createTx(user, id, accountId, kind, amount, day, extra), 201);
        return id;
    }

    UUID transfer(TestUser user, UUID from, UUID to, String amount, LocalDate day) throws Exception {
        UUID id = UUID.randomUUID();
        expect(post(user, F + "/transfers", body("id", id.toString(), "fromAccountId", from.toString(),
                "toAccountId", to.toString(), "amount", amount, "occurredOn", day.toString())), 201);
        return id;
    }

    UUID budget(TestUser user, UUID categoryIdOrNull, String currency, String amount) throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> body = map("id", id.toString(), "currency", currency, "amount", amount);
        if (categoryIdOrNull != null) {
            body.put("categoryId", categoryIdOrNull.toString());
        }
        expect(post(user, F + "/budgets", json.writeValueAsString(body)), 201);
        return id;
    }

    String balance(TestUser user, UUID accountId) throws Exception {
        return (String) readJson(expect(get(user, F + "/accounts/" + accountId + "/balance"), 200)).get("balance");
    }

    MvcResult expect(MvcResult result, int status) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(status);
        return result;
    }

    String errorCode(MvcResult result) throws Exception {
        return (String) readJson(result).get("errorCode");
    }

    @SuppressWarnings("unchecked")
    String fieldErrorCode(MvcResult result, String field) throws Exception {
        java.util.List<Map<String, Object>> errors = (java.util.List<Map<String, Object>>) readJson(result)
                .get("fieldErrors");
        return errors == null ? null : errors.stream().filter(e -> field.equals(e.get("field")))
                .map(e -> (String) e.get("code")).findFirst().orElse(null);
    }

    void timezone(TestUser user, String zone) {
        jdbc.sql("INSERT INTO user_settings (id, user_id, timezone) VALUES (:id, :u, :z)"
                        + " ON CONFLICT (user_id) DO UPDATE SET timezone = EXCLUDED.timezone")
                .param("id", UUID.randomUUID()).param("u", user.id()).param("z", zone).update();
    }

    LocalDate today(TestUser user) {
        String zone = jdbc.sql("SELECT coalesce((SELECT timezone FROM user_settings WHERE user_id = :u),"
                + " 'Asia/Ho_Chi_Minh')").param("u", user.id()).query(String.class).single();
        return LocalDate.ofInstant(clock.instant(), ZoneId.of(zone));
    }

    Map<String, Object> row(String table, UUID id) {
        return jdbc.sql("SELECT * FROM " + table + " WHERE id = :id").param("id", id).query().singleRow();
    }

    int outboxEvents(String eventType, UUID aggregateId) {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE event_type = :t AND aggregate_id = :a")
                .param("t", eventType).param("a", aggregateId).query(Integer.class).single();
    }

    static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> m = new LinkedHashMap<>();
        put(m, keyValues);
        return m;
    }

    static void put(Map<String, Object> m, Object... keyValues) {
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put((String) keyValues[i], keyValues[i + 1]);
        }
    }
}
