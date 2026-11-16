package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.sync.dto.SnapshotResponse;
import com.lifeos.sync.service.SyncSnapshotService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Đợt sửa backend 1 (BACKEND_FIX_REPORT): F4 — trang snapshot giữ trần 1 MB kể cả với ghi chú lớn (D174); F9 — khóa
 * HMAC của snapshotToken lấy từ cấu hình nên token còn hiệu lực sau khi tiến trình khởi động lại (D151).
 */
class SnapshotFixIT extends SyncTestBase {

    private static final int MAX_PAGE_BYTES = 1_000_000;

    @Test
    @SuppressWarnings("unchecked")
    void F4_snapshotPagesStayUnderOneMegabyteWithoutLosingOrRepeatingItems() throws Exception {
        TestUser user = newUser();
        String content = "ghi chú dài ".repeat(4000); // 48.000 ký tự ≈ 56 KB UTF-8 mỗi ghi chú
        Set<UUID> notes = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            UUID id = UUID.randomUUID();
            MvcResult created = post(user, "/api/v1/notes", body("id", id.toString(), "content", content));
            assertThat(created.getResponse().getStatus()).as(created.getResponse().getContentAsString())
                    .isEqualTo(201);
            notes.add(id);
        }
        List<UUID> seen = new ArrayList<>();
        List<Integer> sizes = new ArrayList<>();
        String token = null;
        String next = null;
        do {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("limit", 1000);
            if (token != null) {
                body.put("snapshotToken", token);
                body.put("next", next);
            }
            MvcResult r = post(user, "/api/v1/sync/snapshot", json.writeValueAsString(body));
            assertThat(r.getResponse().getStatus()).isEqualTo(200);
            sizes.add(r.getResponse().getContentAsByteArray().length);
            Map<String, Object> page = readJson(r);
            token = (String) page.get("snapshotToken");
            next = (String) page.get("next");
            for (Map<String, Object> entity : (List<Map<String, Object>>) page.get("entities")) {
                if ("note".equals(entity.get("type"))) {
                    for (Map<String, Object> item : (List<Map<String, Object>>) entity.get("items")) {
                        seen.add(UUID.fromString((String) item.get("entityId")));
                    }
                }
            }
            assertThat(sizes.size()).as("phân trang phải kết thúc").isLessThan(100);
        } while (next != null);

        assertThat(sizes).as("mọi trang ≤ 1 MB (D174)").allMatch(size -> size <= MAX_PAGE_BYTES);
        assertThat(sizes.size()).as("≈ 2,2 MB dữ liệu phải chia nhiều trang").isGreaterThanOrEqualTo(3);
        assertThat(seen).as("không lặp").doesNotHaveDuplicates();
        assertThat(new HashSet<>(seen)).as("không mất").isEqualTo(notes);
    }

    @Test
    void F9_aSnapshotTokenSurvivesAProcessRestart() throws Exception {
        TestUser user = newUser();
        createProject(user);
        createProject(user);
        Map<String, Object> first = readJson(post(user, "/api/v1/sync/snapshot", "{\"limit\":1}"));
        String token = (String) first.get("snapshotToken");
        String next = (String) first.get("next");
        assertThat(next).isNotNull();

        // Một instance mới của service = tiến trình sau khi khởi động lại / instance khác khi rolling deploy.
        SyncSnapshotService restarted = (SyncSnapshotService) context.getAutowireCapableBeanFactory()
                .createBean(SyncSnapshotService.class);
        SnapshotResponse page = restarted.page(user.id(), token, next, 10);
        assertThat(page.snapshotSeq()).isEqualTo(((Number) first.get("snapshotSeq")).longValue());
        assertThat(page.entities()).isNotEmpty();
    }
}
