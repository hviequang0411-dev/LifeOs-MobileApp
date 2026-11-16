package com.lifeos.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Review, dashboard và analytics qua REST trên dữ liệu thật của nhiều module (PHASE_7; INT-01 dạng ví dụ, INT-03/04/05
 * tích hợp, INT-06 dashboard thật, INT-09 đọc-sau-ghi, INT-11 review REST, giới hạn D208).
 */
class AnalyticsIT extends ApiTestBase {

    private static final ZoneId HCM = ZoneId.of("Asia/Ho_Chi_Minh");

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), HCM);
    }

    private void created(MvcResult r) throws Exception {
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isIn(200, 201, 204);
    }

    private UUID task(TestUser user, Object... extra) throws Exception {
        UUID id = UUID.randomUUID();
        Object[] kv = new Object[4 + extra.length];
        kv[0] = "id";
        kv[1] = id.toString();
        kv[2] = "title";
        kv[3] = "Việc";
        System.arraycopy(extra, 0, kv, 4, extra.length);
        created(post(user, "/api/v1/tasks", body(kv)));
        return id;
    }

    private UUID account(TestUser user, String currency) throws Exception {
        UUID id = UUID.randomUUID();
        created(post(user, "/api/v1/finance/accounts", body("id", id.toString(), "name", "TK " + id.toString()
                .substring(0, 6), "accountType", "CASH", "currency", currency)));
        return id;
    }

    private void txn(TestUser user, UUID account, String kind, String amount, LocalDate day) throws Exception {
        created(post(user, "/api/v1/finance/transactions", body("id", UUID.randomUUID().toString(), "accountId",
                account.toString(), "kind", kind, "amount", amount, "occurredOn", day.toString())));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    /** Dữ liệu một ngày: 1 Task xong đúng hạn, 1 quá hạn, habit làm, thu/chi 2 tiền tệ, chuyển khoản, sự kiện, block. */
    private void seedDay(TestUser user) throws Exception {
        LocalDate today = today();
        UUID done = task(user, "dueDate", today.toString());
        created(post(user, "/api/v1/tasks/" + done + "/complete", body("version", 1)));
        task(user, "dueDate", today.minusDays(1).toString());
        UUID habit = UUID.randomUUID();
        created(post(user, "/api/v1/habits", body("id", habit.toString(), "name", "Đọc sách", "scheduleType",
                "DAILY", "startOn", today.minusDays(3).toString())));
        created(call(user, HttpMethod.PUT, "/api/v1/habits/" + habit + "/checkins/" + today,
                body("id", UUID.randomUUID().toString(), "status", "DONE")));
        UUID vnd = account(user, "VND");
        UUID vnd2 = account(user, "VND");
        txn(user, vnd, "INCOME", "200000", today);
        txn(user, vnd, "EXPENSE", "50000", today);
        txn(user, account(user, "USD"), "INCOME", "10.5", today);
        created(post(user, "/api/v1/finance/transfers", body("id", UUID.randomUUID().toString(), "fromAccountId",
                vnd.toString(), "toAccountId", vnd2.toString(), "amount", "30000", "occurredOn", today.toString())));
        created(post(user, "/api/v1/calendar/events", body("id", UUID.randomUUID().toString(), "title", "Họp",
                "timezone", "Asia/Ho_Chi_Minh", "startLocal", today + "T09:00:00", "endLocal", today + "T10:30:00")));
        created(post(user, "/api/v1/time-blocks", body("id", UUID.randomUUID().toString(), "taskId",
                task(user).toString(), "startAt", today.atTime(13, 0).atZone(HCM).toInstant().toString(), "endAt",
                today.atTime(14, 0).atZone(HCM).toInstant().toString())));
    }

    @Test
    @SuppressWarnings("unchecked")
    void Int_01_dailyWeeklyMonthlyReviewsAggregateEveryModule() throws Exception {
        TestUser user = newUser();
        seedDay(user);
        UUID reflection = UUID.randomUUID();
        created(post(user, "/api/v1/reviews", body("id", reflection.toString(), "periodType", "DAY", "periodKey",
                today().toString(), "reflection", "Một ngày tốt", "mood", 4)));

        Map<String, Object> daily = readJson(get(user, "/api/v1/reviews/daily"));
        assertThat(daily).containsEntry("periodType", "DAY").containsEntry("periodKey", today().toString());
        assertThat(daily.get("computedAt")).isNotNull();
        assertThat(map(daily.get("tasks"))).containsEntry("completed", 1).containsEntry("completedWithDue", 1)
                .containsEntry("completedOnTime", 1).containsEntry("overdueOpen", 1);
        assertThat(map(daily.get("habits"))).containsEntry("scheduled", 1).containsEntry("done", 1);
        List<Map<String, Object>> finance = (List<Map<String, Object>>) daily.get("finance");
        assertThat(finance).extracting(f -> f.get("currency")).containsExactly("USD", "VND");
        assertThat(finance.get(1)).as("chuyển khoản không tính").containsEntry("income", "200000")
                .containsEntry("expense", "50000").containsEntry("net", "150000");
        assertThat(map(daily.get("time"))).containsEntry("events", 1).containsEntry("eventMinutes", 90)
                .containsEntry("blockMinutes", 60);
        assertThat(map(daily.get("reflection"))).containsEntry("id", reflection.toString())
                .containsEntry("mood", 4);
        assertThat(daily.get("previous")).as("review ngày không so sánh").isNull();

        Map<String, Object> weekly = readJson(get(user, "/api/v1/reviews/weekly?date=" + today()));
        assertThat(map(weekly.get("tasks"))).containsEntry("completed", 1);
        assertThat(map(weekly.get("previous"))).containsEntry("tasksCompleted", 0).containsEntry("habitsDone", 0);
        assertThat(LocalDate.parse((String) weekly.get("from")).getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
        Map<String, Object> monthly = readJson(get(user, "/api/v1/reviews/monthly?month="
                + java.time.YearMonth.from(today())));
        assertThat(monthly).containsEntry("periodKey", today().withDayOfMonth(1).toString());
        assertThat(get(user, "/api/v1/reviews/daily?date=2027-02-30").getResponse().getStatus()).isEqualTo(400);

        TestUser stranger = newUser();
        assertThat(map(readJson(get(stranger, "/api/v1/reviews/daily")).get("tasks"))).containsEntry("completed", 0)
                .as("không lộ dữ liệu người khác");
    }

    @Test
    @SuppressWarnings("unchecked")
    void Int_03_04_05_domainAnalyticsAndRangeLimits() throws Exception {
        TestUser user = newUser();
        seedDay(user);
        LocalDate today = today();
        Map<String, Object> tasks = readJson(get(user, "/api/v1/analytics/tasks?from=" + today.minusDays(6) + "&to="
                + today + "&granularity=day"));
        assertThat(tasks).containsEntry("completed", 1).containsEntry("overdueOpen", 1)
                .containsEntry("medianCycleSeconds", null).as("mẫu < 5 ⇒ null");
        assertThat((List<?>) tasks.get("buckets")).hasSize(7);
        assertThat(map(tasks.get("previous"))).containsEntry("completed", 0);

        Map<String, Object> finance = readJson(get(user, "/api/v1/analytics/finance?currency=VND&from="
                + today.withDayOfMonth(1) + "&to=" + today));
        assertThat(((Number) finance.get("savingsRate")).doubleValue()).isEqualTo(0.75);
        assertThat(map(finance.get("flow"))).containsEntry("income", "200000").containsEntry("expense", "50000");
        assertThat(get(user, "/api/v1/analytics/finance?from=" + today + "&to=" + today).getResponse().getStatus())
                .as("currency bắt buộc").isEqualTo(400);

        Map<String, Object> time = readJson(get(user, "/api/v1/analytics/time?from=" + today + "&to=" + today));
        assertThat(time).containsEntry("eventMinutes", 90).containsEntry("blockMinutes", 60);
        Map<String, Object> habits = readJson(get(user, "/api/v1/analytics/habits?from=" + today + "&to=" + today));
        assertThat(habits).containsEntry("done", 1);
        assertThat((List<?>) habits.get("habits")).hasSize(1);

        UUID atRisk = UUID.randomUUID();
        created(post(user, "/api/v1/goals", body("id", atRisk.toString(), "title", "Trễ nhịp", "startOn",
                today.minusDays(50).toString(), "targetOn", today.plusDays(50).toString())));
        task(user, "goalId", atRisk.toString());
        UUID overdue = UUID.randomUUID();
        created(post(user, "/api/v1/goals", body("id", overdue.toString(), "title", "Quá hạn", "targetOn",
                today.minusDays(1).toString())));
        List<Map<String, Object>> goals = (List<Map<String, Object>>) readJson(get(user, "/api/v1/analytics/goals"))
                .get("goals");
        assertThat(goals).filteredOn(g -> atRisk.toString().equals(g.get("goalId"))).singleElement()
                .satisfies(g -> assertThat(g).containsEntry("risk", "AT_RISK"));
        assertThat(goals).filteredOn(g -> overdue.toString().equals(g.get("goalId"))).singleElement()
                .satisfies(g -> assertThat(g).containsEntry("risk", "OVERDUE"));

        MvcResult tooLong = get(user, "/api/v1/analytics/tasks?from=" + today.minusDays(366) + "&to=" + today);
        assertThat(tooLong.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(tooLong)).containsEntry("errorCode", "ANALYTICS_RANGE_TOO_LARGE");
        assertThat(get(user, "/api/v1/analytics/tasks?from=" + today.minusMonths(24) + "&to=" + today
                + "&granularity=month").getResponse().getStatus()).isEqualTo(422);
        assertThat(get(user, "/api/v1/analytics/tasks?from=" + today.minusMonths(23) + "&to=" + today
                + "&granularity=month").getResponse().getStatus()).isEqualTo(200);
        assertThat(get(user, "/api/v1/analytics/time?from=" + today.minusDays(366) + "&to=" + today
                + "&granularity=month").getResponse().getStatus()).as("time luôn ≤ 366 ngày").isEqualTo(422);
        assertThat(get(user, "/api/v1/analytics/tasks?to=" + today).getResponse().getStatus()).isEqualTo(400);
        assertThat(get(user, "/api/v1/analytics/tasks?from=" + today + "&to=" + today + "&granularity=year")
                .getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @SuppressWarnings("unchecked")
    void Int_06_09_dashboardCardsAreOkAndReflectWritesImmediately() throws Exception {
        TestUser user = newUser();
        UUID due = task(user, "dueDate", today().toString(), "title", "Hôm nay");
        Map<String, Object> dashboard = readJson(get(user, "/api/v1/dashboard"));
        assertThat(dashboard.get("computedAt")).isNotNull();
        List<Map<String, Object>> cards = (List<Map<String, Object>>) dashboard.get("cards");
        assertThat(cards).extracting(c -> c.get("id")).containsExactlyInAnyOrder("tasks_today", "habits_today",
                "budgets", "upcoming_events", "goals");
        assertThat(cards).allSatisfy(c -> assertThat(c).containsEntry("status", "OK"));
        Map<String, Object> tasksToday = map(cards.stream().filter(c -> "tasks_today".equals(c.get("id")))
                .findFirst().orElseThrow().get("data"));
        assertThat((List<Map<String, Object>>) tasksToday.get("dueToday")).extracting(t -> t.get("taskId"))
                .containsExactly(due.toString());

        created(post(user, "/api/v1/tasks/" + due + "/complete", body("version", 1)));
        assertThat(map(readJson(get(user, "/api/v1/reviews/daily")).get("tasks"))).as("đọc ngay sau ghi")
                .containsEntry("completed", 1);
    }

    @Test
    void Int_11_reviewsAreUniquePerPeriodAndValidated() throws Exception {
        TestUser user = newUser();
        LocalDate monday = today().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        UUID id = UUID.randomUUID();
        MvcResult first = post(user, "/api/v1/reviews", body("id", id.toString(), "periodType", "WEEK", "periodKey",
                monday.toString(), "reflection", "Tuần ổn"));
        assertThat(first.getResponse().getStatus()).isEqualTo(201);
        MvcResult dup = post(user, "/api/v1/reviews", body("id", UUID.randomUUID().toString(), "periodType", "WEEK",
                "periodKey", monday.toString()));
        assertThat(dup.getResponse().getStatus()).isEqualTo(409);
        assertThat(readJson(dup)).containsEntry("errorCode", "REVIEW_EXISTS");
        assertThat(readJson(post(user, "/api/v1/reviews", body("id", UUID.randomUUID().toString(), "periodType",
                "WEEK", "periodKey", monday.plusDays(1).toString())))).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(readJson(post(user, "/api/v1/reviews", body("id", UUID.randomUUID().toString(), "periodType",
                "MONTH", "periodKey", today().withDayOfMonth(1).toString(), "mood", 6))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(readJson(post(user, "/api/v1/reviews", body("id", UUID.randomUUID().toString(), "periodType",
                "DAY", "periodKey", today().toString(), "reflection", "x".repeat(5001)))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(readJson(post(user, "/api/v1/reviews", body("id", UUID.randomUUID().toString(), "periodType",
                "YEAR", "periodKey", today().toString())))).containsEntry("errorCode", "VALIDATION_FAILED");

        Map<String, Object> patched = readJson(patch(user, "/api/v1/reviews/" + id, body("version", 1, "reflection",
                "Tuần rất ổn", "mood", 5, "periodKey", "2000-01-03")));
        assertThat(patched).containsEntry("reflection", "Tuần rất ổn").containsEntry("mood", 5)
                .containsEntry("periodKey", monday.toString());
        assertThat(items(get(user, "/api/v1/reviews?periodType=WEEK"))).hasSize(1);
        assertThat(delete(user, "/api/v1/reviews/" + id + "?version=2").getResponse().getStatus()).isEqualTo(204);
        assertThat(post(user, "/api/v1/reviews", body("id", UUID.randomUUID().toString(), "periodType", "WEEK",
                "periodKey", monday.toString())).getResponse().getStatus()).as("tạo lại sau khi xóa").isEqualTo(201);
        assertThat(get(user, "/api/v1/reviews/" + id).getResponse().getStatus()).isEqualTo(404);
    }
}
