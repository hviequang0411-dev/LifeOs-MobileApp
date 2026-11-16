package com.lifeos.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Giao dịch, ngân sách, ngưỡng, mục tiêu, báo cáo, tìm kiếm (PHASE_3 FIN-04, FIN-12..14, FIN-16, FIN-17). */
class FinanceTransactionIT extends FinanceTestBase {

    @Test
    void Fin_04_transfersNeverAppearInIncomeOrExpenseReports() throws Exception {
        TestUser user = newUser();
        UUID a = account(user, "VND");
        UUID b = account(user, "VND");
        LocalDate day = today(user);
        tx(user, a, "INCOME", "1000000", day);
        tx(user, a, "EXPENSE", "300000", day);
        String month = YearMonth.from(day).toString();
        Map<String, Object> before = readJson(expect(get(user, F + "/reports/monthly?month=" + month
                + "&currency=VND"), 200));
        transfer(user, a, b, "500000", day);
        Map<String, Object> after = readJson(expect(get(user, F + "/reports/monthly?month=" + month
                + "&currency=VND"), 200));
        assertThat(after).isEqualTo(before);
        assertThat(after).containsEntry("income", "1000000").containsEntry("expense", "300000")
                .containsEntry("net", "700000");
    }

    @Test
    void Fin_12_budgetUsageFollowsTheCalendarMonthCurrencyAndCategory() throws Exception {
        TestUser user = newUser();
        timezone(user, "Pacific/Kiritimati");
        UUID vnd = account(user, "VND");
        UUID vnd2 = account(user, "VND");
        UUID usd = account(user, "USD");
        UUID food = category(user, "EXPENSE");
        UUID other = category(user, "EXPENSE");
        YearMonth month = YearMonth.from(today(user));
        LocalDate first = month.atDay(1);
        UUID total = budget(user, null, "VND", "1000000");
        UUID foodBudget = budget(user, food, "VND", "200000");
        tx(user, vnd, "EXPENSE", "100000", first, "categoryId", food.toString());
        tx(user, vnd2, "EXPENSE", "50000", month.atEndOfMonth(), "categoryId", other.toString());
        tx(user, vnd, "EXPENSE", "999999", first.minusDays(1), "categoryId", food.toString());
        tx(user, vnd, "EXPENSE", "7777", month.plusMonths(1).atDay(1));
        tx(user, usd, "EXPENSE", "12.5", first);
        tx(user, vnd, "INCOME", "5000000", first);
        transfer(user, vnd, vnd2, "300000", first);
        UUID deleted = tx(user, vnd, "EXPENSE", "400000", first);
        expect(delete(user, F + "/transactions/" + deleted + "?version=1"), 204);
        UUID adjustment = jdbc.sql("SELECT id FROM financial_categories WHERE user_id = :u AND system_key ="
                + " 'ADJUSTMENT' AND kind = 'EXPENSE'").param("u", user.id()).query(UUID.class).optional()
                .orElseGet(() -> insertAdjustment(user.id()));
        tx(user, vnd, "EXPENSE", "90000", first, "categoryId", adjustment.toString());

        Map<String, Object> totalUsage = readJson(expect(get(user, F + "/budgets/" + total + "/usage?month=" + month),
                200));
        assertThat(totalUsage).containsEntry("spent", "150000").containsEntry("limit", "1000000")
                .containsEntry("remaining", "850000").containsEntry("month", month.toString());
        assertThat(totalUsage.get("ratio").toString()).isEqualTo("0.15");
        Map<String, Object> foodUsage = readJson(get(user, F + "/budgets/" + foodBudget + "/usage?month=" + month));
        assertThat(foodUsage).containsEntry("spent", "100000").containsEntry("remaining", "100000");
        Map<String, Object> previous = readJson(get(user, F + "/budgets/" + foodBudget + "/usage?month="
                + month.minusMonths(1)));
        assertThat(previous).containsEntry("spent", "999999").containsEntry("remaining", "-799999");
        assertThat(readJson(get(user, F + "/budgets/" + total + "/usage"))).containsEntry("month", month.toString());
        List<Map<String, Object>> all = readList(get(user, F + "/budgets/usage?month=" + month));
        assertThat(all).extracting(m -> m.get("budgetId")).containsExactlyInAnyOrder(total.toString(),
                foodBudget.toString());

        MvcResult duplicate = post(user, F + "/budgets", body("id", UUID.randomUUID().toString(), "currency", "VND",
                "amount", "1"));
        expect(duplicate, 409);
        assertThat(errorCode(duplicate)).isEqualTo("FINANCE_BUDGET_EXISTS");
        budget(user, null, "USD", "100");
        MvcResult incomeBudget = post(user, F + "/budgets", body("id", UUID.randomUUID().toString(),
                "categoryId", category(user, "INCOME").toString(), "currency", "VND", "amount", "1"));
        assertThat(errorCode(incomeBudget)).isEqualTo("FINANCE_CATEGORY_KIND_MISMATCH");
        expect(post(user, F + "/budgets/" + total + "/archive", body("version", 1)), 200);
        budget(user, null, "VND", "2000000");
        MvcResult reopen = post(user, F + "/budgets/" + total + "/unarchive", body("version", 2));
        assertThat(errorCode(reopen)).isEqualTo("FINANCE_BUDGET_EXISTS");
    }

