package com.lifeos.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.finance.domain.Recurrence;
import com.lifeos.finance.service.RecurringRunService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Giao dịch định kỳ và job sinh giao dịch (PHASE_3 §7, FIN-09..11). Gọi RecurringRunService trực tiếp (job tắt). */
class RecurringIT extends FinanceTestBase {

    @Autowired
    RecurringRunService runs;

    UUID recurring(TestUser user, UUID accountId, String frequency, LocalDate startOn, Object... extra)
            throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> body = map("id", id.toString(), "accountId", accountId.toString(), "kind", "EXPENSE",
                "amount", "100000", "frequency", frequency, "startOn", startOn.toString(), "note", "Tiền nhà");
        put(body, extra);
        expect(post(user, F + "/recurring", json.writeValueAsString(body)), 201);
        return id;
    }

    List<LocalDate> generated(UUID recurringId) {
        return jdbc.sql("SELECT recurrence_date FROM financial_transactions WHERE recurring_transaction_id = :r"
                + " ORDER BY recurrence_date").param("r", recurringId).query(LocalDate.class).list();
    }

    LocalDate nextRunOn(UUID recurringId) {
        return jdbc.sql("SELECT next_run_on FROM recurring_transactions WHERE id = :r").param("r", recurringId)
                .query(LocalDate.class).single();
    }

    @Test
    void Fin_09_runningTheJobTwiceGeneratesEachOccurrenceOnce() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        LocalDate today = today(user);
        UUID r = recurring(user, acc, "DAILY", today.minusDays(4));
        assertThat(runs.catchUp(r)).isEqualTo(5);
        assertThat(runs.catchUp(r)).isZero();
        assertThat(generated(r)).hasSize(5).doesNotHaveDuplicates().last().isEqualTo(today);
        assertThat(nextRunOn(r)).isEqualTo(today.plusDays(1));
        Map<String, Object> tx = jdbc.sql("SELECT * FROM financial_transactions WHERE recurring_transaction_id = :r"
                + " AND recurrence_date = :d").param("r", r).param("d", today).query().singleRow();
        assertThat(tx.get("occurred_on").toString()).isEqualTo(today.toString());
        assertThat(tx.get("currency")).isEqualTo("VND");
        assertThat(changeLogs(user.id(), "financial_transaction", (UUID) tx.get("id"), "CREATE")).isEqualTo(1);
        assertThat(changeLogs(user.id(), "recurring_transaction", r, "UPDATE")).isEqualTo(5);
        assertThat(balance(user, acc)).isEqualTo("-500000");
        assertThat(runs.runOnce()).isGreaterThanOrEqualTo(0);
        assertThat(generated(r)).hasSize(5);
    }

    @RepeatedTest(20)
    void Fin_09_parallelRunsNeverDuplicateAnOccurrence() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        LocalDate today = today(user);
        UUID r = recurring(user, acc, "DAILY", today.minusDays(9));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                results.add(pool.submit(() -> {
                    go.await();
                    return runs.catchUp(r);
                }));
            }
            go.countDown();
            int total = 0;
            for (Future<Integer> f : results) {
                total += f.get(60, TimeUnit.SECONDS);
            }
            // SKIP LOCKED: luồng thua bỏ qua, nên có thể còn lại lần xuất hiện chưa sinh — chạy bù lần nữa.
            total += runs.catchUp(r);
            assertThat(total).isEqualTo(10);
        } finally {
            pool.shutdownNow();
        }
        assertThat(generated(r)).hasSize(10).doesNotHaveDuplicates();
        assertThat(nextRunOn(r)).isEqualTo(today.plusDays(1));
    }

    @Test
    void Fin_10_monthEndRuleCatchUpCapPauseResumeAndUserTimezone() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        LocalDate today = today(user);
        LocalDate lastJan31 = LocalDate.of(today.getYear() - 1, 1, 31);
        UUID monthly = recurring(user, acc, "MONTHLY", lastJan31);
        runs.catchUp(monthly);
        Recurrence rule = new Recurrence("MONTHLY", 1, lastJan31);
        List<LocalDate> expected = new ArrayList<>();
        for (int k = 0; !rule.occurrence(k).isAfter(today); k++) {
            expected.add(rule.occurrence(k));
        }
        assertThat(generated(monthly)).isEqualTo(expected);
        assertThat(generated(monthly).get(1).getDayOfMonth()).isIn(28, 29);
        assertThat(generated(monthly).get(2).getDayOfMonth()).isEqualTo(31);

        UUID daily = recurring(user, acc, "DAILY", today.minusDays(39));
        assertThat(runs.catchUp(daily)).isEqualTo(RecurringRunService.MAX_CATCH_UP);
        assertThat(runs.catchUp(daily)).isEqualTo(9);

        UUID paused = recurring(user, acc, "DAILY", today.minusDays(2));
        expect(post(user, F + "/recurring/" + paused + "/pause", body("version", 1)), 200);
        assertThat(runs.catchUp(paused)).isZero();
        Map<String, Object> resumed = readJson(expect(post(user, F + "/recurring/" + paused + "/resume",
                body("version", 2)), 200));
        assertThat(resumed.get("nextRunOn")).as("không bù lần bị lỡ khi tạm dừng").isEqualTo(today.toString());
        assertThat(runs.catchUp(paused)).isEqualTo(1);

        UUID ended = recurring(user, acc, "DAILY", today.minusDays(5), "endOn", today.minusDays(4).toString());
        assertThat(runs.catchUp(ended)).isEqualTo(2);

        for (String zone : List.of("Pacific/Kiritimati", "Pacific/Pago_Pago")) {
            TestUser z = newUser();
            timezone(z, zone);
            UUID zAcc = account(z, "USD");
            LocalDate zToday = today(z);
            UUID due = recurring(z, zAcc, "WEEKLY", zToday);
            UUID tomorrow = recurring(z, zAcc, "WEEKLY", zToday.plusDays(1));
            assertThat(runs.catchUp(due)).as(zone).isEqualTo(1);
            assertThat(runs.catchUp(tomorrow)).as(zone + ": chưa tới ngày theo múi giờ người dùng").isZero();
            assertThat(nextRunOn(due)).isEqualTo(zToday.plusWeeks(1));
        }

        UUID archivedAcc = account(user, "VND");
        UUID onArchived = recurring(user, archivedAcc, "DAILY", today.minusDays(1));
        expect(post(user, F + "/accounts/" + archivedAcc + "/archive", body("version", 1)), 200);
        assertThat(runs.catchUp(onArchived)).isZero();
        // Đợt sửa F6 (người dùng bác hành vi "bỏ lặng, chỉ tiến nextRunOn"): lưu trữ tài khoản tạm dừng định kỳ và
        // con trỏ KHÔNG tiến khi không sinh giao dịch. Chi tiết ở FinanceFixIT.
        assertThat(nextRunOn(onArchived)).isEqualTo(today.minusDays(1));
        assertThat(row("recurring_transactions", onArchived).get("status")).isEqualTo("PAUSED");
    }

    @Test
    void Fin_11_deletingAGeneratedTransactionDoesNotRegenerateIt() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        LocalDate today = today(user);
        UUID r = recurring(user, acc, "DAILY", today.minusDays(1));
        runs.catchUp(r);
        UUID first = jdbc.sql("SELECT id FROM financial_transactions WHERE recurring_transaction_id = :r"
                + " ORDER BY recurrence_date LIMIT 1").param("r", r).query(UUID.class).single();
        expect(delete(user, F + "/transactions/" + first + "?version=1"), 204);
        jdbc.sql("UPDATE recurring_transactions SET next_run_on = :d WHERE id = :r").param("d", today.minusDays(1))
                .param("r", r).update();
        assertThat(runs.catchUp(r)).isZero();
        assertThat(generated(r)).hasSize(2);
        assertThat(jdbc.sql("SELECT count(*) FROM financial_transactions WHERE recurring_transaction_id = :r"
                + " AND deleted_at IS NULL").param("r", r).query(Integer.class).single()).isEqualTo(1);
        assertThat(nextRunOn(r)).isEqualTo(today.plusDays(1));

        long version = version(readJson(get(user, F + "/recurring/" + r)));
        expect(patch(user, F + "/recurring/" + r, body("version", version, "amount", "200000")), 200);
        expect(delete(user, F + "/recurring/" + r + "?version=" + (version + 1)), 204);
        assertThat(generated(r)).as("xóa định kỳ giữ giao dịch đã sinh").hasSize(2);
    }
}
