package com.lifeos.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * IDOR-01/02 dạng ma trận (TESTING_RULES §7, SECURITY_RULES ownership; PHASE_2 HAB-11): quét MỌI route cần xác thực có
 * biến đường dẫn ({id}, {logId}, {stepId}, {date}) từ RequestMappingHandlerMapping, nên route mới tự động vào phạm vi.
 * Với mỗi route, user tấn công gọi lên tài nguyên CÓ THẬT của nạn nhân và lên id ngẫu nhiên:
 * <ul>
 *   <li>cả hai đều 404 và thân lỗi giống hệt (bỏ correlationId, instance) — không lộ sự tồn tại;</li>
 *   <li>dòng của nạn nhân không đổi một cột nào, nạn nhân không có change_logs mới.</li>
 * </ul>
 * Route thuộc loại tài nguyên chưa khai báo làm test đỏ: người thêm route phải khai báo cách tạo tài nguyên.
 */
@TestPropertySource(properties = {"lifeos.ai.enabled=true", "lifeos.ai.features.chat=true",
        "lifeos.ai.features.notes=true", "lifeos.ai.rate-limit-per-minute=1000"})
class IdorMatrixIT extends ApiTestBase {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    /** Loại tài nguyên theo đoạn đường dẫn ngay sau /api/v1/; table = bảng kiểm "không đổi". */
    private enum Resource {
        TASKS("tasks", "tasks", "{\"version\":1,\"title\":\"Bị sửa\"}"),
        PROJECTS("projects", "projects", "{\"version\":1,\"name\":\"Bị sửa\"}"),
        CATEGORIES("categories", "categories", "{\"version\":1,\"name\":\"Bị sửa\"}"),
        DEVICES("devices", "devices", null),
        HABITS("habits", "habits", "{\"version\":1,\"name\":\"Bị sửa\"}"),
        ROUTINES("routines", "routines", "{\"version\":1,\"name\":\"Bị sửa\"}"),
        ROUTINE_STEPS("routine-steps", "routine_steps", "{\"version\":1,\"title\":\"Bị sửa\"}"),
        ROUTINE_LOGS("routine-logs", "routine_logs", null),
        FIN_ACCOUNTS("finance/accounts", "financial_accounts", "{\"version\":1,\"name\":\"Bị sửa\"}"),
        FIN_CATEGORIES("finance/categories", "financial_categories", "{\"version\":1,\"name\":\"Bị sửa\"}"),
        FIN_TRANSACTIONS("finance/transactions", "financial_transactions", "{\"version\":1,\"note\":\"Bị sửa\"}"),
        FIN_TRANSFERS("finance/transfers", "account_transfers", "{\"version\":1,\"note\":\"Bị sửa\"}"),
        FIN_BUDGETS("finance/budgets", "budgets", "{\"version\":1,\"amount\":\"1\"}"),
        FIN_RECURRING("finance/recurring", "recurring_transactions", "{\"version\":1,\"note\":\"Bị sửa\"}"),
        FIN_GOALS("finance/saving-goals", "saving_goals", "{\"version\":1,\"name\":\"Bị sửa\"}"),
        FIN_CONTRIBUTIONS("finance/saving-contributions", "saving_contributions", null),
        CAL_EVENTS("calendar", "calendar_events", "{\"version\":1,\"title\":\"Bị sửa\"}"),
        TIME_BLOCKS("time-blocks", "time_blocks", "{\"version\":1,\"note\":\"Bị sửa\"}"),
        REMINDERS("reminders", "reminders", "{\"version\":1,\"urgent\":true}"),
        NOTIFICATIONS("notifications", "notifications", null),
        GOALS("goals", "goals", "{\"version\":1,\"title\":\"Bị sửa\"}"),
        MILESTONES("milestones", "milestones", "{\"version\":1,\"title\":\"Bị sửa\"}"),
        NOTES("notes", "notes", "{\"version\":1,\"content\":\"Bị sửa\"}"),
        TAGS("tags", "tags", "{\"version\":1,\"name\":\"Bị sửa\"}"),
        REVIEWS("reviews", "reviews", "{\"version\":1,\"reflection\":\"Bị sửa\"}"),
        ACCOUNT_EXPORTS("account", "data_exports", null),
        SYNC_ENTITIES("sync", "projects", null),
        AI_CONVERSATIONS("ai/conversations", "ai_conversations", null),
        AI_MESSAGES("ai/messages", "ai_messages", null);

