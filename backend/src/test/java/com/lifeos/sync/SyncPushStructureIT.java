package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Kiểm tra cấu trúc toàn request POST /sync/push (SYNC_PROTOCOL §2.1, D90, D93): lỗi cấu trúc làm cả request thất
 * bại, không mutation nào được xử lý hay lưu. SYN-08, SYN-36, SYN-37, SYN-38.
 */
class SyncPushStructureIT extends SyncTestBase {

    private Map<String, Object> validProject(UUID mutationId) {
        return mutation(mutationId, "project", UUID.randomUUID(), "CREATE", 0, payload("name", "P"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> fieldErrors(Map<String, Object> problem) {
        return (List<Map<String, Object>>) problem.get("fieldErrors");
    }

    @Test
    void Sync_08_batchOfMoreThan50IsRejectedAndNothingIsApplied() throws Exception {
        TestUser user = newUser();
        List<Map<String, Object>> mutations = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            mutations.add(validProject(UUID.randomUUID()));
        }
        MvcResult result = post(user, PUSH, json.writeValueAsString(Map.of("mutations", mutations)));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(result)).containsEntry("errorCode", "SYNC_BATCH_TOO_LARGE").doesNotContainKey("results");
        assertThat(changeLogsOfUser(user.id())).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM sync_mutations WHERE user_id = :u").param("u", user.id())
                .query(Integer.class).single()).isZero();
    }

    @Test
    void Sync_37_emptyBatchIsValidationFailedAndBatchOf50IsAccepted() throws Exception {
        TestUser user = newUser();
        MvcResult empty = pushRaw(user, "{\"mutations\":[]}");
        assertThat(empty.getResponse().getStatus()).isEqualTo(400);
        Map<String, Object> problem = readJson(empty);
        assertThat(problem).containsEntry("errorCode", "VALIDATION_FAILED");
        assertThat(fieldErrors(problem)).singleElement()
                .satisfies(e -> assertThat(e).containsEntry("field", "mutations").containsEntry("code", "SIZE"));

        List<Map<String, Object>> fifty = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            fifty.add(validProject(UUID.randomUUID()));
        }
        assertThat(push(user, fifty)).allSatisfy(r -> assertThat(r).containsEntry("status", "APPLIED"));
    }

    @Test
    void Sync_36_malformedMutationFailsTheWholeRequestWithPositionsAndStoresNothing() throws Exception {
        TestUser user = newUser();
        UUID okMutation = UUID.randomUUID();
        Map<String, Object> badIds = mutation("project", UUID.randomUUID(), "CREATE", 0, payload("name", "P"));
        badIds.put("mutationId", "not-a-uuid");
        badIds.put("entityId", 42);
        Map<String, Object> badOperation = mutation("project", UUID.randomUUID(), "UPSERT", 0, null);
        Map<String, Object> badBase = mutation("project", UUID.randomUUID(), "UPDATE", -1, null);
        Map<String, Object> badBaseFraction = mutation("project", UUID.randomUUID(), "UPDATE", 0, null);
        badBaseFraction.put("baseVersion", 1.5);
        Map<String, Object> missingType = mutation("project", UUID.randomUUID(), "CREATE", 0, null);
        missingType.remove("entityType");
        Map<String, Object> blankType = mutation(" ", UUID.randomUUID(), "CREATE", 0, null);
        Map<String, Object> longType = mutation("x".repeat(31), UUID.randomUUID(), "CREATE", 0, null);
        Map<String, Object> payloadArray = mutation("project", UUID.randomUUID(), "CREATE", 0, null);
        payloadArray.put("payload", List.of("a"));

        MvcResult result = pushResult(user, validProject(okMutation), badIds, badOperation, badBase, badBaseFraction,
                missingType, blankType, longType, payloadArray);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        Map<String, Object> problem = readJson(result);
        assertThat(problem).containsEntry("errorCode", "MALFORMED_REQUEST").doesNotContainKey("results");
        assertThat(fieldErrors(problem)).extracting(e -> e.get("field") + ":" + e.get("code")).containsExactly(
                "mutations[1].mutationId:INVALID_VALUE", "mutations[1].entityId:INVALID_VALUE",
                "mutations[2].operation:INVALID_VALUE", "mutations[3].baseVersion:INVALID_VALUE",
                "mutations[4].baseVersion:INVALID_VALUE", "mutations[5].entityType:REQUIRED",
                "mutations[6].entityType:INVALID_VALUE", "mutations[7].entityType:SIZE",
                "mutations[8].payload:INVALID_VALUE");
        assertThat(syncMutations(user.id(), okMutation)).as("mutation hợp lệ trong lô hỏng không được xử lý").isZero();
        assertThat(changeLogsOfUser(user.id())).isZero();
    }

    @Test
    void Sync_36_brokenJsonOrMissingMutationsIsMalformedRequest() throws Exception {
        TestUser user = newUser();
        assertThat(readJson(pushRaw(user, "{\"mutations\":[")))
                .containsEntry("errorCode", "MALFORMED_REQUEST");
        MvcResult missing = pushRaw(user, "{}");
        assertThat(missing.getResponse().getStatus()).isEqualTo(400);
        assertThat(readJson(missing)).containsEntry("errorCode", "MALFORMED_REQUEST");
        assertThat(readJson(pushRaw(user, "{\"mutations\":{}}")))
                .containsEntry("errorCode", "MALFORMED_REQUEST");
        assertThat(readJson(pushRaw(user, "[]"))).containsEntry("errorCode", "MALFORMED_REQUEST");
    }

    @Test
    void Sync_36_unknownEntityTypeIsNotAStructuralError() throws Exception {
        TestUser user = newUser();
        assertThat(pushOne(user, mutation("x".repeat(30), UUID.randomUUID(), "CREATE", 0, null)))
                .containsEntry("status", "REJECTED").containsEntry("errorCode", "SYNC_UNKNOWN_ENTITY_TYPE");
    }

    @Test
    void Sync_38_bodyOver1MbIs413AndNothingIsProcessed() throws Exception {
        TestUser user = newUser();
        UUID mutationId = UUID.randomUUID();
        Map<String, Object> huge = mutation(mutationId, "project", UUID.randomUUID(), "CREATE", 0,
                payload("name", "P", "description", "a".repeat(1_100_000)));
        MvcResult result = pushResult(user, huge);

        assertThat(result.getResponse().getStatus()).isEqualTo(413);
        assertThat(readJson(result)).containsEntry("errorCode", "PAYLOAD_TOO_LARGE");
        assertThat(syncMutations(user.id(), mutationId)).isZero();
    }

    @Test
    void unknownFieldsAtMutationAndPayloadLevelAreIgnored() throws Exception {
        TestUser user = newUser();
        UUID id = UUID.randomUUID();
        Map<String, Object> m = mutation("project", id, "CREATE", 0, payload("name", "P", "colour", "red"));
        m.put("deviceHint", "abc");
        assertThat(pushOne(user, m)).containsEntry("status", "APPLIED");
    }
}
