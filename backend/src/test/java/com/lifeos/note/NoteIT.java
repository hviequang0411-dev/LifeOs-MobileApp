package com.lifeos.note;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Ghi chú, thẻ, liên kết và tìm kiếm qua REST (PHASE_6 §6; KNW-01, 03, 04, 06, 14, 15). Ghi chú là dữ liệu riêng tư:
 * truy vấn tham số hóa, không injection tsquery.
 */
class NoteIT extends ApiTestBase {

    private static final String NOTES = "/api/v1/notes";

    private UUID note(TestUser user, String title, String content) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult r = post(user, NOTES, body("id", id.toString(), "title", title, "content", content));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    private UUID tag(TestUser user, String name) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult r = post(user, "/api/v1/tags", body("id", id.toString(), "name", name));
        assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    private List<String> ids(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        return items(result).stream().map(i -> (String) i.get("id")).toList();
    }

    private MvcResult putTags(TestUser user, UUID note, UUID... tags) throws Exception {
        return call(user, HttpMethod.PUT, NOTES + "/" + note + "/tags", body("tagIds",
                List.of(tags).stream().map(UUID::toString).toList()));
    }

    @Test
    void Knw_01_contentLimitCountsCodePointsAndNormalizesToNfc() throws Exception {
        TestUser user = newUser();
        String emoji = "😀";
        String max = emoji.repeat(50_000);
        MvcResult ok = post(user, NOTES, body("id", UUID.randomUUID().toString(), "content", max));
        assertThat(ok.getResponse().getStatus()).as("50 000 code point (100 000 UTF-16)").isEqualTo(201);
        MvcResult over = post(user, NOTES, body("id", UUID.randomUUID().toString(), "content", max + "a"));
        assertThat(over.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(over)).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(readJson(over).toString()).contains("content").contains("MAX_LENGTH");

        String decomposed = Normalizer.normalize("Hóa đơn điện", Normalizer.Form.NFD);
        UUID id = UUID.randomUUID();
        Map<String, Object> created = readJson(post(user, NOTES, body("id", id.toString(), "title", decomposed,
                "content", decomposed + "\r\ndòng 2")));
        assertThat(created).containsEntry("title", "Hóa đơn điện").containsEntry("content", "Hóa đơn điện\ndòng 2");
        assertThat(readJson(post(user, NOTES, body("id", UUID.randomUUID().toString(), "title", "t".repeat(201)))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
        Map<String, Object> empty = readJson(post(user, NOTES, body("id", UUID.randomUUID().toString())));
        assertThat(empty).as("quick capture: ghi chú rỗng").containsEntry("title", null).containsEntry("content", "");

        Map<String, Object> listed = items(get(user, NOTES)).stream().filter(n -> id.toString().equals(n.get("id")))
                .findFirst().orElseThrow();
        assertThat(listed).doesNotContainKey("content").containsEntry("contentLength", 19);
        Map<String, Object> big = items(get(user, NOTES)).stream().filter(n -> ((Number) n.get("contentLength"))
                .intValue() == 50_000).findFirst().orElseThrow();
        assertThat((String) big.get("excerpt")).hasSize(400).as("200 code point emoji = 400 UTF-16");
    }

    @Test
    void Knw_03_fullTextSearchIsAccentInsensitiveAndSafe() throws Exception {
        TestUser user = newUser();
        UUID electric = note(user, "Hóa đơn điện", "Tháng 11 thanh toán qua ngân hàng");
        UUID water = note(user, null, "Hóa đơn nước tháng 11");
        UUID other = note(user, "Đi chợ", "rau, thịt");
        UUID tagged = note(user, "Không chứa từ", "nội dung khác");
        UUID work = tag(user, "Điện-lực");
        assertThat(putTags(user, tagged, work).getResponse().getStatus()).isEqualTo(200);

        assertThat(ids(get(user, NOTES + "?q=hoa don dien"))).as("không dấu tìm có dấu").first()
                .isEqualTo(electric.toString());
        assertThat(ids(get(user, NOTES + "?q=hoa don dien"))).doesNotContain(other.toString());
        assertThat(ids(get(user, NOTES + "?q=hóa đơn"))).contains(electric.toString(), water.toString())
                .doesNotContain(other.toString());
        assertThat(ids(get(user, NOTES + "?q=\"đơn nước\""))).containsExactly(water.toString());
        assertThat(ids(get(user, NOTES + "?q=hoa -nuoc"))).contains(electric.toString())
                .doesNotContain(water.toString());
        assertThat(ids(get(user, NOTES + "?q=dien-luc"))).as("tìm theo tên thẻ").contains(tagged.toString());

        for (String hostile : List.of("':* | !", "a & | b", "(((", "\\'; DROP TABLE notes; --", "<->", "!!!",
                "x".repeat(100))) {
            MvcResult r = get(user, NOTES + "?q=" + hostile);
            assertThat(r.getResponse().getStatus()).as(hostile).isEqualTo(200);
        }
        MvcResult tooLong = get(user, NOTES + "?q=" + "x".repeat(101));
        assertThat(tooLong.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(tooLong)).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(jdbc.sql("SELECT count(*) FROM notes WHERE user_id = :u").param("u", user.id())
                .query(Integer.class).single()).isEqualTo(4);

        TestUser stranger = newUser();
        assertThat(ids(get(stranger, NOTES + "?q=hoa don dien"))).as("không lộ ghi chú người khác").isEmpty();
    }

    @Test
    void Knw_04_rankingAndPaginationAreStableAndComplete() throws Exception {
        TestUser user = newUser();
        Set<String> expected = new LinkedHashSet<>();
        for (int i = 0; i < 23; i++) {
            expected.add(note(user, i % 3 == 0 ? "lịch họp" : null, "họp nhóm " + i).toString());
        }
        note(user, null, "không liên quan");
        List<String> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            MvcResult page = get(user, NOTES + "?q=hop&limit=5" + (cursor == null ? "" : "&cursor=" + cursor));
            seen.addAll(ids(page));
            if (pages == 1) {
                note(user, null, "chen giữa không khớp");
            }
            cursor = (String) readJson(page).get("nextCursor");
            pages++;
        } while (cursor != null);
        assertThat(seen).as("không sót, không lặp").hasSize(23).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(seen.subList(0, 8)).as("khớp tiêu đề (trọng số A) xếp trước").allSatisfy(id -> assertThat(
                jdbc.sql("SELECT title IS NOT NULL FROM notes WHERE id = :id").param("id", UUID.fromString(id))
                        .query(Boolean.class).single()).isTrue());

        List<String> listed = new ArrayList<>();
        cursor = null;
        do {
            MvcResult page = get(user, NOTES + "?limit=7" + (cursor == null ? "" : "&cursor=" + cursor));
            listed.addAll(ids(page));
            cursor = (String) readJson(page).get("nextCursor");
        } while (cursor != null);
        assertThat(listed).hasSize(25).doesNotHaveDuplicates();
        assertThat(get(user, NOTES + "?q=hop&cursor=" + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("rank:offset\n1001\n" + UUID.randomUUID()).getBytes())).getResponse().getStatus())
                .as("offset tối đa 1000").isEqualTo(400);
    }