        final String segment;
        final String table;
        final String patchBody;

        Resource(String segment, String table, String patchBody) {
            this.segment = segment;
            this.table = table;
            this.patchBody = patchBody;
        }
    }

    /** Tài nguyên của nạn nhân: id chính và (khi cần) id bước thuộc cùng routine. */
    private record Victim(UUID id, UUID stepId) {
    }

    private record Route(HttpMethod method, String pattern) implements Comparable<Route> {
        Resource resource() {
            if (pattern.startsWith("/api/v1/ai/conversations/")) return Resource.AI_CONVERSATIONS;
            if (pattern.startsWith("/api/v1/ai/messages/")) return Resource.AI_MESSAGES;
            if (pattern.startsWith("/api/v1/ai/tasks/")) return Resource.TASKS;
            if (pattern.startsWith("/api/v1/ai/notes/")) return Resource.NOTES;
            String[] parts = pattern.substring("/api/v1/".length()).split("/");
            String segment = parts[0].equals("finance") ? parts[0] + "/" + parts[1] : parts[0];
            return Arrays.stream(Resource.values()).filter(r -> r.segment.equals(segment)).findFirst().orElse(null);
        }

        @Override
        public int compareTo(Route o) {
            return (pattern + method).compareTo(o.pattern + o.method);
        }

        @Override
        public String toString() {
            return method + " " + pattern;
        }
    }

