package com.lifeos.support;

import com.lifeos.common.security.JwtService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.json.JsonMapper;

/**
 * Nền cho test API dữ liệu (Project/Category/Task): user được tạo bằng SQL và access token cấp trực tiếp từ JwtService
 * (không đi qua đăng ký/đăng nhập — luồng đó đã có test riêng ở module identity), nên nhanh và không phụ thuộc rate
 * limit của endpoint auth. Mỗi user một UUID mới, mỗi instance test một IP riêng.
 */
public abstract class ApiTestBase extends IntegrationTestBase {

    @Autowired
    protected JdbcClient jdbc;

    @Autowired
    protected JsonMapper json;

    @Autowired
    protected JwtService jwtService;

    protected final String ip = randomIp();

    /** Một user đã đăng nhập: userId và access token của một thiết bị. */
    public record TestUser(UUID id, String token) {
    }

    protected TestUser newUser() {
        UUID userId = TestUsers.insert(jdbc);
        return new TestUser(userId, jwtService.issue(userId, UUID.randomUUID()).token());
    }

    protected String body(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return json.writeValueAsString(map);
    }

    protected MvcResult call(TestUser user, HttpMethod method, String path, String jsonBody) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(method, path).with(fromIp(ip));
        if (user != null) {
            request.header("Authorization", "Bearer " + user.token());
        }
        if (jsonBody != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(jsonBody);
        }
        return mvc.perform(request).andReturn();
    }

    protected MvcResult get(TestUser user, String path) throws Exception {
        return call(user, HttpMethod.GET, path, null);
    }

    protected MvcResult post(TestUser user, String path, String jsonBody) throws Exception {
        return call(user, HttpMethod.POST, path, jsonBody);
    }

    protected MvcResult patch(TestUser user, String path, String jsonBody) throws Exception {
        return call(user, HttpMethod.PATCH, path, jsonBody);
    }

    protected MvcResult delete(TestUser user, String path) throws Exception {
        return call(user, HttpMethod.DELETE, path, null);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> readJson(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsString(), Map.class);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> items(MvcResult result) throws Exception {
        return (List<Map<String, Object>>) readJson(result).get("items");
    }

    /** Thân lỗi bỏ correlationId và instance (luôn khác giữa hai request) để so "giống hệt". */
    protected Map<String, Object> problem(MvcResult result) throws Exception {
        Map<String, Object> problem = new LinkedHashMap<>(readJson(result));
        problem.remove("correlationId");
        problem.remove("instance");
        return problem;
    }

    protected static long version(Map<String, Object> resource) {
        return ((Number) resource.get("version")).longValue();
    }

    /** Số dòng change_logs của một entity, theo thao tác. */
    protected int changeLogs(UUID userId, String entityType, UUID entityId, String operation) {
        return jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u AND entity_type = :t"
                        + " AND entity_id = :e AND operation = :o")
                .param("u", userId).param("t", entityType).param("e", entityId).param("o", operation)
                .query(Integer.class).single();
    }

    protected int changeLogs(UUID userId, String entityType, UUID entityId) {
        return jdbc.sql("SELECT count(*) FROM change_logs WHERE user_id = :u AND entity_type = :t"
                        + " AND entity_id = :e")
                .param("u", userId).param("t", entityType).param("e", entityId)
                .query(Integer.class).single();
    }
}
