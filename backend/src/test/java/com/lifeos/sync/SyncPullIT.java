package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * GET /sync/pull (SYSTEM_ARCHITECTURE §3.7, §4.2; SYNC_PROTOCOL §6, D89): SYN-05, 10, 16, 20, 39, 40, 41, 43.
 */
class SyncPullIT extends SyncTestBase {

    private static final String PULL = "/api/v1/sync/pull";

    @Autowired
    DataSource dataSource;

    private static String cursorOf(String decimal) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(decimal.getBytes(StandardCharsets.US_ASCII));
    }

    private Map<String, Object> pull(TestUser user, String query) throws Exception {
        MvcResult result = get(user, PULL + query);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        return readJson(result);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> changes(Map<String, Object> page) {
        return (List<Map<String, Object>>) page.get("changes");
    }

    @Test
    void Sync_16_firstPullReturnsOwnPushedChangesAsUpsertsInSeqOrder() throws Exception {
        TestUser user = newUser();
        UUID project = createProject(user);
        UUID category = createCategory(user);

        Map<String, Object> page = pull(user, "");

        assertThat(page).containsEntry("hasMore", false).containsEntry("nextCursor", cursorOf("2"));
        assertThat(changes(page)).extracting(c -> c.get("entityType") + ":" + c.get("entityId") + ":" + c.get("operation")
                        + ":" + c.get("version"))
                .containsExactly("project:" + project + ":UPSERT:1", "category:" + category + ":UPSERT:1");
        assertThat(changes(page).get(0)).containsOnlyKeys("entityType", "entityId", "operation", "version", "data");
    }

    @Test
    void Sync_05_deletedEntityIsATombstoneWithVersionAndNoData() throws Exception {
        TestUser user = newUser();
        UUID task = UUID.randomUUID();
        pushOne(user, mutation("task", task, "CREATE", 0, payload("title", "T")));
        pushOne(user, mutation("task", task, "DELETE", 1, null));

        Map<String, Object> page = pull(user, "?cursor=" + cursorOf("1"));

        assertThat(changes(page)).singleElement().isEqualTo(Map.of("entityType", "task", "entityId", task.toString(),
                "operation", "DELETE", "version", 2));
    }

    @Test
    void Sync_41_createThenDeleteInOnePageYieldsOneTombstone() throws Exception {
        TestUser user = newUser();
        UUID task = UUID.randomUUID();
        pushOne(user, mutation("task", task, "CREATE", 0, payload("title", "T")));
        pushOne(user, mutation("task", task, "UPDATE", 1, payload("title", "T2")));
        pushOne(user, mutation("task", task, "DELETE", 2, null));

        Map<String, Object> page = pull(user, "");

        assertThat(changes(page)).singleElement().satisfies(c -> assertThat(c)
                .containsEntry("operation", "DELETE").containsEntry("version", 3).doesNotContainKey("data"));
        assertThat(page).containsEntry("nextCursor", cursorOf("3"));
    }

    @Test
    void Sync_10_pagingVisitsEverySeqOnceWithMonotonicCursorsAndCollapsedEntitiesUseTheirLastSeq()
            throws Exception {
        TestUser user = newUser();
        List<UUID> projects = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            projects.add(createProject(user));
        }
        pushOne(user, mutation("project", projects.get(0), "UPDATE", 1, payload("name", "Đổi"))); // seq 8

        List<String> order = new ArrayList<>();
        String cursor = "";
        long previous = 0;
        int pages = 0;
        boolean hasMore = true;
        while (hasMore) {
            Map<String, Object> page = pull(user, "?limit=3&cursor=" + cursor);
            pages++;
            List<String> ids = changes(page).stream().map(c -> (String) c.get("entityId")).toList();
            assertThat(ids).as("không lặp trong cùng trang").doesNotHaveDuplicates();
            order.addAll(ids);
            cursor = (String) page.get("nextCursor");
            long seq = Long.parseLong(new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.US_ASCII));
            assertThat(seq).as("nextCursor đơn điệu").isGreaterThan(previous);
            previous = seq;
            hasMore = (Boolean) page.get("hasMore");
        }

        assertThat(pages).isEqualTo(3);
        assertThat(previous).isEqualTo(8);
        // Project 0 đổi ở seq 8 nên xuất hiện ở trang 1 (seq 1) VÀ trang 3 (seq 8, trạng thái mới); không sót project nào.
        assertThat(order).containsAll(projects.stream().map(UUID::toString).toList());
        assertThat(order.get(order.size() - 1)).isEqualTo(projects.get(0).toString());
        Map<String, Object> last = changes(pull(user, "?cursor=" + cursorOf("7"))).get(0);
        assertThat(last).containsEntry("version", 2);
        Map<String, Object> end = pull(user, "?cursor=" + cursor);
        assertThat(end).containsEntry("hasMore", false).containsEntry("nextCursor", cursor);
        assertThat(changes(end)).isEmpty();
    }

    @Test
    void Sync_20_otherUsersDataNeverAppearsEvenWithInterleavedWrites() throws Exception {
        TestUser a = newUser();
        TestUser b = newUser();
        Set<String> aIds = new HashSet<>();
        Set<String> bIds = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            aIds.add(createProject(a).toString());
            bIds.add(createCategory(b).toString());
        }

        List<Map<String, Object>> bChanges = changes(pull(b, "?limit=500"));

        assertThat(bChanges).extracting(c -> (String) c.get("entityId")).containsExactlyInAnyOrderElementsOf(bIds)
                .doesNotContainAnyElementsOf(aIds);
        // Cursor chỉ chứa seq: dùng cursor lớn của A cho B không lộ gì.
        assertThat(changes(pull(b, "?cursor=" + cursorOf("2")))).extracting(c -> (String) c.get("entityId"))
                .doesNotContainAnyElementsOf(aIds);
    }

    @Test
    void Sync_39_invalidCursorOrLimitIsValidationFailedWithTheRightField() throws Exception {
        TestUser user = newUser();
        Map<String, String> cases = Map.of(
                "?cursor=***", "cursor",
                "?cursor=" + cursorOf("-5"), "cursor",
                "?cursor=" + cursorOf("abc"), "cursor",
                "?cursor=" + cursorOf("99999999999999999999"), "cursor",
                "?limit=0", "limit",
                "?limit=501", "limit",
                "?limit=abc", "limit");
        for (Map.Entry<String, String> c : cases.entrySet()) {
            MvcResult result = get(user, PULL + c.getKey());
            assertThat(result.getResponse().getStatus()).as(c.getKey()).isEqualTo(400);
            Map<String, Object> problem = readJson(result);
            assertThat(problem).as(c.getKey()).containsEntry("errorCode", "VALIDATION_FAILED");
            assertThat(problem.get("fieldErrors").toString()).as(c.getKey()).contains("field=" + c.getValue());
        }
        assertThat(get(user, PULL + "?limit=500").getResponse().getStatus()).isEqualTo(200);
        assertThat(get(user, PULL + "?limit=1").getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void Sync_40_cursorBeyondLastSeqReturnsAnEmptyPageEchoingTheCursor() throws Exception {
        TestUser user = newUser();
        createProject(user);
        String beyond = cursorOf("1000");

        Map<String, Object> page = pull(user, "?cursor=" + beyond);

        assertThat(changes(page)).isEmpty();
        assertThat(page).containsEntry("hasMore", false).containsEntry("nextCursor", beyond);
        assertThat(pull(newUser(), "")).containsEntry("nextCursor", cursorOf("0"))
                .containsEntry("hasMore", false);
    }

    @Test
    void Sync_43_pullDoesNotWaitForTheUserSyncStateRowLockHeldByAWriter() throws Exception {
        TestUser user = newUser();
        UUID project = createProject(user);
        try (Connection writer = dataSource.getConnection()) {
            writer.setAutoCommit(false);
            try (PreparedStatement ps = writer.prepareStatement(
                    "UPDATE user_sync_state SET last_seq = last_seq WHERE user_id = ?")) {
                ps.setObject(1, user.id());
                ps.executeUpdate();
            }
            // Trong main context lock_timeout = 5s: nếu pull chờ khóa sẽ thành 503; ở đây phải trả ngay 200.
            long started = System.nanoTime();
            Map<String, Object> page = pull(user, "");
            assertThat((System.nanoTime() - started) / 1_000_000).isLessThan(4_000);
            assertThat(changes(page)).extracting(c -> c.get("entityId")).containsExactly(project.toString());
            writer.rollback();
        }
    }

    @Test
    void cascadedTasksAppearWithTheirNewVersionBeforeTheDeletedProject() throws Exception {
        TestUser user = newUser();
        UUID project = createProject(user);
        UUID task = UUID.randomUUID();
        pushOne(user, mutation("task", task, "CREATE", 0, payload("title", "T", "projectId", project.toString())));
        pushOne(user, mutation("project", project, "DELETE", 1, null));

        List<Map<String, Object>> changes = changes(pull(user, ""));

        assertThat(changes).extracting(c -> c.get("entityType") + ":" + c.get("operation") + ":" + c.get("version"))
                .containsExactly("task:UPSERT:2", "project:DELETE:2");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) changes.get(0).get("data");
        assertThat(data).containsEntry("projectId", null);
    }
}
