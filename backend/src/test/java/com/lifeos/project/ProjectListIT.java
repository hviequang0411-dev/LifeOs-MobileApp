package com.lifeos.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * GET /projects (API_CONVENTIONS §7, §8.5; D59, D79): phân trang keyset theo createdAt giảm dần rồi id giảm dần, lọc
 * archived, chỉ bản ghi chưa xóa và chỉ của user hiện tại; limit 1–200; con trỏ hỏng → 400.
 */
class ProjectListIT extends ApiTestBase {

    private static final String BASE = "/api/v1/projects";

    private UUID create(TestUser user, String name) throws Exception {
        UUID id = UUID.randomUUID();
        assertThat(post(user, BASE, body("id", id.toString(), "name", name)).getResponse().getStatus())
                .isEqualTo(201);
        return id;
    }

    /** Đi hết các trang; trả id theo thứ tự nhận được. */
    private List<String> walk(TestUser user, String query, int limit) throws Exception {
        List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            String url = BASE + "?limit=" + limit + query + (cursor == null ? "" : "&cursor=" + cursor);
            MvcResult result = get(user, url);
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
            Map<String, Object> page = readJson(result);
            items(result).forEach(p -> seen.add((String) p.get("id")));
            boolean hasMore = (Boolean) page.get("hasMore");
            cursor = (String) page.get("nextCursor");
            assertThat(cursor == null).isEqualTo(!hasMore);
            assertThat(++pages).as("không lặp vô hạn").isLessThan(50);
        } while (cursor != null);
        return seen;
    }

    @Test
    void pagesCoverEveryProjectOnceInCreatedAtDescThenIdDescOrder() throws Exception {
        TestUser user = newUser();
        List<UUID> expected = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            clock.advance(Duration.ofSeconds(1));
            expected.add(create(user, "P" + i));
        }
        // Ba Project cùng createdAt: thứ tự phụ theo id giảm dần.
        clock.advance(Duration.ofSeconds(1));
        List<UUID> sameInstant = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            sameInstant.add(create(user, "S" + i));
        }
        // PostgreSQL so uuid theo byte (không dấu) = thứ tự chuỗi hex chữ thường; UUID.compareTo của Java thì có dấu.
        List<String> order = new ArrayList<>(sameInstant.stream().map(UUID::toString)
                .sorted(java.util.Comparator.reverseOrder()).toList());
        for (int i = expected.size() - 1; i >= 0; i--) {
            order.add(expected.get(i).toString());
        }

        assertThat(walk(user, "", 2)).containsExactlyElementsOf(order);
        assertThat(walk(user, "", 200)).containsExactlyElementsOf(order);
    }

    @Test
    void archivedFilterAndDeletedAndForeignProjectsAreRespected() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID active = create(user, "Đang làm");
        UUID archived = create(user, "Lưu trữ");
        UUID deleted = create(user, "Đã xóa");
        create(other, "Của người khác");
        post(user, BASE + "/" + archived + "/archive", body("version", 1));
        delete(user, BASE + "/" + deleted + "?version=1");

        assertThat(walk(user, "", 50)).containsExactly(active.toString());
        assertThat(walk(user, "&archived=false", 50)).containsExactly(active.toString());
        assertThat(walk(user, "&archived=true", 50)).containsExactly(archived.toString());
        assertThat(walk(user, "&archived=all", 50))
                .containsExactlyInAnyOrder(active.toString(), archived.toString());
    }

    @Test
    void defaultLimitIs50AndOutOfRangeLimitIsRejected() throws Exception {
        TestUser user = newUser();
        for (int i = 0; i < 51; i++) {
            create(user, "N" + i);
        }
        MvcResult first = get(user, BASE);
        assertThat(items(first)).hasSize(50);
        assertThat(readJson(first)).containsEntry("hasMore", true);

        for (String limit : List.of("0", "201", "-1")) {
            MvcResult bad = get(user, BASE + "?limit=" + limit);
            assertThat(bad.getResponse().getStatus()).as(limit).isEqualTo(400);
            assertThat(bad.getResponse().getContentAsString()).contains("\"field\":\"limit\"");
        }
        assertThat(get(user, BASE + "?limit=abc").getResponse().getContentAsString())
                .contains("\"errorCode\":\"VALIDATION_FAILED\"").contains("\"field\":\"limit\"");
    }

    @Test
    void malformedCursorOrArchivedValueIsRejected() throws Exception {
        TestUser user = newUser();
        for (String cursor : List.of("rác", "dGFzazpkZXNj", "Y3JlYXRlZEF0OmRlc2MKeAp4")) {
            MvcResult bad = get(user, BASE + "?cursor=" + cursor);
            assertThat(bad.getResponse().getStatus()).as(cursor).isEqualTo(400);
            assertThat(bad.getResponse().getContentAsString()).contains("\"field\":\"cursor\"");
        }
        MvcResult archived = get(user, BASE + "?archived=maybe");
        assertThat(archived.getResponse().getStatus()).isEqualTo(400);
        assertThat(archived.getResponse().getContentAsString()).contains("\"field\":\"archived\"");
    }

    @Test
    void cursorOfAnotherUserCannotRevealTheirProjects() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        for (int i = 0; i < 3; i++) {
            create(alice, "A" + i);
        }
        String aliceCursor = (String) readJson(get(alice, BASE + "?limit=1")).get("nextCursor");

        MvcResult result = get(bob, BASE + "?cursor=" + aliceCursor);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(items(result)).isEmpty();
    }
}