    @Test
    void Knw_06_tagsAreUniqueIgnoringCaseLimitedTo20AndDeletionCascades() throws Exception {
        TestUser user = newUser();
        UUID work = tag(user, "Công việc");
        MvcResult dup = post(user, "/api/v1/tags", body("id", UUID.randomUUID().toString(), "name", " CÔNG VIỆC "));
        assertThat(dup.getResponse().getStatus()).isEqualTo(409);
        assertThat(readJson(dup)).containsEntry("errorCode", "TAG_NAME_TAKEN");
        UUID home = tag(user, "Nhà");
        assertThat(readJson(patch(user, "/api/v1/tags/" + home, body("version", 1, "name", "công VIỆC"))))
                .containsEntry("errorCode", "TAG_NAME_TAKEN");
        assertThat(readJson(post(user, "/api/v1/tags", body("id", UUID.randomUUID().toString(), "name",
                "t".repeat(31))))).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(readJson(post(user, "/api/v1/tags", body("id", UUID.randomUUID().toString(), "name", "x",
                "color", "red")))).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(readJson(patch(user, "/api/v1/tags/" + home, body("version", 1, "color", "#a1b2c3"))))
                .containsEntry("color", "#A1B2C3");

        UUID a = note(user, "A", "a");
        UUID b = note(user, "B", "b");
        List<UUID> many = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            many.add(tag(user, "T" + i));
        }
        MvcResult tooMany = putTags(user, a, many.toArray(new UUID[0]));
        assertThat(tooMany.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(tooMany)).containsEntry("errorCode", "NOTE_TAG_LIMIT_EXCEEDED");
        assertThat(putTags(user, a, many.subList(0, 20).toArray(new UUID[0])).getResponse().getStatus())
                .isEqualTo(200);
        assertThat(putTags(user, a, work, home).getResponse().getStatus()).isEqualTo(200);
        assertThat(putTags(user, b, work).getResponse().getStatus()).isEqualTo(200);
        assertThat(readJson(putTags(user, b, UUID.randomUUID()))).containsEntry("errorCode", "TAG_NOT_FOUND");

