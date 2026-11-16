package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * UC-ID-08 phần cài đặt (FR-ID-11, D58): user_settings là entity đồng bộ — PATCH bắt buộc version, lệch → 409 kèm
 * currentVersion; mỗi thay đổi thật tăng version và ghi change_logs UPDATE đúng một lần; giá trị được kiểm tra.
 */
class SettingsIT extends IdentityIntegrationTest {

    private static final String PATH = "/api/v1/me/settings";

    private record Session(UUID userId, String access) {
    }

    private Session session() throws Exception {
        String email = newEmail();
        UUID userId = registerVerified(email);
        return new Session(userId, str(loginOk(email, null), "accessToken"));
    }

    private MvcResult patch(Session s, Object... keyValues) throws Exception {
        return sendAuthed("PATCH", PATH, body(keyValues), s.access());
    }

    private List<Map<String, Object>> settingsChanges(UUID userId) {
        return jdbc.sql("SELECT * FROM change_logs WHERE user_id = :u AND entity_type = 'user_settings' ORDER BY seq")
                .param("u", userId).query().listOfRows();
    }

    private Map<String, Object> settingsRow(UUID userId) {
        return jdbc.sql("SELECT * FROM user_settings WHERE user_id = :u").param("u", userId).query().singleRow();
    }

    @Test
    void getReturnsDefaults() throws Exception {
        Session s = session();
        Map<String, Object> body = readJson(getAuthed(PATH, s.access()));
        // WP4 (BACKEND_MASTER_PLAN §5 #1): quietStart, quietEnd, showDetailsOnLockScreen (mặc định null/null/false).
        assertThat(body).containsOnlyKeys("id", "timezone", "locale", "defaultCurrency", "weekStartDay", "quietStart",
                "quietEnd", "showDetailsOnLockScreen", "version", "updatedAt");
        assertThat(body).containsEntry("quietStart", null).containsEntry("quietEnd", null)
                .containsEntry("showDetailsOnLockScreen", false);
        assertThat(body).containsEntry("timezone", "Asia/Ho_Chi_Minh").containsEntry("locale", "vi-VN")
                .containsEntry("defaultCurrency", "VND").containsEntry("weekStartDay", 1);
        assertThat(((Number) body.get("version")).longValue()).isEqualTo(1);
    }

    @Test
    void patchWithCurrentVersionUpdatesBumpsVersionAndRecordsExactlyOneChange() throws Exception {
        Session s = session();

        MvcResult result = patch(s, "version", 1, "timezone", "Europe/Berlin", "locale", "en-US",
                "defaultCurrency", "EUR", "weekStartDay", 7);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> body = readJson(result);
        assertThat(body).containsEntry("timezone", "Europe/Berlin").containsEntry("locale", "en-US")
                .containsEntry("defaultCurrency", "EUR").containsEntry("weekStartDay", 7);
        assertThat(((Number) body.get("version")).longValue()).isEqualTo(2);
        assertThat(settingsRow(s.userId()).get("version")).isEqualTo(2L);

        List<Map<String, Object>> changes = settingsChanges(s.userId());
        assertThat(changes).hasSize(2); // CREATE khi đăng ký + UPDATE lần này
        assertThat(changes.get(1).get("operation")).isEqualTo("UPDATE");
        assertThat(changes.get(1).get("entity_version")).isEqualTo(2L);
        assertThat(changes.get(1).get("entity_id")).isEqualTo(settingsRow(s.userId()).get("id"));
        assertThat(changes.get(1).get("mutation_id")).isNull();
    }

    @Test
    void staleVersionIsAConflictWithCurrentVersionAndChangesNothing() throws Exception {
        Session s = session();
        patch(s, "version", 1, "weekStartDay", 3); // → version 2

        MvcResult stale = patch(s, "version", 1, "timezone", "Asia/Tokyo");

        assertThat(stale.getResponse().getStatus()).isEqualTo(409);
        Map<String, Object> problem = readJson(stale);
        assertThat(problem).containsEntry("errorCode", "ENTITY_VERSION_CONFLICT");
        assertThat(((Number) problem.get("currentVersion")).longValue()).isEqualTo(2);
        assertThat(settingsRow(s.userId()).get("timezone")).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(settingsChanges(s.userId())).hasSize(2);
    }

    @Test
    void missingVersionIsValidationFailed() throws Exception {
        Session s = session();
        MvcResult result = patch(s, "timezone", "Asia/Tokyo");
        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(result.getResponse().getContentAsString()).contains("\"errorCode\":\"VALIDATION_FAILED\"")
                .contains("\"field\":\"version\"").contains("\"code\":\"REQUIRED\"");
    }

    @Test
    void invalidValuesAreRejectedAndNothingChanges() throws Exception {
        Session s = session();
        Object[][] invalid = {
            {"timezone", "Mars/Phobos", "INVALID_VALUE"},
            {"timezone", "+07:00", "INVALID_VALUE"},
            {"locale", "vietnamese", "PATTERN"},
            {"defaultCurrency", "vnd", "PATTERN"},
            {"defaultCurrency", "XYZ", "INVALID_VALUE"},
            {"weekStartDay", 0, "SIZE"},
            {"weekStartDay", 8, "SIZE"},
        };
        for (Object[] c : invalid) {
            MvcResult result = patch(s, "version", 1, c[0], c[1]);
            assertThat(result.getResponse().getStatus()).as(c[0] + "=" + c[1]).isEqualTo(400);
            assertThat(result.getResponse().getContentAsString()).as(c[0] + "=" + c[1])
                    .contains("\"field\":\"" + c[0] + "\"").contains("\"code\":\"" + c[2] + "\"");
        }
        Map<String, Object> explicitNull = new LinkedHashMap<>();
        explicitNull.put("version", 1);
        explicitNull.put("locale", null);
        MvcResult nullLocale = sendAuthed("PATCH", PATH, json.writeValueAsString(explicitNull), s.access());
        assertThat(nullLocale.getResponse().getContentAsString()).contains("\"field\":\"locale\"")
                .contains("\"code\":\"REQUIRED\"");

        assertThat(settingsRow(s.userId()).get("version")).isEqualTo(1L);
        assertThat(settingsChanges(s.userId())).hasSize(1);
    }

    @Test
    void patchThatChangesNothingKeepsVersionAndRecordsNoChange() throws Exception {
        Session s = session();
        MvcResult result = patch(s, "version", 1, "timezone", "Asia/Ho_Chi_Minh", "locale", "vi-VN");
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(((Number) readJson(result).get("version")).longValue()).isEqualTo(1);
        assertThat(settingsChanges(s.userId())).hasSize(1);
    }

    @Test
    void serverControlledFieldsCannotBeMassAssigned() throws Exception {
        Session s = session();
        Object idBefore = settingsRow(s.userId()).get("id");

        MvcResult result = patch(s, "version", 1, "id", UUID.randomUUID().toString(),
                "userId", UUID.randomUUID().toString(), "weekStartDay", 2);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Map<String, Object> row = settingsRow(s.userId());
        assertThat(row.get("id")).isEqualTo(idBefore);
        assertThat(row.get("version")).isEqualTo(2L);
        assertThat(readJson(result)).containsEntry("id", idBefore.toString());
    }
}
