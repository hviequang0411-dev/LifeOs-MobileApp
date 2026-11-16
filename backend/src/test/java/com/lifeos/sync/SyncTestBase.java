package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.web.servlet.MvcResult;

/** Hỗ trợ test POST /sync/push: dựng mutation, gửi lô, đọc kết quả và trạng thái DB. */
abstract class SyncTestBase extends ApiTestBase {

    static final String PUSH = "/api/v1/sync/push";

    /** Một mutation dạng Map (để test được cả JSON sai cấu trúc). */
    static Map<String, Object> mutation(UUID mutationId, String entityType, UUID entityId, String operation,
            long baseVersion, Map<String, Object> payload) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("mutationId", mutationId.toString());
        m.put("entityType", entityType);
        m.put("entityId", entityId.toString());
        m.put("operation", operation);
        m.put("baseVersion", baseVersion);
        if (payload != null) {
            m.put("payload", payload);
        }
        return m;
    }

    static Map<String, Object> mutation(String entityType, UUID entityId, String operation, long baseVersion,
            Map<String, Object> payload) {
        return mutation(UUID.randomUUID(), entityType, entityId, operation, baseVersion, payload);
    }

    /** Payload từ cặp khóa/giá trị (giữ được null tường minh). */
    static Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    MvcResult pushRaw(TestUser user, String json) throws Exception {
        return post(user, PUSH, json);
    }

    @SafeVarargs
    final MvcResult pushResult(TestUser user, Map<String, Object>... mutations) throws Exception {
        return post(user, PUSH, json.writeValueAsString(Map.of("mutations", Arrays.asList(mutations))));
    }

    /** Gửi lô hợp lệ, khẳng định 200 và trả results. */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> push(TestUser user, Map<String, Object>... mutations) throws Exception {
        MvcResult result = pushResult(user, mutations);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
        List<Map<String, Object>> results = (List<Map<String, Object>>) readJson(result).get("results");
        assertThat(results).hasSize(mutations.length);
        return results;
    }

    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> push(TestUser user, List<Map<String, Object>> mutations) throws Exception {
        return push(user, mutations.toArray(new Map[0]));
    }

    Map<String, Object> pushOne(TestUser user, Map<String, Object> mutation) throws Exception {
        return push(user, List.of(mutation)).get(0);
    }

    UUID createProject(TestUser user) throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> r = pushOne(user, mutation("project", id, "CREATE", 0, payload("name", "Dự án " + id)));
        assertThat(r).containsEntry("status", "APPLIED");
        return id;
    }

    UUID createCategory(TestUser user) throws Exception {
        UUID id = UUID.randomUUID();
        Map<String, Object> r = pushOne(user, mutation("category", id, "CREATE", 0,
                payload("name", "DM " + id.toString().substring(0, 8))));
        assertThat(r).containsEntry("status", "APPLIED");
        return id;
    }

    Map<String, Object> row(String table, UUID id) {
        return jdbc.sql("SELECT * FROM " + table + " WHERE id = :id").param("id", id).query().singleRow();
    }

    int syncMutations(UUID userId, UUID mutationId) {
        return jdbc.sql("SELECT count(*) FROM sync_mutations WHERE user_id = :u AND mutation_id = :m")
                .param("u", userId).param("m", mutationId).query(Integer.class).single();
    }

    int changeLogsOfUser(UUID userId) {
        return jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u").param("u", userId)
                .query(Integer.class).single();
    }

    /** mutation_id của các dòng change_logs của một entity theo seq tăng dần. */
    List<Map<String, Object>> changeLogRows(UUID userId, UUID entityId) {
        return new ArrayList<>(jdbc.sql("SELECT seq, operation, entity_version, mutation_id FROM change_logs"
                        + " WHERE user_id = :u AND entity_id = :e ORDER BY seq")
                .param("u", userId).param("e", entityId).query().listOfRows());
    }

    static UUID mutationIdOf(Map<String, Object> mutation) {
        return UUID.fromString((String) mutation.get("mutationId"));
    }

    static long longOf(Object value) {
        return ((Number) value).longValue();
    }
}