        assertThat(ids(get(user, NOTES + "?tagId=" + work + "&tagId=" + home))).as("AND")
                .containsExactly(a.toString());
        assertThat(ids(get(user, NOTES + "?tagId=" + work + "&tagId=" + home + "&tagMode=any"))).as("OR")
                .containsExactlyInAnyOrder(a.toString(), b.toString());
        assertThat(readJson(get(user, NOTES + "?tagId=" + UUID.randomUUID()))).containsEntry("errorCode",
                "TAG_NOT_FOUND");
        assertThat(jdbc.sql("SELECT count(*) FROM note_tags WHERE note_id = :n AND deleted_at IS NULL")
                .param("n", a).query(Integer.class).single()).isEqualTo(2);

        assertThat(delete(user, "/api/v1/tags/" + work + "?version=1").getResponse().getStatus()).isEqualTo(204);
        assertThat(jdbc.sql("SELECT count(*) FROM note_tags WHERE tag_id = :t AND deleted_at IS NULL")
                .param("t", work).query(Integer.class).single()).as("gỡ khỏi mọi ghi chú").isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u AND entity_type = 'note_tag'"
                + " AND operation = 'DELETE' AND mutation_id IS NULL").param("u", user.id()).query(Integer.class)
                .single()).isGreaterThanOrEqualTo(2);
        assertThat(tag(user, "công việc")).as("tên được dùng lại sau khi xóa").isNotNull();
        assertThat(get(user, NOTES + "/" + a).getResponse().getStatus()).isEqualTo(200);
        assertThat((List<?>) readJson(get(user, NOTES + "/" + a)).get("tags")).hasSize(1);
    }

    @Test
    void Knw_14_noteLinksCheckOwnershipAndAreCascadedWithTheirTarget() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID n = note(user, "Ghi chú", "liên kết");
        UUID task = UUID.randomUUID();
        assertThat(post(user, "/api/v1/tasks", body("id", task.toString(), "title", "Việc")).getResponse()
                .getStatus()).isEqualTo(201);
        UUID goal = UUID.randomUUID();
        assertThat(post(user, "/api/v1/goals", body("id", goal.toString(), "title", "Mục tiêu")).getResponse()
                .getStatus()).isEqualTo(201);
        UUID foreignTask = UUID.randomUUID();
        assertThat(post(other, "/api/v1/tasks", body("id", foreignTask.toString(), "title", "Của người khác"))
                .getResponse().getStatus()).isEqualTo(201);

        MvcResult foreign = post(user, NOTES + "/" + n + "/links", body("id", UUID.randomUUID().toString(),
                "targetType", "TASK", "targetId", foreignTask.toString()));
        assertThat(foreign.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(foreign)).containsEntry("errorCode", "TASK_NOT_FOUND");
        assertThat(readJson(post(user, NOTES + "/" + n + "/links", body("id", UUID.randomUUID().toString(),
                "targetType", "HABIT", "targetId", task.toString())))).containsEntry("errorCode", "VALIDATION_FAILED");

        UUID toTask = UUID.randomUUID();
        assertThat(post(user, NOTES + "/" + n + "/links", body("id", toTask.toString(), "targetType", "TASK",
                "targetId", task.toString())).getResponse().getStatus()).isEqualTo(201);
        MvcResult again = post(user, NOTES + "/" + n + "/links", body("id", UUID.randomUUID().toString(),
                "targetType", "TASK", "targetId", task.toString()));
        assertThat(again.getResponse().getStatus()).as("trùng: trả bản đã có").isEqualTo(200);
        assertThat(readJson(again)).containsEntry("id", toTask.toString());
        UUID toGoal = UUID.randomUUID();
        assertThat(post(user, NOTES + "/" + n + "/links", body("id", toGoal.toString(), "targetType", "GOAL",
                "targetId", goal.toString())).getResponse().getStatus()).isEqualTo(201);
        assertThat(ids(get(user, "/api/v1/goals/" + goal + "/notes"))).containsExactly(n.toString());
        assertThat((List<?>) readJson(get(user, NOTES + "/" + n)).get("links")).hasSize(2);

        assertThat(delete(user, "/api/v1/tasks/" + task + "?version=1").getResponse().getStatus()).isEqualTo(204);
        assertThat(jdbc.sql("SELECT deleted_at IS NOT NULL FROM note_links WHERE id = :id").param("id", toTask)
                .query(Boolean.class).single()).isTrue();
        assertThat(changeLogs(user.id(), "note_link", toTask, "DELETE")).isEqualTo(1);
        assertThat(readJson(post(user, NOTES + "/" + n + "/links", body("id", UUID.randomUUID().toString(),
                "targetType", "TASK", "targetId", task.toString())))).as("mục tiêu đã xóa")
                .containsEntry("errorCode", "TASK_NOT_FOUND");

        assertThat(delete(user, "/api/v1/goals/" + goal + "?version=1").getResponse().getStatus()).isEqualTo(204);
        assertThat(jdbc.sql("SELECT deleted_at IS NOT NULL FROM note_links WHERE id = :id").param("id", toGoal)
                .query(Boolean.class).single()).isTrue();
        assertThat(get(user, "/api/v1/goals/" + goal + "/notes").getResponse().getStatus()).isEqualTo(404);

        UUID n2 = note(user, null, "x");
        UUID t2 = UUID.randomUUID();
        post(user, "/api/v1/tasks", body("id", t2.toString(), "title", "T2"));
        UUID link2 = UUID.randomUUID();
        post(user, NOTES + "/" + n2 + "/links", body("id", link2.toString(), "targetType", "TASK", "targetId",
                t2.toString()));
        assertThat(delete(user, NOTES + "/" + n + "/links/" + link2 + "?version=1").getResponse().getStatus())
                .as("liên kết phải thuộc ghi chú trong URL").isEqualTo(404);
        assertThat(delete(user, NOTES + "/" + n2 + "/links/" + link2 + "?version=1").getResponse().getStatus())
                .isEqualTo(204);
        assertThat(delete(user, NOTES + "/" + n2 + "?version=1").getResponse().getStatus()).isEqualTo(204);
    }

    @Test
    void Knw_15_pinArchiveAndServerFieldsCannotBeMassAssigned() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        MvcResult created = post(user, NOTES, body("id", id.toString(), "content", "x", "pinned", true,
                "archivedAt", "2020-01-01T00:00:00Z", "searchVector", "'x':1", "userId", UUID.randomUUID().toString()));
        assertThat(readJson(created)).containsEntry("pinned", false).containsEntry("archivedAt", null);
        assertThat(jdbc.sql("SELECT user_id FROM notes WHERE id = :id").param("id", id).query(UUID.class).single())
                .isEqualTo(user.id());

        assertThat(readJson(post(user, NOTES + "/" + id + "/pin", body("version", 1)))).containsEntry("pinned", true);
        assertThat(readJson(post(user, NOTES + "/" + id + "/archive", body("version", 2))).get("archivedAt"))
                .isNotNull();
        assertThat(ids(get(user, NOTES))).doesNotContain(id.toString());
        assertThat(ids(get(user, NOTES + "?archived=true"))).containsExactly(id.toString());
        assertThat(ids(get(user, NOTES + "?archived=all&pinned=true"))).containsExactly(id.toString());
        assertThat(ids(get(user, NOTES + "?q=x"))).as("lưu trữ ẩn khỏi tìm kiếm mặc định").isEmpty();
        assertThat(ids(get(user, NOTES + "?q=x&archived=all"))).containsExactly(id.toString());
        assertThat(readJson(post(user, NOTES + "/" + id + "/unarchive", body("version", 3)))).containsEntry(
                "archivedAt", null);

        for (int i = 0; i < 9; i++) {
            UUID n = note(user, null, "p" + i);
            assertThat(post(user, NOTES + "/" + n + "/pin", body("version", 1)).getResponse().getStatus())
                    .isEqualTo(200);
        }
        UUID eleventh = note(user, null, "p11");
        MvcResult over = post(user, NOTES + "/" + eleventh + "/pin", body("version", 1));
        assertThat(over.getResponse().getStatus()).isEqualTo(422);
        assertThat(readJson(over)).containsEntry("errorCode", "LIMIT_EXCEEDED");

        Map<String, Object> patched = readJson(patch(user, NOTES + "/" + id, body("version", 4, "content", "y",
                "pinned", false)));
        assertThat(patched).containsEntry("content", "y").containsEntry("pinned", true);
        assertThat(delete(user, NOTES + "/" + id + "?version=4").getResponse().getStatus()).isEqualTo(409);
        assertThat(delete(user, NOTES + "/" + id + "?version=5").getResponse().getStatus()).isEqualTo(204);
        assertThat(get(user, NOTES + "/" + id).getResponse().getStatus()).isEqualTo(404);
    }
}
