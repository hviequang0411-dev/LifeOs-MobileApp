package com.lifeos.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Thứ tự kiểm ở *CommandService (D96), áp dụng cho REST như sync: tồn tại → version → validation giá trị → tham chiếu
 * → áp dụng; CREATE: id đã dùng → 409 trước validation. Validation cấu trúc (Bean Validation ở DTO, kiểu JSON) vẫn ở
 * controller như trước.
 */
class CommandOrderIT extends TaskApiTestBase {

    private int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    @Test
    void patchOfMissingEntityWithInvalidValueIs404() throws Exception {
        TestUser user = newUser();
        assertThat(status(patch(user, TASKS + "/" + UUID.randomUUID(), body("version", 1, "title", ""))))
                .isEqualTo(404);
        assertThat(status(patch(user, PROJECTS + "/" + UUID.randomUUID(), body("version", 1, "name", ""))))
                .isEqualTo(404);
        assertThat(status(patch(user, CATEGORIES + "/" + UUID.randomUUID(), body("version", 1, "name", ""))))
                .isEqualTo(404);
    }

    @Test
    void patchWithWrongVersionAndInvalidValueIs409() throws Exception {
        TestUser user = newUser();
        UUID task = task(user);
        UUID project = project(user);
        UUID category = category(user);

        MvcResult onTask = patch(user, TASKS + "/" + task, body("version", 7, "title", "", "dueTime", "25:00"));
        assertThat(status(onTask)).isEqualTo(409);
        assertThat(readJson(onTask)).containsEntry("errorCode", "ENTITY_VERSION_CONFLICT")
                .containsEntry("currentVersion", 1);
        assertThat(status(patch(user, PROJECTS + "/" + project, body("version", 7, "name", "")))).isEqualTo(409);
        assertThat(status(patch(user, CATEGORIES + "/" + category, body("version", 7, "name", "")))).isEqualTo(409);
    }

    @Test
    void valuesAreCheckedBeforeReferences() throws Exception {
        TestUser user = newUser();
        UUID task = task(user);
        MvcResult result = patch(user, TASKS + "/" + task, body("version", 1, "title", "",
                "projectId", UUID.randomUUID().toString()));
        assertThat(status(result)).isEqualTo(400);
        assertThat(readJson(result)).containsEntry("errorCode", "VALIDATION_FAILED");
    }

    @Test
    void createWithUsedIdIs409BeforeValueValidation() throws Exception {
        TestUser user = newUser();
        UUID task = task(user);
        UUID project = project(user);
        UUID category = category(user);

        assertThat(readJson(post(user, TASKS, body("id", task.toString(), "title", ""))))
                .containsEntry("errorCode", "ENTITY_ALREADY_EXISTS");
        assertThat(readJson(post(user, PROJECTS, body("id", project.toString(), "name", ""))))
                .containsEntry("errorCode", "ENTITY_ALREADY_EXISTS");
        assertThat(readJson(post(user, CATEGORIES, body("id", category.toString(), "name", ""))))
                .containsEntry("errorCode", "ENTITY_ALREADY_EXISTS");
    }
}