    @Test
    void Fin_13_thresholdsFireExactlyOncePerBudgetMonthAndThreshold() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        UUID budget = budget(user, null, "VND", "1000000");
        LocalDate day = today(user);
        tx(user, acc, "EXPENSE", "790000", day);
        assertThat(alerts(budget)).isEmpty();
        UUID crossing = tx(user, acc, "EXPENSE", "10000", day);
        assertThat(alerts(budget)).containsExactly(80);
        tx(user, acc, "EXPENSE", "10000", day);
        long version = version(readJson(get(user, F + "/transactions/" + crossing)));
        expect(patch(user, F + "/transactions/" + crossing, body("version", version, "note", "sửa")), 200);
        assertThat(alerts(budget)).containsExactly(80);
        tx(user, acc, "EXPENSE", "190000", day);
        assertThat(alerts(budget)).containsExactly(80, 100);
        tx(user, acc, "EXPENSE", "1", day);
        tx(user, acc, "EXPENSE", "500000", day.minusMonths(1));
        assertThat(alerts(budget)).containsExactly(80, 100);
        assertThat(outboxEvents("BUDGET_THRESHOLD", budget)).isEqualTo(2);
        Map<String, Object> payload = json.readValue(jdbc.sql("SELECT payload::text FROM outbox_events WHERE"
                + " aggregate_id = :a ORDER BY created_at, id LIMIT 1").param("a", budget).query(String.class).single(),
                Map.class);
        assertThat(payload).containsOnlyKeys("userId", "category", "templateKey", "variables");
        assertThat(payload.toString()).doesNotContain("790000").doesNotContain("1000000");
    }

    @RepeatedTest(20)
    void Fin_13_concurrentExpensesCrossingThresholdsStillFireOnce() throws Exception {
        TestUser user = newUser();
        UUID acc = account(user, "VND");
        UUID budget = budget(user, null, "VND", "1000");
        LocalDate day = today(user);
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                results.add(pool.submit(() -> {
                    go.await();
                    return createTx(user, UUID.randomUUID(), acc, "EXPENSE", "250", day).getResponse().getStatus();
                }));
            }
            go.countDown();
            for (Future<Integer> f : results) {
                assertThat(f.get(30, TimeUnit.SECONDS)).isEqualTo(201);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(alerts(budget)).containsExactly(80, 100);
        assertThat(outboxEvents("BUDGET_THRESHOLD", budget)).isEqualTo(2);
    }

    @Test
    void Fin_14_savingGoalProgressAndServerManagedAchievedStatus() throws Exception {
        TestUser user = newUser();
        UUID goal = UUID.randomUUID();
        expect(post(user, F + "/saving-goals", body("id", goal.toString(), "name", "Quỹ khẩn cấp", "currency", "VND",
                "targetAmount", "1000000")), 201);
        UUID first = contribute(user, goal, "600000");
        Map<String, Object> view = readJson(get(user, F + "/saving-goals/" + goal));
        assertThat(view).containsEntry("status", "ACTIVE").containsEntry("saved", "600000")
                .containsEntry("remaining", "400000").containsEntry("target", "1000000");
        assertThat(view.get("ratio").toString()).isEqualTo("0.6");
        UUID second = contribute(user, goal, "400000");
        assertThat(readJson(get(user, F + "/saving-goals/" + goal))).containsEntry("status", "ACHIEVED")
                .containsEntry("remaining", "0");
        assertThat(changeLogs(user.id(), "saving_goal", goal, "UPDATE")).isEqualTo(1);
        expect(delete(user, F + "/saving-contributions/" + second + "?version=1"), 204);
        Map<String, Object> reopened = readJson(get(user, F + "/saving-goals/" + goal));
        assertThat(reopened).containsEntry("status", "ACTIVE").containsEntry("saved", "600000");
        long version = version(reopened);
        Map<String, Object> ignored = readJson(expect(patch(user, F + "/saving-goals/" + goal,
                body("version", version, "status", "ACHIEVED")), 200));
        assertThat(ignored).containsEntry("status", "ACTIVE").containsEntry("version", (int) version);
        Map<String, Object> lowered = readJson(expect(patch(user, F + "/saving-goals/" + goal,
                body("version", version, "targetAmount", "500000")), 200));
        assertThat(lowered).containsEntry("status", "ACHIEVED");
        Map<String, Object> userReopen = readJson(expect(patch(user, F + "/saving-goals/" + goal,
                body("version", version(lowered), "status", "ACTIVE")), 200));
        assertThat(userReopen).containsEntry("status", "ACTIVE");
        Map<String, Object> archived = readJson(expect(patch(user, F + "/saving-goals/" + goal,
                body("version", version(userReopen), "status", "ARCHIVED")), 200));
        assertThat(archived).containsEntry("status", "ARCHIVED");
        MvcResult locked = patch(user, F + "/saving-goals/" + goal, body("version", version(archived),
                "currency", "USD"));
        assertThat(errorCode(locked)).isEqualTo("FINANCE_CURRENCY_LOCKED");
        assertThat(items(get(user, F + "/saving-goals/" + goal + "/contributions"))).extracting(m -> m.get("id"))
                .containsExactly(first.toString());
        int balanceRows = jdbc.sql("SELECT count(*) FROM financial_transactions WHERE user_id = :u")
                .param("u", user.id()).query(Integer.class).single();
        assertThat(balanceRows).as("góp không tạo giao dịch (D111)").isZero();
        expect(delete(user, F + "/saving-goals/" + goal + "?version=" + version(archived)), 204);
        assertThat(row("saving_contributions", first).get("deleted_at")).isNotNull();
    }

    @Test
    void Fin_16_reportsSplitByCurrencySkipTombstonesAndFillEveryPeriod() throws Exception {
        TestUser user = newUser();
        UUID vnd = account(user, "VND");
        UUID usd = account(user, "USD");
        UUID food = category(user, "EXPENSE");
        YearMonth month = YearMonth.from(today(user)).minusMonths(1);
        LocalDate d3 = month.atDay(3);
        tx(user, vnd, "INCOME", "10000000", d3);
        tx(user, vnd, "EXPENSE", "250000", d3, "categoryId", food.toString());
        tx(user, vnd, "EXPENSE", "50000", month.atDay(10));
        UUID gone = tx(user, vnd, "EXPENSE", "999", month.atDay(10));
        expect(delete(user, F + "/transactions/" + gone + "?version=1"), 204);
        tx(user, usd, "EXPENSE", "3.25", d3);
        Map<String, Object> report = readJson(expect(get(user, F + "/reports/monthly?month=" + month
                + "&currency=VND"), 200));
        assertThat(report).containsEntry("income", "10000000").containsEntry("expense", "300000")
                .containsEntry("net", "9700000").containsEntry("currency", "VND");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> daily = (List<Map<String, Object>>) report.get("daily");
        assertThat(daily).hasSize(month.lengthOfMonth());
        assertThat(daily.get(2)).containsEntry("income", "10000000").containsEntry("expense", "250000");
        assertThat(daily.get(9)).containsEntry("expense", "50000");
        assertThat(daily.get(0)).containsEntry("income", "0").containsEntry("expense", "0");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byCategory = (List<Map<String, Object>>) report.get("byCategory");
        assertThat(byCategory).extracting(m -> m.get("total")).containsExactlyInAnyOrder("10000000", "250000",
                "50000");
        Map<String, Object> usdReport = readJson(get(user, F + "/reports/monthly?month=" + month + "&currency=USD"));
        assertThat(usdReport).containsEntry("expense", "3.25").containsEntry("income", "0");

        Map<String, Object> flow = readJson(expect(get(user, F + "/reports/cash-flow?from=" + month.atDay(1)
                + "&to=" + month.atDay(10) + "&granularity=day&currency=VND"), 200));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) flow.get("buckets");
        assertThat(buckets).hasSize(10);
        assertThat(buckets.get(2)).containsEntry("net", "9750000");
        assertThat(flow).containsEntry("net", "9700000");
        Map<String, Object> monthly = readJson(expect(get(user, F + "/reports/cash-flow?from=" + month.atDay(1)
                + "&to=" + month.plusMonths(1).atEndOfMonth() + "&currency=VND"), 200));
        assertThat((List<?>) monthly.get("buckets")).hasSize(2);
        Map<String, Object> weekly = readJson(expect(get(user, F + "/reports/cash-flow?from=" + month.atDay(1)
                + "&to=" + month.atEndOfMonth() + "&granularity=week&currency=VND"), 200));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> weeks = (List<Map<String, Object>>) weekly.get("buckets");
        assertThat(weeks).allSatisfy(w -> assertThat(LocalDate.parse((String) w.get("periodStart")).getDayOfWeek())
                .isEqualTo(java.time.DayOfWeek.MONDAY));
        MvcResult tooLong = get(user, F + "/reports/cash-flow?from=" + month.atDay(1) + "&to="
                + month.atDay(1).plusDays(366) + "&granularity=day&currency=VND");
        assertThat(fieldErrorCode(tooLong, "to")).isEqualTo("OUT_OF_RANGE");
        MvcResult tooManyMonths = get(user, F + "/reports/cash-flow?from=" + month.atDay(1) + "&to="
                + month.atDay(1).plusMonths(24) + "&currency=VND");
        assertThat(fieldErrorCode(tooManyMonths, "to")).isEqualTo("OUT_OF_RANGE");
    }

    @Test
    void Fin_17_combinedFiltersPagingAndSpecialCharactersInQ() throws Exception {
        TestUser user = newUser();
        UUID a = account(user, "VND");
        UUID b = account(user, "VND");
        UUID food = category(user, "EXPENSE");
        LocalDate base = today(user).minusDays(40);
        Set<String> expectedA = new HashSet<>();
        for (int i = 0; i < 25; i++) {
            UUID acc = i % 2 == 0 ? a : b;
            String note = switch (i % 5) {
                case 0 -> "giảm 50% hôm nay";
                case 1 -> "mã_KM đặc biệt";
                case 2 -> "O'Brien's cafe";
                case 3 -> "bình thường";
                default -> null;
            };
            Object[] extra = note == null ? new Object[] {"categoryId", food.toString()}
                    : new Object[] {"note", note, "categoryId", food.toString()};
            UUID id = tx(user, acc, i % 3 == 0 ? "INCOME" : "EXPENSE", String.valueOf(1000 * (i + 1)),
                    base.plusDays(i % 7), i % 3 == 0 ? new Object[] {"note", note == null ? "x" : note} : extra);
            if (acc.equals(a)) {
                expectedA.add(id.toString());
            }
        }
        Set<String> seen = new HashSet<>();
        String cursor = null;
        int pages = 0;
        do {
            Map<String, Object> page = readJson(expect(get(user, F + "/transactions?accountId=" + a + "&limit=4"
                    + (cursor == null ? "" : "&cursor=" + cursor)), 200));
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) page.get("items");
            for (Map<String, Object> r : rows) {
                assertThat(seen.add((String) r.get("id"))).as("không lặp").isTrue();
            }
            cursor = (String) page.get("nextCursor");
            pages++;
        } while (cursor != null);
        assertThat(seen).isEqualTo(expectedA);
        assertThat(pages).isEqualTo(4);

        assertThat(notes(user, "q=" + enc("%"))).allSatisfy(n -> assertThat(n).contains("%"));
        assertThat(notes(user, "q=" + enc("%"))).hasSize(5);
        assertThat(notes(user, "q=" + enc("_"))).allSatisfy(n -> assertThat(n).contains("_")).isNotEmpty();
        assertThat(notes(user, "q=" + enc("'"))).allSatisfy(n -> assertThat(n).contains("'")).isNotEmpty();
        assertThat(notes(user, "q=" + enc("\\"))).isEmpty();
        assertThat(notes(user, "q=" + enc("GIẢM"))).hasSize(5);
        List<Map<String, Object>> filtered = items(expect(get(user, F + "/transactions?kind=EXPENSE&categoryId="
                + food + "&minAmount=5000&maxAmount=15000&from=" + base.plusDays(1) + "&to=" + base.plusDays(5)
                + "&limit=200"), 200));
        assertThat(filtered).isNotEmpty().allSatisfy(r -> {
            assertThat(r.get("kind")).isEqualTo("EXPENSE");
            assertThat(r.get("categoryId")).isEqualTo(food.toString());
            int amount = Integer.parseInt((String) r.get("amount"));
            assertThat(amount).isBetween(5000, 15000);
            LocalDate day = LocalDate.parse((String) r.get("occurredOn"));
            assertThat(day).isBetween(base.plusDays(1), base.plusDays(5));
        });
        MvcResult longQ = get(user, F + "/transactions?q=" + "a".repeat(101));
        assertThat(fieldErrorCode(longQ, "q")).isEqualTo("SIZE");
        MvcResult badAmount = get(user, F + "/transactions?minAmount=abc");
        assertThat(fieldErrorCode(badAmount, "minAmount")).isEqualTo("INVALID_VALUE");
    }

    private List<String> notes(TestUser user, String query) throws Exception {
        return items(expect(get(user, F + "/transactions?limit=200&" + query), 200)).stream()
                .map(r -> (String) r.get("note")).toList();
    }

    /** MockMvc tự mã hóa URI template, nên giá trị truyền nguyên văn. */
    private static String enc(String raw) {
        return raw;
    }

    private UUID contribute(TestUser user, UUID goal, String amount) throws Exception {
        UUID id = UUID.randomUUID();
        expect(post(user, F + "/saving-goals/" + goal + "/contributions", body("id", id.toString(), "amount", amount,
                "occurredOn", today(user).toString())), 201);
        return id;
    }

    private List<Integer> alerts(UUID budgetId) {
        return jdbc.sql("SELECT threshold FROM budget_alerts WHERE budget_id = :b ORDER BY threshold")
                .param("b", budgetId).query(Integer.class).list();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readList(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsString(), List.class);
    }

    private UUID insertAdjustment(UUID userId) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO financial_categories (id, user_id, name, kind, system_key) VALUES (:id, :u,"
                + " 'Điều chỉnh', 'EXPENSE', 'ADJUSTMENT')").param("id", id).param("u", userId).update();
        return id;
    }
}