    private List<Route> pathVariableRoutes() {
        Set<Route> routes = new TreeSet<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            for (String pattern : info.getPatternValues()) {
                if (!pattern.startsWith("/api/v1/") || !pattern.contains("{")) {
                    continue;
                }
                for (RequestMethod method : info.getMethodsCondition().getMethods()) {
                    routes.add(new Route(HttpMethod.valueOf(method.name()), pattern));
                }
            }
        }
        return new ArrayList<>(routes);
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private Victim victimResource(Resource resource, TestUser victim) throws Exception {
        UUID id = UUID.randomUUID();
        switch (resource) {
            case TASKS -> expect201(post(victim, "/api/v1/tasks", body("id", id.toString(), "title", "Của nạn nhân")));
            case PROJECTS -> expect201(post(victim, "/api/v1/projects", body("id", id.toString(), "name", "Riêng")));
            case CATEGORIES -> expect201(post(victim, "/api/v1/categories", body("id", id.toString(),
                    "name", "Riêng " + id.toString().substring(0, 8))));
            case DEVICES -> jdbc.sql("INSERT INTO devices (id, user_id, name, platform) VALUES (:id, :u, 'Pixel',"
                    + " 'ANDROID')").param("id", id).param("u", victim.id()).update();
            case HABITS -> expect201(post(victim, "/api/v1/habits", body("id", id.toString(), "name", "Riêng",
                    "scheduleType", "DAILY", "startOn", today().minusDays(5).toString())));
            case ROUTINES -> expect201(post(victim, "/api/v1/routines", body("id", id.toString(), "name", "Riêng",
                    "kind", "DAILY")));
            case ROUTINE_STEPS -> {
                UUID routine = UUID.randomUUID();
                expect201(post(victim, "/api/v1/routines", body("id", routine.toString(), "name", "R",
                        "kind", "DAILY")));
                expect201(post(victim, "/api/v1/routines/" + routine + "/steps", body("id", id.toString(),
                        "title", "Bước")));
            }
            case ROUTINE_LOGS -> {
                UUID routine = UUID.randomUUID();
                UUID step = UUID.randomUUID();
                expect201(post(victim, "/api/v1/routines", body("id", routine.toString(), "name", "R",
                        "kind", "DAILY")));
                expect201(post(victim, "/api/v1/routines/" + routine + "/steps", body("id", step.toString(),
                        "title", "Bước")));
                expect201(call(victim, HttpMethod.PUT, "/api/v1/routines/" + routine + "/logs/" + today(),
                        body("id", id.toString(), "totalSteps", 1)));
                return new Victim(id, step);
            }
            case FIN_ACCOUNTS -> finAccount(victim, id);
            case FIN_CATEGORIES -> expect201(post(victim, "/api/v1/finance/categories", body("id", id.toString(),
                    "name", "Riêng " + id.toString().substring(0, 8), "kind", "EXPENSE")));
            case FIN_TRANSACTIONS -> expect201(post(victim, "/api/v1/finance/transactions", body("id", id.toString(),
                    "accountId", finAccount(victim, UUID.randomUUID()).toString(), "kind", "EXPENSE",
                    "amount", "1000", "occurredOn", today().toString(), "note", "Riêng")));
            case FIN_TRANSFERS -> expect201(post(victim, "/api/v1/finance/transfers", body("id", id.toString(),
                    "fromAccountId", finAccount(victim, UUID.randomUUID()).toString(),
                    "toAccountId", finAccount(victim, UUID.randomUUID()).toString(), "amount", "1000",
                    "occurredOn", today().toString())));
            case FIN_BUDGETS -> {
                UUID category = UUID.randomUUID();
                expect201(post(victim, "/api/v1/finance/categories", body("id", category.toString(),
                        "name", "NS " + category.toString().substring(0, 8), "kind", "EXPENSE")));
                expect201(post(victim, "/api/v1/finance/budgets", body("id", id.toString(),
                        "categoryId", category.toString(), "currency", "VND", "amount", "5000000")));
            }
            case FIN_RECURRING -> expect201(post(victim, "/api/v1/finance/recurring", body("id", id.toString(),
                    "accountId", finAccount(victim, UUID.randomUUID()).toString(), "kind", "EXPENSE",
                    "amount", "1000", "frequency", "MONTHLY", "startOn", today().plusDays(3).toString())));
            case FIN_GOALS -> finGoal(victim, id);
            case SYNC_ENTITIES -> expect201(post(victim, "/api/v1/projects", body("id", id.toString(), "name",
                    "Riêng " + id.toString().substring(0, 8))));
            case CAL_EVENTS -> expect201(post(victim, "/api/v1/calendar/events", body("id", id.toString(), "title",
                    "Riêng", "timezone", "Asia/Ho_Chi_Minh", "startLocal", "2027-03-01T09:00:00", "endLocal",
                    "2027-03-01T10:00:00", "recurrence", Map.of("freq", "DAILY", "count", 5))));
            case TIME_BLOCKS -> {
                UUID task = UUID.randomUUID();
                expect201(post(victim, "/api/v1/tasks", body("id", task.toString(), "title", "Riêng")));
                expect201(post(victim, "/api/v1/time-blocks", body("id", id.toString(), "taskId", task.toString(),
                        "startAt", "2027-01-01T01:00:00Z", "endAt", "2027-01-01T02:00:00Z")));
            }
            case REMINDERS -> expect201(post(victim, "/api/v1/reminders", body("id", id.toString(), "targetType",
                    "NONE", "mode", "LOCAL_AT", "localTime", "07:00")));
            case NOTIFICATIONS -> jdbc.sql("INSERT INTO notifications (id, user_id, category, template_key, title,"
                    + " body, dedupe_key, created_at, expires_at) VALUES (:id, :u, 'REMINDER', 'reminder.generic',"
                    + " 'Nhắc nhở', 'Bạn có nhắc nhở', :k, now(), now() + interval '90 days')").param("id", id)
                    .param("u", victim.id()).param("k", "idor:" + id).update();
            case GOALS -> expect201(post(victim, "/api/v1/goals", body("id", id.toString(), "title", "Riêng")));
            case MILESTONES -> {
                UUID goal = UUID.randomUUID();
                expect201(post(victim, "/api/v1/goals", body("id", goal.toString(), "title", "Riêng")));
                expect201(post(victim, "/api/v1/goals/" + goal + "/milestones", body("id", id.toString(),
                        "title", "Mốc")));
            }
            case REVIEWS -> expect201(post(victim, "/api/v1/reviews", body("id", id.toString(), "periodType", "DAY",
                    "periodKey", today().minusDays(java.util.concurrent.ThreadLocalRandom.current().nextInt(5000))
                            .toString(), "reflection", "Riêng")));
            case ACCOUNT_EXPORTS -> {
                MvcResult requested = post(victim, "/api/v1/account/export", null);
                assertThat(requested.getResponse().getStatus()).as("202 tạo mới, 200 khi đang có yêu cầu chờ")
                        .isIn(200, 202);
                return new Victim(UUID.fromString((String) readJson(requested).get("id")), null);
            }
            case NOTES -> expect201(post(victim, "/api/v1/notes", body("id", id.toString(), "content", "Riêng")));
            case TAGS -> expect201(post(victim, "/api/v1/tags", body("id", id.toString(), "name",
                    "Riêng " + id.toString().substring(0, 8))));
            case FIN_CONTRIBUTIONS -> expect201(post(victim, "/api/v1/finance/saving-goals/"
                    + finGoal(victim, UUID.randomUUID()) + "/contributions", body("id", id.toString(),
                    "amount", "1000", "occurredOn", today().toString())));
            case AI_CONVERSATIONS -> insertAiConversation(id, victim.id());
            case AI_MESSAGES -> {
                UUID conversationId = UUID.randomUUID();
                insertAiConversation(conversationId, victim.id());
                jdbc.sql("INSERT INTO ai_messages (id, user_id, conversation_id, role, kind, content, "
                        + "context_snapshot) VALUES (:id, :u, :c, 'ASSISTANT', 'CHAT', 'Riêng', '{}'::jsonb)")
                        .param("id", id).param("u", victim.id()).param("c", conversationId).update();
            }
        }
        return new Victim(id, null);
    }

