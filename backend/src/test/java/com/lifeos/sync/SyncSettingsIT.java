package com.lifeos.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * POST /sync/push với entityType user_settings (SYNC_PROTOCOL §3.4, D88): SYN-33, ma trận UPDATE, cùng validation với
 * PATCH /me/settings (SYN-34), snapshot data theo §6.1 (có updatedAt — quyết định A).
 */
class SyncSettingsIT extends SyncTestBase {

    /** User test được tạo bằng SQL nên tự thêm dòng settings mặc định (đăng ký thật tạo dòng này). */
    private UUID settingsOf(TestUser user) {
        UUID id = UUID.randomUUID();
        jdbc.sql("INSERT INTO user_settings (id, user_id, timezone) VALUES (:id, :u, 'Asia/Ho_Chi_Minh')")
                .param("id", id).param("u", user.id()).update();
        return id;
    }

    @Test
    void Sync_33_updateIsAppliedCreateAndDeleteAreNotAllowedAndWrongIdIsNotFound() throws Exception {
        TestUser user = newUser();
        TestUser other = newUser();
        UUID settings = settingsOf(user);
        UUID foreign = settingsOf(other);
        Map<String, Object> update = mutation("user_settings", settings, "UPDATE", 1,
                payload("timezone", "Asia/Tokyo", "weekStartDay", 7));

        List<Map<String, Object>> results = push(user, update,
                mutation("user_settings", UUID.randomUUID(), "CREATE", 0, payload("timezone", "UTC")),
                mutation("user_settings", settings, "DELETE", 2, null),
                mutation("user_settings", foreign, "UPDATE", 1, payload("locale", "en-US")),
                mutation("user_settings", UUID.randomUUID(), "UPDATE", 1, payload("locale", "en-US")),
                mutation("user_settings", UUID.randomUUID(), "CREATE", 9, null));

        assertThat(results.get(0)).containsEntry("status", "APPLIED").containsEntry("entityVersion", 2);
        assertThat(results.get(1)).containsEntry("errorCode", "SYNC_OPERATION_NOT_ALLOWED");
        assertThat(results.get(2)).containsEntry("errorCode", "SYNC_OPERATION_NOT_ALLOWED");
        assertThat(results.get(3)).containsOnlyKeys("mutationId", "status", "errorCode", "message")
                .containsEntry("errorCode", "USER_SETTINGS_NOT_FOUND");
        assertThat(results.get(4)).containsOnlyKeys("mutationId", "status", "errorCode", "message")
                .containsEntry("errorCode", "USER_SETTINGS_NOT_FOUND");
        assertThat(results.get(5)).as("thao tác không được phép kiểm trước baseVersion của CREATE (D93)")
                .containsEntry("errorCode", "SYNC_OPERATION_NOT_ALLOWED");

        assertThat(row("user_settings", settings)).containsEntry("timezone", "Asia/Tokyo")
                .containsEntry("week_start_day", 7).containsEntry("version", 2L);
        assertThat(changeLogRows(user.id(), settings)).singleElement().satisfies(log -> assertThat(log)
                .containsEntry("operation", "UPDATE").containsEntry("mutation_id", mutationIdOf(update)));
        assertThat(row("user_settings", foreign)).containsEntry("locale", "vi-VN").containsEntry("version", 1L);
        assertThat(changeLogsOfUser(other.id())).isZero();
    }

    @Test
    void staleVersionIsConflictWithSnapshotDataPerSection61() throws Exception {
        TestUser user = newUser();
        UUID settings = settingsOf(user);
        pushOne(user, mutation("user_settings", settings, "UPDATE", 1, payload("locale", "en-US")));

        Map<String, Object> r = pushOne(user, mutation("user_settings", settings, "UPDATE", 1,
                payload("weekStartDay", "3")));

        assertThat(r).containsEntry("status", "CONFLICT");
        @SuppressWarnings("unchecked")
        Map<String, Object> snapshot = (Map<String, Object>) r.get("serverSnapshot");
        assertThat(snapshot).containsEntry("entityType", "user_settings").containsEntry("version", 2)
                .containsEntry("deleted", false);
        assertThat(snapshot.get("data")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsOnlyKeys("timezone", "locale", "defaultCurrency", "weekStartDay", "quietStart", "quietEnd",
                        "showDetailsOnLockScreen", "updatedAt")
                .containsEntry("locale", "en-US");
    }

    @Test
    void unchangedUpdateIsAppliedWithoutVersionBumpAndWrongTypeIsRejectedPerField() throws Exception {
        TestUser user = newUser();
        UUID settings = settingsOf(user);

        assertThat(pushOne(user, mutation("user_settings", settings, "UPDATE", 1,
                payload("timezone", "Asia/Ho_Chi_Minh", "locale", "vi-VN", "defaultCurrency", "VND",
                        "weekStartDay", 1)))).containsEntry("status", "APPLIED").containsEntry("entityVersion", 1);
        assertThat(changeLogsOfUser(user.id())).isZero();

        assertThat(pushOne(user, mutation("user_settings", settings, "UPDATE", 1, payload("weekStartDay", "3"))))
                .containsEntry("fieldErrors", List.of(Map.of("field", "weekStartDay", "code", "INVALID_VALUE")));
        assertThat(pushOne(user, mutation("user_settings", settings, "UPDATE", 1, payload("weekStartDay", 2.5))))
                .containsEntry("fieldErrors", List.of(Map.of("field", "weekStartDay", "code", "INVALID_VALUE")));
    }

    /** SYN-34 cho user_settings: cùng đầu vào sai qua PATCH /me/settings và sync → cùng errorCode, fieldErrors. */
    @Test
    void Sync_34_settingsValidationIsIdenticalToRest() throws Exception {
        TestUser user = newUser();
        UUID settings = settingsOf(user);
        List<Object[]> invalid = List.of(
                new Object[] {"timezone", "Mars/Olympus"},
                new Object[] {"timezone", null},
                new Object[] {"locale", "vietnamese"},
                new Object[] {"defaultCurrency", "vnd"},
                new Object[] {"defaultCurrency", "XYZ"},
                new Object[] {"weekStartDay", 8});

        List<String> mismatches = new ArrayList<>();
        for (Object[] fields : invalid) {
            Map<String, Object> problem = readJson(patch(user, "/api/v1/me/settings",
                    body("version", 1, fields[0], fields[1])));
            Map<String, Object> sync = pushOne(user, mutation("user_settings", settings, "UPDATE", 1,
                    payload(fields)));
            Object restFieldErrors = ((List<?>) problem.get("fieldErrors")).stream()
                    .map(e -> Map.of("field", ((Map<?, ?>) e).get("field"), "code", ((Map<?, ?>) e).get("code")))
                    .toList();
            if (!problem.get("errorCode").equals(sync.get("errorCode"))
                    || !Objects.equals(restFieldErrors, sync.get("fieldErrors"))) {
                mismatches.add(fields[0] + ": REST " + problem.get("errorCode") + restFieldErrors + " vs sync "
                        + sync.get("errorCode") + sync.get("fieldErrors"));
            }
        }
        assertThat(mismatches).isEmpty();
        assertThat(row("user_settings", settings)).containsEntry("version", 1L);
    }
}
