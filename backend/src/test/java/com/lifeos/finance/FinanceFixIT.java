package com.lifeos.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.outbox.OutboxWorker;
import com.lifeos.finance.service.RecurringRunService;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Đợt sửa backend 1 (BACKEND_FIX_REPORT): F1 — đổi tài khoản khác tiền tệ bị từ chối (D103); F6 — giao dịch định kỳ
 * của tài khoản lưu trữ được TẠM DỪNG kèm thông báo, không tiến nextRunOn khi không sinh giao dịch (D110).
 */
class FinanceFixIT extends FinanceTestBase {

    @Autowired
    RecurringRunService runs;

    @Autowired
    OutboxWorker outboxWorker;

    private UUID recurring(TestUser user, UUID accountId, LocalDate startOn) throws Exception {
        UUID id = UUID.randomUUID();
        expect(post(user, F + "/recurring", body("id", id.toString(), "accountId", accountId.toString(), "kind",
                "EXPENSE", "amount", "100", "frequency", "DAILY", "startOn", startOn.toString())), 201);
        return id;
    }

    private int pausedEvents(UUID recurringId) {
        return outboxEvents("RECURRING_PAUSED", recurringId);
    }

    private List<LocalDate> generated(UUID recurringId) {
        return jdbc.sql("SELECT recurrence_date FROM financial_transactions WHERE recurring_transaction_id = :r"
                + " ORDER BY recurrence_date").param("r", recurringId).query(LocalDate.class).list();
    }

    // ------------------------------------------------------------------------------------------------ F1

    @Test
    void F1_movingATransactionToAnAccountInAnotherCurrencyIsRejectedAndNothingChanges() throws Exception {
        TestUser user = newUser();
        UUID usd = account(user, "USD");
        UUID vnd = account(user, "VND");
        UUID usd2 = account(user, "USD");
        UUID tx = tx(user, usd, "EXPENSE", "100", today(user));
        Map<String, Object> before = row("financial_transactions", tx);
        String usdBalance = balance(user, usd);
        String vndBalance = balance(user, vnd);

        MvcResult rejected = expect(patch(user, F + "/transactions/" + tx, body("version", 1, "accountId",
                vnd.toString())), 422);
        assertThat(errorCode(rejected)).isEqualTo("FINANCE_CURRENCY_MISMATCH");
        MvcResult withAmount = expect(patch(user, F + "/transactions/" + tx, body("version", 1, "accountId",
                vnd.toString(), "amount", "2500000")), 422);
        assertThat(errorCode(withAmount)).as("không tái định giá ngầm kể cả khi gửi kèm amount")
                .isEqualTo("FINANCE_CURRENCY_MISMATCH");

        Map<String, Object> after = row("financial_transactions", tx);
        assertThat(after).as("dữ liệu không đổi").isEqualTo(before);
        assertThat(after.get("currency")).isEqualTo("USD");
        assertThat(changeLogs(user.id(), "financial_transaction", tx, "UPDATE")).isZero();
        assertThat(balance(user, usd)).isEqualTo(usdBalance);
        assertThat(balance(user, vnd)).isEqualTo(vndBalance);

        expect(patch(user, F + "/transactions/" + tx, body("version", 1, "accountId", usd2.toString())), 200);
        assertThat(row("financial_transactions", tx).get("account_id")).as("cùng tiền tệ vẫn chuyển được")
                .isEqualTo(usd2);
    }

    @Test
    void F1_movingARecurringTransactionToAnAccountInAnotherCurrencyIsRejected() throws Exception {
        TestUser user = newUser();
        UUID usd = account(user, "USD");
        UUID vnd = account(user, "VND");
        UUID usd2 = account(user, "USD");
        UUID r = recurring(user, usd, today(user).plusDays(5));
        Map<String, Object> before = row("recurring_transactions", r);

        MvcResult rejected = expect(patch(user, F + "/recurring/" + r, body("version", 1, "accountId",
                vnd.toString())), 422);
        assertThat(errorCode(rejected)).isEqualTo("FINANCE_CURRENCY_MISMATCH");
        assertThat(row("recurring_transactions", r)).as("dữ liệu không đổi").isEqualTo(before);
        assertThat(changeLogs(user.id(), "recurring_transaction", r, "UPDATE")).isZero();

        expect(patch(user, F + "/recurring/" + r, body("version", 1, "accountId", usd2.toString())), 200);
        assertThat(row("recurring_transactions", r).get("currency")).isEqualTo("USD");
    }