    private void insertAiConversation(UUID id, UUID userId) {
        jdbc.sql("INSERT INTO ai_conversations (id, user_id, title, status, last_message_at, expires_at) "
                + "VALUES (:id, :u, 'Riêng', 'ACTIVE', now(), now() + interval '90 days')")
                .param("id", id).param("u", userId).update();
    }

    private void consentAi(TestUser user) {
        jdbc.sql("INSERT INTO ai_user_preferences (user_id, allow_task_calendar, allow_notes, policy_version, "
                + "accepted_provider_terms_at) VALUES (:u, true, true, 'v1', now())")
                .param("u", user.id()).update();
    }

    private UUID finAccount(TestUser victim, UUID id) throws Exception {
        expect201(post(victim, "/api/v1/finance/accounts", body("id", id.toString(),
                "name", "TK " + id.toString().substring(0, 8), "accountType", "CASH", "currency", "VND")));
        return id;
    }

    private UUID finGoal(TestUser victim, UUID id) throws Exception {
        expect201(post(victim, "/api/v1/finance/saving-goals", body("id", id.toString(), "name", "Riêng",
                "currency", "VND", "targetAmount", "1000000")));
        return id;
    }

    private static void expect201(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
    }

    private MvcResult attack(TestUser attacker, Route route, Victim target) throws Exception {
        String path = route.pattern().replace("{id}", target.id().toString())
                .replace("{logId}", target.id().toString())
                .replace("{stepId}", (target.stepId() != null ? target.stepId() : UUID.randomUUID()).toString())
                .replace("{exceptionId}", UUID.randomUUID().toString())
                .replace("{linkId}", UUID.randomUUID().toString())
                .replace("{type}", "project")
                .replace("{date}", today().toString());
        if (path.endsWith("/stats")) {
            path += "?from=" + today().minusDays(7) + "&to=" + today();
        }
        Resource resource = route.resource();
        return switch (route.method().name()) {
            case "GET" -> get(attacker, path);
            case "PATCH" -> patch(attacker, path, resource.patchBody);
            case "PUT" -> call(attacker, HttpMethod.PUT, path,
                    "{\"status\":\"DONE\",\"totalSteps\":1,\"token\":\"idor-token\",\"platform\":\"ANDROID\","
                            + "\"tagIds\":[]}");
            case "POST" -> post(attacker, path, path.endsWith("/steps")
                    ? body("id", UUID.randomUUID().toString(), "title", "Chèn")
                    : path.contains("/ai/conversations/") && path.endsWith("/messages")
                    ? body("message", "Không được đọc", "mode", "CHAT",
                            "contextDomains", List.of("TASK_CALENDAR"))
                    : path.contains("/ai/tasks/") && path.endsWith("/breakdown")
                    ? body("instruction", "Không được đọc", "contextDomains", List.of("TASK_CALENDAR"))
                    : path.contains("/ai/notes/") && path.endsWith("/summary")
                    ? body("style", "BRIEF", "contextDomains", List.of("NOTES"))
                    : path.endsWith("/reconcile") ? body("statementBalance", "0", "asOfDate", today().toString())
                    : path.endsWith("/contributions") ? body("id", UUID.randomUUID().toString(), "amount", "1",
                            "occurredOn", today().toString())
                    : path.endsWith("/exceptions") ? body("id", UUID.randomUUID().toString(), "originalStartLocal",
                            "2027-03-02T09:00:00", "kind", "CANCELLED")
                    : path.endsWith("/milestones") ? body("id", UUID.randomUUID().toString(), "title", "Chèn")
                    : path.endsWith("/links") ? body("id", UUID.randomUUID().toString(), "targetType", "TASK",
                            "targetId", UUID.randomUUID().toString())
                    : path.endsWith("/status") ? body("version", 1, "status", "PAUSED")
                    : path.endsWith("/split") ? body("version", 1, "originalStartLocal", "2027-03-03T09:00:00",
                            "newEventId", UUID.randomUUID().toString())
                    : "{\"version\":1}");
            case "DELETE" -> delete(attacker, resource == Resource.DEVICES || route.pattern().contains("{date}")
                    || route.pattern().contains("{stepId}") ? path : path + "?version=1");
            default -> throw new IllegalStateException("Phương thức chưa hỗ trợ trong ma trận: " + route);
        };
    }

