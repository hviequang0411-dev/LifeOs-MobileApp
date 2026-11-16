package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-CT-01 qua REST (API_CONVENTIONS §8.6; D25, D58, D62, D78): tên duy nhất theo user không phân biệt hoa thường, bỏ
 * qua bản đã xóa (TESTING_RULES §5.1); version; xóa mềm. IDOR-01/02/03 và SEC-08 phần Category.
 */
class CategoryIT extends ApiTestBase {

    private static final String BASE = "/api/v1/categories";

    private MvcResult create(TestUser user, UUID id, String name) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id.toString());
        body.put("name", name);
        return post(user, BASE, json.writeValueAsString(body));
    }

    private UUID created(TestUser user, String name) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult result = create(user, id, name);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    private Map<String, Object> row(UUID id) {
        return jdbc.sql("SELECT * FROM categories WHERE id = :id").param("id", id).query().singleRow();
    }

    private static void assertNameTaken(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"CATEGORY_NAME_TAKEN\"");
    }

    @Test
    void createReturns201WithTrimmedNameAndRecordsOneCreate() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();

        MvcResult result = create(user, id, "  Công việc  ");

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(BASE + "/" + id);
        Map<String, Object> body = readJson(result);
        assertThat(body).containsOnlyKeys("id", "name", "version", "createdAt", "updatedAt");
        assertThat(body).containsEntry("name", "Công việc");
        assertThat(version(body)).isEqualTo(1);
        assertThat(changeLogs(user.id(), "category", id, "CREATE")).isEqualTo(1);
    }

    @Test
    void nameIsUniquePerUserIgnoringCaseButReusableAfterDeleteAndAcrossUsers() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        UUID work = created(alice, "Work");

        assertNameTaken(create(alice, UUID.randomUUID(), "work"));
        assertNameTaken(create(alice, UUID.randomUUID(), "  WORK "));
        assertThat(create(bob, UUID.randomUUID(), "Work").getResponse().getStatus()).isEqualTo(201);

        assertThat(delete(alice, BASE + "/" + work + "?version=1").getResponse().getStatus()).isEqualTo(204);
        assertThat(create(alice, UUID.randomUUID(), "work").getResponse().getStatus())
                .as("tên dùng lại được sau khi xóa mềm").isEqualTo(201);
    }

    @Test
    void renameToAnotherCategorysNameIsTakenButChangingOwnCaseIsAllowed() throws Exception {
        TestUser user = newUser();
        UUID home = created(user, "Nhà");
        created(user, "Học");

        assertNameTaken(patch(user, BASE + "/" + home, body("version", 1, "name", "học")));
        assertThat(row(home).get("name")).isEqualTo("Nhà");

        MvcResult recased = patch(user, BASE + "/" + home, body("version", 1, "name", "NHÀ"));
        assertThat(recased.getResponse().getStatus()).isEqualTo(200);
        assertThat(readJson(recased)).containsEntry("name", "NHÀ");
        assertThat(version(readJson(recased))).isEqualTo(2);
        assertThat(changeLogs(user.id(), "category", home, "UPDATE")).isEqualTo(1);
    }

    @Test
    void invalidNamesAreRejected() throws Exception {
        TestUser user = newUser();
        Object[][] cases = {{"   ", "REQUIRED"}, {null, "REQUIRED"}, {"x".repeat(51), "SIZE"}};
        for (Object[] c : cases) {
            MvcResult result = create(user, UUID.randomUUID(), (String) c[0]);
            assertThat(result.getResponse().getStatus()).isEqualTo(400);
            assertThat(result.getResponse().getContentAsString()).contains("\"field\":\"name\"")
                    .contains("\"code\":\"" + c[1] + "\"");
        }
        assertThat(create(user, UUID.randomUUID(), " " + "y".repeat(50) + " ").getResponse().getStatus())
                .isEqualTo(201);
    }

    @Test
    void existingIdIsAConflictIndistinguishableAcrossOwnersAndTombstones() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        UUID aliceCategory = created(alice, "A");
        UUID tomb = created(bob, "Xóa");
        delete(bob, BASE + "/" + tomb + "?version=1");

        MvcResult own = create(alice, aliceCategory, "Khác");
        MvcResult foreign = create(bob, aliceCategory, "Chiếm");      // IDOR-03
        MvcResult tombstone = create(bob, tomb, "Dùng lại");           // D78
        for (MvcResult r : new MvcResult[] {own, foreign, tombstone}) {
            assertThat(r.getResponse().getStatus()).isEqualTo(409);
            assertThat(problem(r)).isEqualTo(problem(own)).containsEntry("errorCode", "ENTITY_ALREADY_EXISTS");
        }
        assertThat(row(aliceCategory).get("user_id")).isEqualTo(alice.id());
    }

    @Test
    void serverControlledFieldsCannotBeMassAssigned() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        MvcResult result = post(user, BASE, body("id", id.toString(), "name", "SEC-08",
                "userId", newUser().id().toString(), "version", 42, "deletedAt", "2020-01-01T00:00:00Z"));

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        Map<String, Object> row = row(id);
        assertThat(row.get("user_id")).isEqualTo(user.id());
        assertThat(row.get("version")).isEqualTo(1L);
        assertThat(row.get("deleted_at")).isNull();
    }

    @Test
    void anotherUsersCategoryIsIndistinguishableFromAMissingOne() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        UUID id = created(alice, "Riêng");
        String path = BASE + "/" + id;

        MvcResult foreign = get(bob, path);                                   // IDOR-01
        MvcResult missing = get(bob, BASE + "/" + UUID.randomUUID());
        assertThat(foreign.getResponse().getStatus()).isEqualTo(404);
        assertThat(problem(foreign)).isEqualTo(problem(missing)).containsEntry("errorCode", "CATEGORY_NOT_FOUND");

        assertThat(patch(bob, path, body("version", 1, "name", "Sửa")).getResponse().getStatus()).isEqualTo(404);
        assertThat(delete(bob, path + "?version=1").getResponse().getStatus()).isEqualTo(404);   // IDOR-02
        Map<String, Object> row = row(id);
        assertThat(row.get("name")).isEqualTo("Riêng");
        assertThat(row.get("version")).isEqualTo(1L);
        assertThat(row.get("deleted_at")).isNull();
    }

    @Test
    void patchIsVersionedAndNoOpDoesNotBumpVersion() throws Exception {
        TestUser user = newUser();
        UUID id = created(user, "V");
        String path = BASE + "/" + id;

        assertThat(patch(user, path, body("name", "X")).getResponse().getContentAsString())
                .contains("\"field\":\"version\"").contains("\"code\":\"REQUIRED\"");
        MvcResult same = patch(user, path, body("version", 1, "name", " V "));
        assertThat(version(readJson(same))).isEqualTo(1);
        assertThat(version(readJson(patch(user, path, body("version", 1))))).isEqualTo(1);
        assertThat(changeLogs(user.id(), "category", id, "UPDATE")).isZero();

        patch(user, path, body("version", 1, "name", "V2"));
        MvcResult stale = patch(user, path, body("version", 1, "name", "V3"));
        assertThat(stale.getResponse().getStatus()).isEqualTo(409);
        assertThat(readJson(stale)).containsEntry("errorCode", "ENTITY_VERSION_CONFLICT")
                .containsEntry("currentVersion", 2);

        Map<String, Object> nullName = new LinkedHashMap<>();
        nullName.put("version", 2);
        nullName.put("name", null);
        assertThat(patch(user, path, json.writeValueAsString(nullName)).getResponse().getContentAsString())
                .contains("\"field\":\"name\"").contains("\"code\":\"REQUIRED\"");
    }

    @Test
    void deleteIsVersionedSoftAndFinal() throws Exception {
        TestUser user = newUser();
        UUID id = created(user, "Xóa");
        String path = BASE + "/" + id;

        assertThat(delete(user, path).getResponse().getContentAsString()).contains("\"field\":\"version\"");
        assertThat(delete(user, path + "?version=3").getResponse().getStatus()).isEqualTo(409);
        assertThat(delete(user, path + "?version=1").getResponse().getStatus()).isEqualTo(204);

        Map<String, Object> row = row(id);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(row.get("version")).isEqualTo(2L);
        assertThat(changeLogs(user.id(), "category", id, "DELETE")).isEqualTo(1);
        assertThat(get(user, path).getResponse().getStatus()).isEqualTo(404);
        assertThat(delete(user, path + "?version=2").getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void listPagesOwnLiveCategoriesOnly() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID a = created(user, "A");
        clock.advance(java.time.Duration.ofSeconds(1));
        UUID b = created(user, "B");
        clock.advance(java.time.Duration.ofSeconds(1));
        UUID c = created(user, "C");
        UUID gone = created(user, "Gone");
        delete(user, BASE + "/" + gone + "?version=1");
        created(other, "Của người khác");

        MvcResult first = get(user, BASE + "?limit=2");
        assertThat(items(first)).extracting(m -> m.get("id")).containsExactly(c.toString(), b.toString());
        String cursor = (String) readJson(first).get("nextCursor");
        MvcResult second = get(user, BASE + "?limit=2&cursor=" + cursor);
        assertThat(items(second)).extracting(m -> m.get("id")).containsExactly(a.toString());
        assertThat(readJson(second)).containsEntry("hasMore", false).containsEntry("nextCursor", null);

        assertThat(get(user, BASE + "?limit=201").getResponse().getStatus()).isEqualTo(400);
        assertThat(get(user, BASE + "?cursor=rác").getResponse().getContentAsString()).contains("\"field\":\"cursor\"");
        assertThat(get(null, BASE).getResponse().getStatus()).isEqualTo(401);
    }
}