    // ------------------------------------------------------------------------------------------------ F6

    @Test
    void F6_archivingAnAccountPausesItsRecurringTransactionsAndNothingIsLostSilently() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        UUID other = account(user, "VND");
        LocalDate start = today(user).plusDays(1);
        UUID r = recurring(user, acc, start);
        UUID untouched = recurring(user, other, start);

        expect(post(user, F + "/accounts/" + acc + "/archive", body("version", 1)), 200);
        Map<String, Object> paused = row("recurring_transactions", r);
        assertThat(paused.get("status")).as("archive tạm dừng định kỳ phụ thuộc").isEqualTo("PAUSED");
        assertThat(((Number) paused.get("version")).longValue()).isEqualTo(2);
        assertThat(changeLogs(user.id(), "recurring_transaction", r, "UPDATE")).isEqualTo(1);
        assertThat(pausedEvents(r)).as("có sự kiện báo người dùng").isEqualTo(1);
        assertThat(row("recurring_transactions", untouched).get("status")).isEqualTo("ACTIVE");

        clock.advance(Duration.ofDays(10));
        // Access token sống 15 phút: cấp token mới cho cùng người dùng sau khi đồng hồ test tiến 10 ngày.
        user = new TestUser(user.id(), jwtService.issue(user.id(), UUID.randomUUID()).token());
        assertThat(runs.catchUp(r)).isZero();
        runs.runOnce();
        assertThat(generated(r)).isEmpty();
        assertThat(row("recurring_transactions", r).get("next_run_on").toString())
                .as("không tiến con trỏ khi không sinh giao dịch").isEqualTo(start.toString());

        long accountVersion = ((Number) row("financial_accounts", acc).get("version")).longValue();
        expect(post(user, F + "/accounts/" + acc + "/unarchive", body("version", accountVersion)), 200);
        assertThat(row("recurring_transactions", r).get("status")).as("mở lại tài khoản không tự chạy lại")
                .isEqualTo("PAUSED");
        Map<String, Object> resumed = readJson(expect(post(user, F + "/recurring/" + r + "/resume",
                body("version", 2)), 200));
        assertThat(resumed).containsEntry("status", "ACTIVE").containsEntry("nextRunOn", today(user).toString());
        assertThat(runs.catchUp(r)).isEqualTo(1);

        // Thông báo tới hộp thư: sự kiện outbox → notification FINANCE (không số tiền, không ghi chú).
        for (int i = 0; i < 50 && pendingPausedEvents(r) > 0; i++) {
            outboxWorker.runOnce();
        }
        List<Map<String, Object>> inbox = items(get(user, "/api/v1/notifications"));
        assertThat(inbox).filteredOn(n -> "finance.recurring.paused".equals(n.get("templateKey"))).hasSize(1);
    }

    @Test
    void F6_theJobPausesAnActiveRecurringWhoseAccountIsNotWritableInsteadOfSkippingOccurrences() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        LocalDate start = today(user).minusDays(3);
        UUID r = recurring(user, acc, start);
        // Trạng thái có thể tồn tại từ trước đợt sửa (hoặc do đua): tài khoản đã lưu trữ, định kỳ còn ACTIVE.
        jdbc.sql("UPDATE financial_accounts SET archived_at = now() WHERE id = :id").param("id", acc).update();

        assertThat(runs.catchUp(r)).isZero();
        Map<String, Object> after = row("recurring_transactions", r);
        assertThat(after.get("status")).isEqualTo("PAUSED");
        assertThat(after.get("next_run_on").toString()).as("không mất lần xuất hiện nào").isEqualTo(start.toString());
        assertThat(generated(r)).isEmpty();
        assertThat(changeLogs(user.id(), "recurring_transaction", r, "UPDATE")).isEqualTo(1);
        assertThat(pausedEvents(r)).isEqualTo(1);

        assertThat(runs.catchUp(r)).isZero();
        runs.runOnce();
        assertThat(pausedEvents(r)).as("chỉ báo một lần").isEqualTo(1);
        assertThat(changeLogs(user.id(), "recurring_transaction", r, "UPDATE")).isEqualTo(1);
    }

    private int pendingPausedEvents(UUID recurringId) {
        return jdbc.sql("SELECT count(*) FROM outbox_events WHERE event_type = 'RECURRING_PAUSED'"
                + " AND aggregate_id = :a AND status IN ('PENDING', 'PROCESSING')").param("a", recurringId)
                .query(Integer.class).single();
    }
}
