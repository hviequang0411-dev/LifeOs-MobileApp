package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.ApiTestBase;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.web.servlet.MvcResult;

/** Hỗ trợ chung cho test Task: tạo Project/Category/Task qua REST và đọc trạng thái cuối từ DB. */
abstract class TaskApiTestBase extends ApiTestBase {

    static final String TASKS = "/api/v1/tasks";
    static final String PROJECTS = "/api/v1/projects";
    static final String CATEGORIES = "/api/v1/categories";

    UUID project(TestUser user) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult result = post(user, PROJECTS, body("id", id.toString(), "name", "Dự án " + id));
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return id;
    }

    UUID category(TestUser user) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult result = post(user, CATEGORIES, body("id", id.toString(),
                "name", "DM " + id.toString().substring(0, 8)));
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        return id;
    }

    /** Tạo Task với các trường bổ sung (cặp khóa/giá trị); trả kết quả để test tự kiểm. */
    MvcResult createTask(TestUser user, UUID id, Object... extra) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id.toString());
        body.put("title", "Mua sữa");
        for (int i = 0; i < extra.length; i += 2) {
            body.put((String) extra[i], extra[i + 1]);
        }
        return post(user, TASKS, json.writeValueAsString(body));
    }

    UUID task(TestUser user, Object... extra) throws Exception {
        UUID id = UUID.randomUUID();
        MvcResult result = createTask(user, id, extra);
        assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(201);
        return id;
    }

    Map<String, Object> taskRow(UUID id) {
        return jdbc.sql("SELECT * FROM tasks WHERE id = :id").param("id", id).query().singleRow();
    }

    /** JSON Merge Patch có thể chứa null tường minh. */
    MvcResult patchTask(TestUser user, UUID id, Object... keyValues) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            body.put((String) keyValues[i], keyValues[i + 1]);
        }
        return patch(user, TASKS + "/" + id, json.writeValueAsString(body));
    }
}