    private Map<String, Object> row(Resource resource, UUID id) {
        return jdbc.sql("SELECT * FROM " + resource.table + " WHERE id = :id").param("id", id).query().singleRow();
    }

    private int changeLogsOf(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u").param("u", userId)
                .query(Integer.class).single();
    }

    @Test
    void everyPathVariableRouteAnswersAnotherUsersResourceExactlyLikeAMissingOneAndChangesNothing()
            throws Exception {
        List<Route> routes = pathVariableRoutes();
        assertThat(routes).extracting(Route::toString).contains(
                "GET /api/v1/tasks/{id}", "PATCH /api/v1/tasks/{id}", "POST /api/v1/tasks/{id}/complete",
                "POST /api/v1/tasks/{id}/reopen", "DELETE /api/v1/tasks/{id}",
                "GET /api/v1/projects/{id}", "PATCH /api/v1/projects/{id}", "POST /api/v1/projects/{id}/archive",
                "POST /api/v1/projects/{id}/unarchive", "DELETE /api/v1/projects/{id}",
                "GET /api/v1/projects/{id}/progress",
                "GET /api/v1/categories/{id}", "PATCH /api/v1/categories/{id}", "DELETE /api/v1/categories/{id}",
                "DELETE /api/v1/devices/{id}",
                "GET /api/v1/habits/{id}", "PATCH /api/v1/habits/{id}", "DELETE /api/v1/habits/{id}",
                "PUT /api/v1/habits/{id}/checkins/{date}", "DELETE /api/v1/habits/{id}/checkins/{date}",
                "GET /api/v1/habits/{id}/checkins", "GET /api/v1/habits/{id}/stats",
                "GET /api/v1/routines/{id}", "POST /api/v1/routines/{id}/steps",
                "PUT /api/v1/routines/{id}/logs/{date}", "GET /api/v1/routines/{id}/stats",
                "PATCH /api/v1/routine-steps/{id}", "DELETE /api/v1/routine-steps/{id}",
                "PUT /api/v1/routine-logs/{logId}/steps/{stepId}",
                "DELETE /api/v1/routine-logs/{logId}/steps/{stepId}",
                "GET /api/v1/finance/accounts/{id}", "PATCH /api/v1/finance/accounts/{id}",
                "GET /api/v1/finance/accounts/{id}/balance", "POST /api/v1/finance/accounts/{id}/reconcile",
                "POST /api/v1/finance/accounts/{id}/archive", "DELETE /api/v1/finance/accounts/{id}",
                "PATCH /api/v1/finance/categories/{id}", "DELETE /api/v1/finance/categories/{id}",
                "GET /api/v1/finance/transactions/{id}", "PATCH /api/v1/finance/transactions/{id}",
                "DELETE /api/v1/finance/transactions/{id}", "PATCH /api/v1/finance/transfers/{id}",
                "GET /api/v1/finance/budgets/{id}/usage", "PATCH /api/v1/finance/budgets/{id}",
                "POST /api/v1/finance/recurring/{id}/pause", "DELETE /api/v1/finance/recurring/{id}",
                "GET /api/v1/finance/saving-goals/{id}", "POST /api/v1/finance/saving-goals/{id}/contributions",
                "GET /api/v1/finance/saving-goals/{id}/contributions",
                "DELETE /api/v1/finance/saving-contributions/{id}",
                "GET /api/v1/calendar/events/{id}", "PATCH /api/v1/calendar/events/{id}",
                "DELETE /api/v1/calendar/events/{id}", "POST /api/v1/calendar/events/{id}/split",
                "POST /api/v1/calendar/events/{id}/exceptions",
                "DELETE /api/v1/calendar/events/{id}/exceptions/{exceptionId}",
                "PATCH /api/v1/time-blocks/{id}", "DELETE /api/v1/time-blocks/{id}",
                "GET /api/v1/reminders/{id}", "PATCH /api/v1/reminders/{id}", "POST /api/v1/reminders/{id}/pause",
                "DELETE /api/v1/reminders/{id}", "PATCH /api/v1/notifications/{id}/read",
                "DELETE /api/v1/notifications/{id}", "PUT /api/v1/devices/{id}/push-token",
                "DELETE /api/v1/devices/{id}/push-token", "GET /api/v1/sync/entities/{type}/{id}",
                "GET /api/v1/goals/{id}", "PATCH /api/v1/goals/{id}", "DELETE /api/v1/goals/{id}",
                "POST /api/v1/goals/{id}/status", "POST /api/v1/goals/{id}/milestones",
                "GET /api/v1/goals/{id}/tasks", "GET /api/v1/goals/{id}/notes",
                "PATCH /api/v1/milestones/{id}", "POST /api/v1/milestones/{id}/complete",
                "POST /api/v1/milestones/{id}/reopen", "DELETE /api/v1/milestones/{id}",
                "GET /api/v1/notes/{id}", "PATCH /api/v1/notes/{id}", "DELETE /api/v1/notes/{id}",
                "POST /api/v1/notes/{id}/archive", "POST /api/v1/notes/{id}/pin", "PUT /api/v1/notes/{id}/tags",
                "POST /api/v1/notes/{id}/links", "DELETE /api/v1/notes/{id}/links/{linkId}",
                "PATCH /api/v1/tags/{id}", "DELETE /api/v1/tags/{id}",
                "GET /api/v1/reviews/{id}", "PATCH /api/v1/reviews/{id}", "DELETE /api/v1/reviews/{id}",
                "GET /api/v1/account/export/{id}", "GET /api/v1/account/export/{id}/download",
                "GET /api/v1/ai/conversations/{id}", "DELETE /api/v1/ai/conversations/{id}",
                "POST /api/v1/ai/conversations/{id}/messages", "GET /api/v1/ai/messages/{id}/context",
                "POST /api/v1/ai/tasks/{id}/breakdown", "POST /api/v1/ai/notes/{id}/summary");

        TestUser victim = newUser();
        TestUser attacker = newUser();
        consentAi(victim);
        consentAi(attacker);
        List<String> failures = new ArrayList<>();
        for (Route route : routes) {
            Resource resource = route.resource();
            if (resource == null) {
                failures.add(route + ": loại tài nguyên chưa khai báo trong IdorMatrixIT");
                continue;
            }
            Victim owned = victimResource(resource, victim);
            Map<String, Object> before = row(resource, owned.id());
            int victimLogs = changeLogsOf(victim.id());

            MvcResult onVictim = attack(attacker, route, owned);
            MvcResult onRandom = attack(attacker, route, new Victim(UUID.randomUUID(), null));

            if (onVictim.getResponse().getStatus() != 404) {
                failures.add(route + ": tài nguyên của người khác → " + onVictim.getResponse().getStatus() + " "
                        + onVictim.getResponse().getContentAsString());
                continue;
            }
            if (onRandom.getResponse().getStatus() != 404 || !problem(onVictim).equals(problem(onRandom))) {
                failures.add(route + ": khác với id không tồn tại: " + problem(onVictim) + " vs "
                        + onRandom.getResponse().getStatus() + " " + problem(onRandom));
            }
            if (!row(resource, owned.id()).equals(before)) {
                failures.add(route + ": dòng của nạn nhân bị thay đổi");
            }
            if (changeLogsOf(victim.id()) != victimLogs) {
                failures.add(route + ": nạn nhân có change_logs mới");
            }
        }
        assertThat(failures).as("vi phạm IDOR").isEmpty();
    }
}
