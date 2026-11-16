package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * GET /tasks (API_CONVENTIONS §7; D59, D78, D79): mọi kiểu sắp xếp × hai chiều đi qua nhiều trang không sót, không
 * lặp, đúng thứ tự (priority theo hạng nghiệp vụ, dueDate null cuối ở cả hai chiều, trùng khóa so id cùng chiều);
 * từng bộ lọc; tham số sai → 400; chỉ dữ liệu chưa xóa của chính user.
 */
class TaskListIT extends TaskApiTestBase {

    private static final List<String> PRIORITY_ORDER = List.of("NONE", "LOW", "MEDIUM", "HIGH");

    /** Đi hết các trang với limit nhỏ; trả các Task theo thứ tự nhận được. */
    private List<Map<String, Object>> walk(TestUser user, String query, int limit) throws Exception {
        List<Map<String, Object>> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            String url = TASKS + "?limit=" + limit + query + (cursor == null ? "" : "&cursor=" + cursor);
            MvcResult result = get(user, url);
            assertThat(result.getResponse().getStatus()).as(url + " " + result.getResponse().getContentAsString())
                    .isEqualTo(200);
            seen.addAll(items(result));
            Map<String, Object> page = readJson(result);
            cursor = (String) page.get("nextCursor");
            assertThat(cursor == null).isEqualTo(!(Boolean) page.get("hasMore"));
            assertThat(++pages).isLessThan(100);
        } while (cursor != null);
        return seen;
    }

    private static List<String> ids(List<Map<String, Object>> tasks) {
        return tasks.stream().map(t -> (String) t.get("id")).toList();
    }

    /** Bộ comparator mô phỏng đúng quy tắc D79; id so theo chuỗi hex chữ thường (= thứ tự uuid của PostgreSQL). */
    private static Comparator<Map<String, Object>> expected(String field, boolean asc) {
        Comparator<Map<String, Object>> byId = Comparator.comparing(t -> (String) t.get("id"));
        Comparator<Map<String, Object>> key = switch (field) {
            case "priority" -> Comparator.comparing(t -> PRIORITY_ORDER.indexOf((String) t.get("priority")));
            case "createdAt", "updatedAt" -> Comparator.comparing(t -> Instant.parse((String) t.get(field)));
            default -> Comparator.comparing(t -> (String) t.get("dueDate"));
        };
        Comparator<Map<String, Object>> withId = key.thenComparing(byId);
        Comparator<Map<String, Object>> directed = asc ? withId : withId.reversed();
        if (!"dueDate".equals(field)) {
            return directed;
        }
        // dueDate null luôn cuối, giữa các null so id cùng chiều.
        Comparator<Map<String, Object>> nullsLast = Comparator.comparing(t -> t.get("dueDate") == null);
        return nullsLast.thenComparing((a, b) -> {
            if (a.get("dueDate") == null && b.get("dueDate") == null) {
                return asc ? byId.compare(a, b) : byId.compare(b, a);
            }
            return directed.compare(a, b);
        });
    }

    @Test
    void everySortInBothDirectionsCoversAllTasksOnceInTheRightOrder() throws Exception {
        TestUser user = newUser();
        String[][] specs = {
            {"HIGH", "2026-11-10"}, {"NONE", null}, {"LOW", "2026-11-08"}, {"MEDIUM", "2026-11-10"},
            {"HIGH", null}, {"NONE", "2026-11-08"}, {"LOW", null}, {"MEDIUM", "2026-11-09"}, {"HIGH", "2026-11-08"},
        };
        for (int i = 0; i < specs.length; i++) {
            if (i % 3 != 2) {
                clock.advance(Duration.ofSeconds(1)); // một số Task cùng createdAt để thử tiêu chí phụ id
            }
            List<Object> extra = new ArrayList<>(List.of("priority", specs[i][0]));
            if (specs[i][1] != null) {
                extra.addAll(List.of("dueDate", specs[i][1]));
            }
            task(user, extra.toArray());
        }
        List<Map<String, Object>> all = walk(user, "&sort=createdAt:asc", 200);
        assertThat(all).hasSize(specs.length);

        for (String field : List.of("dueDate", "priority", "createdAt", "updatedAt")) {
            for (boolean asc : List.of(true, false)) {
                String sort = field + ":" + (asc ? "asc" : "desc");
                List<Map<String, Object>> expectedOrder = new ArrayList<>(all);
                expectedOrder.sort(expected(field, asc));
                for (int limit : List.of(1, 2, 4)) {
                    assertThat(ids(walk(user, "&sort=" + sort, limit))).as(sort + " limit " + limit)
                            .containsExactlyElementsOf(ids(expectedOrder));
                }
            }
        }
        // Mặc định = createdAt:desc.
        List<Map<String, Object>> byDefault = new ArrayList<>(all);
        byDefault.sort(expected("createdAt", false));
        assertThat(ids(walk(user, "", 3))).containsExactlyElementsOf(ids(byDefault));
    }

    @Test
    void filtersAreCombinedWithAnd() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        UUID category = category(user);
        UUID inProject = task(user, "projectId", project.toString(), "priority", "HIGH", "dueDate", "2026-11-07");
        UUID inboxDone = task(user, "status", "DONE", "categoryId", category.toString(), "dueDate", "2026-11-09");
        UUID inboxOpen = task(user, "priority", "HIGH");
        UUID deleted = task(user, "projectId", project.toString());
        delete(user, TASKS + "/" + deleted + "?version=1");
        task(newUser(), "priority", "HIGH"); // của user khác

        assertThat(ids(walk(user, "", 50))).containsExactlyInAnyOrder(inProject.toString(), inboxDone.toString(),
                inboxOpen.toString());
        assertThat(ids(walk(user, "&status=DONE", 50))).containsExactly(inboxDone.toString());
        assertThat(ids(walk(user, "&projectId=" + project, 50))).containsExactly(inProject.toString());
        assertThat(ids(walk(user, "&inbox=true", 50)))
                .containsExactlyInAnyOrder(inboxDone.toString(), inboxOpen.toString());
        assertThat(ids(walk(user, "&inbox=false", 50))).containsExactly(inProject.toString());
        assertThat(ids(walk(user, "&categoryId=" + category, 50))).containsExactly(inboxDone.toString());
        assertThat(ids(walk(user, "&priority=HIGH", 50)))
                .containsExactlyInAnyOrder(inProject.toString(), inboxOpen.toString());
        assertThat(ids(walk(user, "&priority=HIGH&inbox=true", 50))).containsExactly(inboxOpen.toString());
        // dueFrom/dueTo bao gồm hai đầu; Task không có hạn bị loại khi lọc theo hạn.
        assertThat(ids(walk(user, "&dueFrom=2026-11-07&dueTo=2026-11-09", 50)))
                .containsExactlyInAnyOrder(inProject.toString(), inboxDone.toString());
        assertThat(ids(walk(user, "&dueFrom=2026-11-08", 50))).containsExactly(inboxDone.toString());
        assertThat(ids(walk(user, "&dueTo=2026-11-07", 50))).containsExactly(inProject.toString());
        // Lọc theo Project của user khác: rỗng (không lộ gì).
        assertThat(walk(newUser(), "&projectId=" + project, 50)).isEmpty();
    }

    @Test
    void tasksOfArchivedProjectsAreNotHidden() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        UUID task = task(user, "projectId", project.toString());
        post(user, PROJECTS + "/" + project + "/archive", body("version", 1));

        assertThat(ids(walk(user, "", 50))).as("D78: REST không tự ẩn").containsExactly(task.toString());
    }

    @Test
    void invalidParametersAreValidationFailed() throws Exception {
        TestUser user = newUser();
        UUID project = project(user);
        Object[][] cases = {
            {"inbox=true&projectId=" + project, "inbox"},
            {"dueFrom=2026-11-10&dueTo=2026-11-01", "dueFrom"},
            {"dueFrom=10/11/2026", "dueFrom"},
            {"sort=title:asc", "sort"},
            {"sort=createdAt", "sort"},
            {"sort=createdAt:up", "sort"},
            {"status=DOING", "status"},
            {"priority=URGENT", "priority"},
            {"limit=0", "limit"},
            {"limit=201", "limit"},
            {"projectId=khong-phai-uuid", "projectId"},
            {"inbox=maybe", "inbox"},
            {"cursor=rác", "cursor"},
        };
        for (Object[] c : cases) {
            MvcResult result = get(user, TASKS + "?" + c[0]);
            assertThat(result.getResponse().getStatus()).as((String) c[0]).isEqualTo(400);
            assertThat(result.getResponse().getContentAsString()).as((String) c[0])
                    .contains("\"errorCode\":\"VALIDATION_FAILED\"").contains("\"field\":\"" + c[1] + "\"");
        }
        // Con trỏ của kiểu sắp xếp khác không dùng được.
        task(user);
        task(user);
        String cursor = (String) readJson(get(user, TASKS + "?limit=1&sort=dueDate:asc")).get("nextCursor");
        assertThat(get(user, TASKS + "?sort=priority:asc&cursor=" + cursor).getResponse().getContentAsString())
                .contains("\"field\":\"cursor\"");
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        assertThat(get(null, TASKS).getResponse().getStatus()).isEqualTo(401);
    }
}
