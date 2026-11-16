package com.lifeos.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.project.api.ProjectDeletionStarted;
import com.lifeos.support.ApiTestBase;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-PJ-01, 03, 04 qua REST (API_CONVENTIONS §8.5; D58, D62, D78, D80). Mỗi thay đổi thật: version +1 và đúng một dòng
 * change_logs; thao tác không đổi gì: không tăng version, không ghi change_logs. IDOR-01/02/03 và SEC-08 phần Project.
 */
@RecordApplicationEvents
class ProjectIT extends ApiTestBase {

    private static final String BASE = "/api/v1/projects";

    private MvcResult create(TestUser user, UUID id, String name, String description) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id.toString());
        body.put("name", name);
        body.put("description", description);
        return post(user, BASE, json.writeValueAsString(body));
    }

    private Map<String, Object> created(TestUser user, String name) throws Exception {
        MvcResult result = create(user, UUID.randomUUID(), name, null);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return readJson(result);
    }

    private static UUID id(Map<String, Object> resource) {
        return UUID.fromString((String) resource.get("id"));
    }

    private Map<String, Object> row(UUID id) {
        return jdbc.sql("SELECT * FROM projects WHERE id = :id").param("id", id).query().singleRow();
    }

    // ---------- Tạo

    @Test
    void createReturns201WithLocationAndRecordsOneCreate() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        clock.advance(Duration.ofSeconds(1));
        Instant now = clock.instant();

        MvcResult result = create(user, id, "  Nhà mới  ", "Mô tả giữ nguyên  ");

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(BASE + "/" + id);
        Map<String, Object> body = readJson(result);
        assertThat(body).containsOnlyKeys("id", "name", "description", "archivedAt", "version", "createdAt",
                "updatedAt");
        assertThat(body).containsEntry("id", id.toString()).containsEntry("name", "Nhà mới")
                .containsEntry("description", "Mô tả giữ nguyên  ").containsEntry("archivedAt", null);
        assertThat(version(body)).isEqualTo(1);
        assertThat(Instant.parse((String) body.get("createdAt"))).isEqualTo(now);
        assertThat(row(id).get("user_id")).isEqualTo(user.id());
        assertThat(changeLogs(user.id(), "project", id, "CREATE")).isEqualTo(1);
    }

    @Test
    void existingIdIsAConflictIndistinguishableAcrossOwnersAndTombstones() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        UUID aliceProject = id(created(alice, "A"));
        Map<String, Object> deleted = created(bob, "Đã xóa");
        assertThat(delete(bob, BASE + "/" + id(deleted) + "?version=1").getResponse().getStatus()).isEqualTo(204);

        MvcResult ownDuplicate = create(alice, aliceProject, "Lại", null);
        MvcResult othersId = create(bob, aliceProject, "Chiếm", null);       // IDOR-03
        MvcResult tombstone = create(bob, id(deleted), "Dùng lại", null);   // D78: id của tombstone không dùng lại

        for (MvcResult r : new MvcResult[] {ownDuplicate, othersId, tombstone}) {
            assertThat(r.getResponse().getStatus()).isEqualTo(409);
            assertThat(problem(r)).isEqualTo(problem(ownDuplicate)).containsEntry("errorCode", "ENTITY_ALREADY_EXISTS");
        }
        assertThat(row(aliceProject).get("name")).isEqualTo("A");
        assertThat(row(aliceProject).get("user_id")).isEqualTo(alice.id());
    }

    @Test
    void invalidValuesAreRejected() throws Exception {
        TestUser user = newUser();
        Object[][] cases = {
            {"   ", null, "name", "REQUIRED"},
            {null, null, "name", "REQUIRED"},
            {"x".repeat(101), null, "name", "SIZE"},
            {"Ok", "d".repeat(10_001), "description", "SIZE"},
        };
        for (Object[] c : cases) {
            UUID id = UUID.randomUUID();
            MvcResult result = create(user, id, (String) c[0], (String) c[1]);
            assertThat(result.getResponse().getStatus()).as(c[2] + "/" + c[3]).isEqualTo(400);
            assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"VALIDATION_FAILED\"")
                    .contains("\"field\":\"" + c[2] + "\"").contains("\"code\":\"" + c[3] + "\"");
            assertThat(jdbc.sql("SELECT count(*) FROM projects WHERE id = :id").param("id", id)
                    .query(Integer.class).single()).isZero();
        }
        // Đúng biên: 100 ký tự sau trim, mô tả 10.000.
        assertThat(create(user, UUID.randomUUID(), " " + "x".repeat(100) + " ", "d".repeat(10_000))
                .getResponse().getStatus()).isEqualTo(201);
        // Thiếu id → 400.
        assertThat(post(user, BASE, body("name", "Không id")).getResponse().getContentAsString())
                .contains("\"field\":\"id\"");
    }

    @Test
    void serverControlledFieldsCannotBeMassAssigned() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID id = UUID.randomUUID();

        MvcResult result = post(user, BASE, body("id", id.toString(), "name", "SEC-08",
                "userId", other.id().toString(), "version", 99, "archivedAt", "2020-01-01T00:00:00Z",
                "deletedAt", "2020-01-01T00:00:00Z", "createdAt", "2000-01-01T00:00:00Z"));

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        Map<String, Object> row = row(id);
        assertThat(row.get("user_id")).isEqualTo(user.id());
        assertThat(row.get("version")).isEqualTo(1L);
        assertThat(row.get("archived_at")).isNull();
        assertThat(row.get("deleted_at")).isNull();
        assertThat(readJson(result).get("createdAt")).isNotEqualTo("2000-01-01T00:00:00Z");
    }

    // ---------- Đọc

    @Test
    void anotherUsersProjectIsIndistinguishableFromAMissingOne() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        Map<String, Object> project = created(alice, "Riêng tư");
        String path = BASE + "/" + id(project);

        assertThat(get(alice, path).getResponse().getStatus()).isEqualTo(200);
        MvcResult foreign = get(bob, path);                                  // IDOR-01
        MvcResult missing = get(bob, BASE + "/" + UUID.randomUUID());
        assertThat(foreign.getResponse().getStatus()).isEqualTo(404);
        assertThat(problem(foreign)).isEqualTo(problem(missing)).containsEntry("errorCode", "PROJECT_NOT_FOUND");
        assertThat(foreign.getResponse().getContentAsString()).doesNotContain("Riêng tư");

        // IDOR-02: mọi thao tác ghi của Bob lên Project của Alice → 404, không đổi gì.
        assertThat(patch(bob, path, body("version", 1, "name", "Bị sửa")).getResponse().getStatus()).isEqualTo(404);
        assertThat(post(bob, path + "/archive", body("version", 1)).getResponse().getStatus()).isEqualTo(404);
        assertThat(delete(bob, path + "?version=1").getResponse().getStatus()).isEqualTo(404);
        Map<String, Object> row = row(id(project));
        assertThat(row.get("name")).isEqualTo("Riêng tư");
        assertThat(row.get("version")).isEqualTo(1L);
        assertThat(row.get("archived_at")).isNull();
        assertThat(row.get("deleted_at")).isNull();
    }

    // ---------- Sửa

    @Test
    void patchUpdatesBumpsVersionAndRecordsOneUpdate() throws Exception {
        TestUser user = newUser();
        Map<String, Object> project = created(user, "Cũ");
        UUID id = id(project);

        MvcResult result = patch(user, BASE + "/" + id, body("version", 1, "name", " Mới ", "description", "Mô tả"));

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> body = readJson(result);
        assertThat(body).containsEntry("name", "Mới").containsEntry("description", "Mô tả");
        assertThat(version(body)).isEqualTo(2);
        assertThat(changeLogs(user.id(), "project", id, "UPDATE")).isEqualTo(1);

        // description: null tường minh = xóa; name vắng = không đổi.
        Map<String, Object> clear = new LinkedHashMap<>();
        clear.put("version", 2);
        clear.put("description", null);
        Map<String, Object> cleared = readJson(patch(user, BASE + "/" + id, json.writeValueAsString(clear)));
        assertThat(cleared).containsEntry("description", null).containsEntry("name", "Mới");
        assertThat(version(cleared)).isEqualTo(3);
    }

    @Test
    void patchThatChangesNothingKeepsVersionAndRecordsNothing() throws Exception {
        TestUser user = newUser();
        UUID id = id(created(user, "Giữ"));

        MvcResult result = patch(user, BASE + "/" + id, body("version", 1, "name", "Giữ"));

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(version(readJson(result))).isEqualTo(1);
        assertThat(changeLogs(user.id(), "project", id, "UPDATE")).isZero();
    }

    @Test
    void patchRequiresVersionAndRejectsStaleOrInvalid() throws Exception {
        TestUser user = newUser();
        UUID id = id(created(user, "V"));
        String path = BASE + "/" + id;

        MvcResult missing = patch(user, path, body("name", "X"));
        assertThat(missing.getResponse().getStatus()).isEqualTo(400);
        assertThat(missing.getResponse().getContentAsString()).contains("\"field\":\"version\"")
                .contains("\"code\":\"REQUIRED\"");

        patch(user, path, body("version", 1, "name", "V2")); // → version 2
        MvcResult stale = patch(user, path, body("version", 1, "name", "V3"));
        assertThat(stale.getResponse().getStatus()).isEqualTo(409);
        assertThat(readJson(stale)).containsEntry("errorCode", "ENTITY_VERSION_CONFLICT")
                .containsEntry("currentVersion", 2);

        Map<String, Object> nullName = new LinkedHashMap<>();
        nullName.put("version", 2);
        nullName.put("name", null);
        assertThat(patch(user, path, json.writeValueAsString(nullName)).getResponse().getContentAsString())
                .contains("\"field\":\"name\"").contains("\"code\":\"REQUIRED\"");
        assertThat(row(id).get("name")).isEqualTo("V2");
        assertThat(changeLogs(user.id(), "project", id, "UPDATE")).isEqualTo(1);
    }

    // ---------- Lưu trữ

    @Test
    void archiveAndUnarchiveAreVersionedAndIdempotent() throws Exception {
        TestUser user = newUser();
        UUID id = id(created(user, "Lưu trữ"));
        String path = BASE + "/" + id;

        Map<String, Object> archived = readJson(post(user, path + "/archive", body("version", 1)));
        assertThat(archived.get("archivedAt")).isNotNull();
        assertThat(version(archived)).isEqualTo(2);

        // Lưu trữ lần nữa: kiểm version rồi 200, không tăng version, không ghi change_logs (D78).
        MvcResult again = post(user, path + "/archive", body("version", 2));
        assertThat(again.getResponse().getStatus()).isEqualTo(200);
        assertThat(version(readJson(again))).isEqualTo(2);
        assertThat(post(user, path + "/archive", body("version", 1)).getResponse().getStatus()).isEqualTo(409);

        Map<String, Object> unarchived = readJson(post(user, path + "/unarchive", body("version", 2)));
        assertThat(unarchived.get("archivedAt")).isNull();
        assertThat(version(unarchived)).isEqualTo(3);
        assertThat(version(readJson(post(user, path + "/unarchive", body("version", 3))))).isEqualTo(3);

        assertThat(changeLogs(user.id(), "project", id, "UPDATE")).isEqualTo(2);
        assertThat(post(user, path + "/archive", "{}").getResponse().getContentAsString())
                .contains("\"field\":\"version\"");
    }

    // ---------- Xóa

    @Test
    void deleteSoftDeletesRecordsDeleteAndPublishesTheCascadeEvent(ApplicationEvents events) throws Exception {
        TestUser user = newUser();
        UUID id = id(created(user, "Xóa"));
        String path = BASE + "/" + id;

        MvcResult result = delete(user, path + "?version=1");

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        Map<String, Object> row = row(id);
        assertThat(row.get("deleted_at")).isNotNull();
        assertThat(row.get("version")).isEqualTo(2L);
        assertThat(jdbc.sql("SELECT entity_version FROM change_logs WHERE user_id = :u AND entity_id = :e"
                        + " AND operation = 'DELETE'").param("u", user.id()).param("e", id)
                .query(Long.class).single()).isEqualTo(2L);
        assertThat(events.stream(ProjectDeletionStarted.class))
                .containsExactly(new ProjectDeletionStarted(user.id(), id));

        // Đã xóa: đọc, sửa, xóa lại đều 404 (D78).
        assertThat(get(user, path).getResponse().getStatus()).isEqualTo(404);
        assertThat(patch(user, path, body("version", 2, "name", "Z")).getResponse().getStatus()).isEqualTo(404);
        assertThat(delete(user, path + "?version=2").getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void deleteRequiresTheCurrentVersion() throws Exception {
        TestUser user = newUser();
        UUID id = id(created(user, "Xóa có version"));
        String path = BASE + "/" + id;

        MvcResult missing = delete(user, path);
        assertThat(missing.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(missing)).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(missing.getResponse().getContentAsString()).contains("\"field\":\"version\"")
                .contains("\"code\":\"REQUIRED\"");

        MvcResult stale = delete(user, path + "?version=7");
        assertThat(stale.getResponse().getStatus()).isEqualTo(409);
        assertThat(readJson(stale)).containsEntry("currentVersion", 1);
        assertThat(row(id).get("deleted_at")).isNull();
        assertThat(changeLogs(user.id(), "project", id, "DELETE")).isZero();
    }

    @Test
    void endpointsRequireAuthenticationAndMalformedIdIsValidationFailed() throws Exception {
        assertThat(get(null, BASE).getResponse().getStatus()).isEqualTo(401);
        TestUser user = newUser();
        MvcResult badId = get(user, BASE + "/khong-phai-uuid");
        assertThat(badId.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(badId)).containsEntry("errorCode", "VALIDATION_FAILED");
    }
}
