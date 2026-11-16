package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * POST /sync/push với entityType project và category (bước 3a): ma trận kết quả §4, idempotency §2.2/§5, thứ tự kiểm
 * D93/D96, lưu kết quả T1/T2. Bộ Task/UserSettings ở 3b; cascade và đồng thời ở 3d.
 */
class SyncPushProjectCategoryIT extends SyncTestBase {

    // ---------- APPLIED / change_logs / mutationId

    @Test
    void Sync_01_createProjectIsAppliedWithVersion1OneChangeLogCarryingTheMutationId() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        Map<String, Object> m = mutation("project", id, "CREATE", 0, payload("name", "  Nhà  ", "description", "d"));

        Map<String, Object> r = pushOne(user, m);

        assertThat(r).containsOnlyKeys("mutationId", "status", "entityVersion")
                .containsEntry("status", "APPLIED").containsEntry("entityVersion", 1)
                .containsEntry("mutationId", m.get("mutationId"));
        Map<String, Object> project = row("projects", id);
        assertThat(project).containsEntry("name", "Nhà").containsEntry("user_id", user.id());
        assertThat(changeLogRows(user.id(), id)).singleElement().satisfies(log -> assertThat(log)
                .containsEntry("operation", "CREATE").containsEntry("entity_version", 1L)
                .containsEntry("mutation_id", mutationIdOf(m)));
        Map<String, Object> stored = jdbc.sql("SELECT status, result_version, entity_type, operation, base_version"
                        + " FROM sync_mutations WHERE user_id = :u AND mutation_id = :m")
                .param("u", user.id()).param("m", mutationIdOf(m)).query().singleRow();
        assertThat(stored).containsEntry("status", "APPLIED").containsEntry("result_version", 1L)
                .containsEntry("entity_type", "project").containsEntry("operation", "CREATE")
                .containsEntry("base_version", 0L);
    }

    @Test
    void Sync_03_updateWithMatchingBaseVersionIncrementsVersionAndArchivedIsOneWrite() throws Exception {
        TestUser user = newUser();
        UUID id = createProject(user);

        Map<String, Object> r = pushOne(user, mutation("project", id, "UPDATE", 1,
                payload("name", "Mới", "archived", true)));

        assertThat(r).containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
        Map<String, Object> project = row("projects", id);
        assertThat(project.get("name")).isEqualTo("Mới");
        assertThat(project.get("archived_at")).isNotNull();
        assertThat(changeLogRows(user.id(), id)).hasSize(2);

        assertThat(pushOne(user, mutation("project", id, "UPDATE", 2, payload("archived", false))))
                .containsEntry("entityVersion", 3);
        assertThat(row("projects", id).get("archived_at")).isNull();
    }

    @Test
    void Sync_26_updateThatChangesNothingIsAppliedWithoutVersionBumpOrChangeLog() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        pushOne(user, mutation("project", id, "CREATE", 0, payload("name", "Giữ", "description", "x")));

        Map<String, Object> r = pushOne(user, mutation("project", id, "UPDATE", 1,
                payload("name", "Giữ", "description", "x", "archived", false)));

        assertThat(r).containsEntry("status", "APPLIED").containsEntry("entityVersion", 1);
        assertThat(changeLogRows(user.id(), id)).hasSize(1);
    }

    @Test
    void Sync_27_absentKeyKeepsValueExplicitNullClearsNullableAndRejectsRequiredField() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        pushOne(user, mutation("project", id, "CREATE", 0, payload("name", "Tên", "description", "mô tả")));

        assertThat(pushOne(user, mutation("project", id, "UPDATE", 1, payload("name", "Tên 2"))))
                .containsEntry("entityVersion", 2);
        assertThat(row("projects", id).get("description")).as("vắng = giữ nguyên").isEqualTo("mô tả");

        assertThat(pushOne(user, mutation("project", id, "UPDATE", 2, payload("description", null))))
                .containsEntry("entityVersion", 3);
        assertThat(row("projects", id).get("description")).as("null = xóa").isNull();

        Map<String, Object> nullName = pushOne(user, mutation("project", id, "UPDATE", 3, payload("name", null)));
        assertThat(nullName).containsEntry("status", "REJECTED").containsEntry("errorCode", "VALIDATION_FAILED")
                .containsEntry("fieldErrors", List.of(Map.of("field", "name", "code", "REQUIRED")));
        assertThat(pushOne(user, mutation("project", id, "UPDATE", 3, payload("archived", null))))
                .containsEntry("errorCode", "VALIDATION_FAILED");
    }

    @Test
    void Sync_18_serverManagedKeysInPayloadAreIgnored() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        Map<String, Object> r = pushOne(user, mutation("project", id, "CREATE", 0, payload("name", "P",
                "id", UUID.randomUUID().toString(), "userId", UUID.randomUUID().toString(), "version", 99,
                "archivedAt", "2020-01-01T00:00:00Z", "createdAt", "2020-01-01T00:00:00Z",
                "updatedAt", "2020-01-01T00:00:00Z", "deletedAt", "2020-01-01T00:00:00Z")));

        assertThat(r).containsEntry("entityVersion", 1);
        Map<String, Object> project = row("projects", id);
        assertThat(project).containsEntry("user_id", user.id()).containsEntry("version", 1L);
        assertThat(project.get("archived_at")).isNull();
        assertThat(project.get("deleted_at")).isNull();
        assertThat(project.get("created_at").toString()).doesNotStartWith("2020");
    }

    // ---------- CONFLICT

    @Test
    void Sync_04_and_28_staleOrFutureBaseVersionIsConflictWithCurrentSnapshotAndNoWrite() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        pushOne(user, mutation("project", id, "CREATE", 0, payload("name", "Gốc", "description", "d")));
        pushOne(user, mutation("project", id, "UPDATE", 1, payload("name", "V2")));

        for (long base : new long[] {1, 7}) {
            Map<String, Object> r = pushOne(user, mutation("project", id, "UPDATE", base, payload("name", "Ghi đè")));
            assertThat(r).containsOnlyKeys("mutationId", "status", "serverSnapshot").containsEntry("status", "CONFLICT");
            @SuppressWarnings("unchecked")
            Map<String, Object> snapshot = (Map<String, Object>) r.get("serverSnapshot");
            assertThat(snapshot).containsEntry("entityType", "project").containsEntry("entityId", id.toString())
                    .containsEntry("version", 2).containsEntry("deleted", false);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) snapshot.get("data");
            assertThat(data).containsOnlyKeys("name", "description", "archivedAt", "createdAt", "updatedAt")
                    .containsEntry("name", "V2").containsEntry("description", "d");
        }
        assertThat(row("projects", id)).containsEntry("name", "V2").containsEntry("version", 2L);
        assertThat(changeLogRows(user.id(), id)).hasSize(2);
    }

    @Test
    void Sync_24_updateOfOwnTombstoneIsConflictWithDeletedSnapshot() throws Exception {
        TestUser user = newUser();
        UUID id = createProject(user);
        pushOne(user, mutation("project", id, "DELETE", 1, null));

        Map<String, Object> r = pushOne(user, mutation("project", id, "UPDATE", 1, payload("name", "Hồi sinh")));

        assertThat(r).containsEntry("status", "CONFLICT");
        assertThat(r.get("serverSnapshot")).isEqualTo(Map.of("entityType", "project", "entityId", id.toString(),
                "version", 2, "deleted", true));
    }

    // ---------- DELETE

    @Test
    void Sync_25_deleteOfOwnTombstoneIsAppliedWithoutChangesOrChangeLog() throws Exception {
        TestUser user = newUser();
        UUID id = createProject(user);
        assertThat(pushOne(user, mutation("project", id, "DELETE", 1, null))).containsEntry("entityVersion", 2);
        int logs = changeLogsOfUser(user.id());

        Map<String, Object> again = mutation("project", id, "DELETE", 1, null);
        Map<String, Object> r = pushOne(user, again);

        assertThat(r).containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
        assertThat(changeLogsOfUser(user.id())).isEqualTo(logs);
        assertThat(row("projects", id)).containsEntry("version", 2L);
        assertThat(syncMutations(user.id(), mutationIdOf(again))).isEqualTo(1);
    }

    @Test
    void deleteWithMatchingVersionSoftDeletesAndCarriesTheMutationId() throws Exception {
        TestUser user = newUser();
        UUID id = createCategory(user);
        Map<String, Object> m = mutation("category", id, "DELETE", 1, null);

        assertThat(pushOne(user, m)).containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);

        assertThat(row("categories", id).get("deleted_at")).isNotNull();
        assertThat(changeLogRows(user.id(), id)).last().satisfies(log -> assertThat(log)
                .containsEntry("operation", "DELETE").containsEntry("mutation_id", mutationIdOf(m)));
        assertThat(pushOne(user, mutation("category", createCategory(user), "DELETE", 5, null)))
                .containsEntry("status", "CONFLICT");
    }

    // ---------- REJECTED

    @Test
    void Sync_22_createWithUsedIdIsTheSameRejectionForLiveTombstoneAndOtherUsersEntity() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID live = createProject(user);
        UUID tombstone = createProject(user);
        pushOne(user, mutation("project", tombstone, "DELETE", 1, null));
        UUID foreign = createProject(other);

        List<Map<String, Object>> results = push(user,
                mutation("project", live, "CREATE", 0, payload("name", "X")),
                mutation("project", tombstone, "CREATE", 0, payload("name", "X")),
                mutation("project", foreign, "CREATE", 0, payload("name", "X")));

        assertThat(results).allSatisfy(r -> assertThat(r).containsOnlyKeys("mutationId", "status", "errorCode", "message")
                .containsEntry("status", "REJECTED").containsEntry("errorCode", "ENTITY_ALREADY_EXISTS"));
        assertThat(results).extracting(r -> r.get("message")).containsOnly(results.get(0).get("message"));
        assertThat(row("projects", foreign)).containsEntry("user_id", other.id()).containsEntry("version", 1L);
    }

    @Test
    void Sync_23_updateOrDeleteOfMissingOrForeignEntityIsTheSameNotFoundAndVictimIsUntouched() throws Exception {
        TestUser user = newUser();
        TestUser victim = newUser();
        UUID foreign = createCategory(victim);
        Map<String, Object> victimRow = row("categories", foreign);
        int victimLogs = changeLogsOfUser(victim.id());

        List<Map<String, Object>> results = push(user,
                mutation("category", foreign, "UPDATE", 1, payload("name", "Chiếm")),
                mutation("category", UUID.randomUUID(), "UPDATE", 1, payload("name", "Chiếm")),
                mutation("category", foreign, "DELETE", 1, null),
                mutation("category", UUID.randomUUID(), "DELETE", 1, null));

        assertThat(results).allSatisfy(r -> assertThat(r).containsOnlyKeys("mutationId", "status", "errorCode", "message")
                .containsEntry("status", "REJECTED").containsEntry("errorCode", "CATEGORY_NOT_FOUND"));
        assertThat(row("categories", foreign)).isEqualTo(victimRow);
        assertThat(changeLogsOfUser(victim.id())).isEqualTo(victimLogs);
    }

    @Test
    void Sync_09_unknownEntityTypeIsRejectedAndStoredWhileOthersProceed() throws Exception {
        TestUser user = newUser();
        Map<String, Object> unknown = mutation("no_such_type", UUID.randomUUID(), "CREATE", 0, payload("name", "x"));
        List<Map<String, Object>> results = push(user, unknown,
                mutation("project", UUID.randomUUID(), "CREATE", 0, payload("name", "P")));

        assertThat(results.get(0)).containsEntry("status", "REJECTED")
                .containsEntry("errorCode", "SYNC_UNKNOWN_ENTITY_TYPE");
        assertThat(results.get(1)).containsEntry("status", "APPLIED");
        assertThat(syncMutations(user.id(), mutationIdOf(unknown))).isEqualTo(1);
    }

    @Test
    void Sync_19_payloadOver64KbIsRejectedBeforeTheHandlerAndStored() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        Map<String, Object> big = mutation("project", id, "CREATE", 0,
                payload("name", "P", "description", "a".repeat(70_000)));

        assertThat(pushOne(user, big)).containsEntry("status", "REJECTED").containsEntry("errorCode", "PAYLOAD_TOO_LARGE");
        assertThat(jdbc.sql("SELECT count(*) FROM projects WHERE id = :id").param("id", id)
                .query(Integer.class).single()).isZero();
        assertThat(syncMutations(user.id(), mutationIdOf(big))).isEqualTo(1);
    }

    @Test
    void createWithNonZeroBaseVersionIsRejected() throws Exception {
        TestUser user = newUser();
        assertThat(pushOne(user, mutation("project", UUID.randomUUID(), "CREATE", 3, payload("name", "P"))))
                .containsEntry("errorCode", "VALIDATION_FAILED")
                .containsEntry("fieldErrors", List.of(Map.of("field", "baseVersion", "code", "INVALID_VALUE")));
    }

    @Test
    void validationAndBusinessRejectionsUseTheRestErrorCodes() throws Exception {
        TestUser user = newUser();
        UUID taken = createCategory(user);
        String takenName = (String) row("categories", taken).get("name");

        List<Map<String, Object>> results = push(user,
                mutation("project", UUID.randomUUID(), "CREATE", 0, payload()),
                mutation("project", UUID.randomUUID(), "CREATE", 0, payload("name", "x".repeat(101))),
                mutation("category", UUID.randomUUID(), "CREATE", 0, payload("name", takenName.toUpperCase())),
                mutation("category", UUID.randomUUID(), "CREATE", 0, payload("name", "  ")));

        assertThat(results.get(0)).containsEntry("errorCode", "VALIDATION_FAILED")
                .containsEntry("fieldErrors", List.of(Map.of("field", "name", "code", "REQUIRED")));
        assertThat(results.get(1)).containsEntry("fieldErrors", List.of(Map.of("field", "name", "code", "SIZE")));
        assertThat(results.get(2)).containsEntry("errorCode", "CATEGORY_NAME_TAKEN").doesNotContainKey("fieldErrors");
        assertThat(results.get(3)).containsEntry("fieldErrors", List.of(Map.of("field", "name", "code", "REQUIRED")));
        assertThat(results).allSatisfy(r -> assertThat((String) r.get("message")).isNotBlank()
                .doesNotContain(takenName));
    }

    // ---------- Sai kiểu trong payload (D93) và thứ tự kiểm (D96)

    @Test
    void wrongJsonTypeInPayloadRejectsOnlyThatMutation() throws Exception {
        TestUser user = newUser();
        List<Map<String, Object>> results = push(user,
                mutation("project", UUID.randomUUID(), "CREATE", 0, payload("name", 123)),
                mutation("project", UUID.randomUUID(), "CREATE", 0, payload("name", "OK")));

        assertThat(results.get(0)).containsEntry("status", "REJECTED").containsEntry("errorCode", "VALIDATION_FAILED")
                .containsEntry("fieldErrors", List.of(Map.of("field", "name", "code", "INVALID_VALUE")));
        assertThat(results.get(1)).containsEntry("status", "APPLIED");
    }

    @Test
    void existenceAndVersionAreCheckedBeforeValuesIncludingWrongTypes() throws Exception {
        TestUser user = newUser();
        UUID id = createProject(user);

        assertThat(pushOne(user, mutation("project", id, "UPDATE", 9, payload("archived", "yes"))))
                .as("sai version + sai kiểu → CONFLICT").containsEntry("status", "CONFLICT");
        assertThat(pushOne(user, mutation("project", id, "UPDATE", 9, payload("name", ""))))
                .as("sai version + giá trị sai → CONFLICT").containsEntry("status", "CONFLICT");
        assertThat(pushOne(user, mutation("project", UUID.randomUUID(), "UPDATE", 1, payload("name", 5))))
                .as("không tồn tại + sai kiểu → NOT_FOUND").containsEntry("errorCode", "PROJECT_NOT_FOUND");
        assertThat(pushOne(user, mutation("project", id, "CREATE", 0, payload("name", 5))))
                .as("id đã dùng + sai kiểu → ENTITY_ALREADY_EXISTS").containsEntry("errorCode", "ENTITY_ALREADY_EXISTS");
    }

    // ---------- DUPLICATE

    @Test
    void Sync_02_resendingTheSameMutationIdReplaysTheStoredResultWithoutApplyingAgain() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        Map<String, Object> m = mutation("project", id, "CREATE", 0, payload("name", "P"));
        pushOne(user, m);
        int logs = changeLogsOfUser(user.id());

        Map<String, Object> again = pushOne(user, m);

        assertThat(again).containsEntry("status", "DUPLICATE").containsEntry("originalStatus", "APPLIED")
                .containsEntry("entityVersion", 1).containsEntry("mutationId", m.get("mutationId"));
        assertThat(changeLogsOfUser(user.id())).isEqualTo(logs);
        assertThat(syncMutations(user.id(), mutationIdOf(m))).isEqualTo(1);
    }

    @Test
    void Sync_07_and_29_rejectedAndConflictResultsAreStoredAndReplayedVerbatimEvenAfterTheEntityChanged()
            throws Exception {
        TestUser user = newUser();
        UUID id = createProject(user);
        Map<String, Object> rejected = mutation("project", id, "UPDATE", 1, payload("name", ""));
        Map<String, Object> conflict = mutation("project", id, "UPDATE", 5, payload("name", "Z"));
        List<Map<String, Object>> first = push(user, rejected, conflict);
        pushOne(user, mutation("project", id, "UPDATE", 1, payload("name", "Đã đổi")));

        List<Map<String, Object>> replay = push(user, rejected, conflict);

        assertThat(replay.get(0)).containsEntry("status", "DUPLICATE").containsEntry("originalStatus", "REJECTED");
        assertThat(replay.get(1)).containsEntry("status", "DUPLICATE").containsEntry("originalStatus", "CONFLICT");
        for (int i = 0; i < 2; i++) {
            Map<String, Object> expected = new java.util.LinkedHashMap<>(first.get(i));
            expected.put("status", "DUPLICATE");
            expected.put("originalStatus", first.get(i).get("status"));
            assertThat(replay.get(i)).as("phát lại nguyên văn, snapshot lúc conflict").isEqualTo(expected);
        }
        assertThat(row("projects", id)).containsEntry("name", "Đã đổi");
    }

    @Test
    void Sync_30_reusedMutationIdWithDifferentMetadataReplaysTheStoredResultAndAppliesNothing() throws Exception {
        TestUser user = newUser();
        UUID mutationId = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        pushOne(user, mutation(mutationId, "project", first, "CREATE", 0, payload("name", "Một")));
        UUID second = UUID.randomUUID();

        Map<String, Object> r = pushOne(user, mutation(mutationId, "category", second, "CREATE", 0,
                payload("name", "Hai")));

        assertThat(r).containsEntry("status", "DUPLICATE").containsEntry("originalStatus", "APPLIED")
                .containsEntry("entityVersion", 1);
        assertThat(jdbc.sql("SELECT count(*) FROM categories WHERE id = :id").param("id", second)
                .query(Integer.class).single()).isZero();
        // Log sync_mutation_id_reuse được kiểm ở SensitiveLogIT/SYN-47 (bước 3e).
    }

    @Test
    void Sync_31_sameMutationIdTwiceInOneBatchIsAppliedOnce() throws Exception {
        TestUser user = newUser();
        UUID mutationId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        Map<String, Object> m = mutation(mutationId, "project", id, "CREATE", 0, payload("name", "P"));

        List<Map<String, Object>> results = push(user, m, m);

        assertThat(results.get(0)).containsEntry("status", "APPLIED");
        assertThat(results.get(1)).containsEntry("status", "DUPLICATE").containsEntry("originalStatus", "APPLIED");
        assertThat(changeLogRows(user.id(), id)).hasSize(1);
    }

    @Test
    void Sync_06_aRejectedMutationInTheMiddleDoesNotAffectTheOthers() throws Exception {
        TestUser user = newUser();
        UUID a = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        List<Map<String, Object>> results = push(user,
                mutation("project", a, "CREATE", 0, payload("name", "A")),
                mutation("project", UUID.randomUUID(), "UPDATE", 1, payload("name", "B")),
                mutation("category", c, "CREATE", 0, payload("name", "C " + c.toString().substring(0, 6))));

        assertThat(results).extracting(r -> r.get("status")).containsExactly("APPLIED", "REJECTED", "APPLIED");
        assertThat(row("projects", a)).containsEntry("version", 1L);
        assertThat(row("categories", c)).containsEntry("version", 1L);
    }
}
