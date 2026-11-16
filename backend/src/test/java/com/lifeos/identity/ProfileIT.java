package com.lifeos.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** UC-ID-08 phần hồ sơ (FR-ID-07): GET/PATCH /me; email không đổi được; SEC-08 (mass assignment) cho /me. */
class ProfileIT extends IdentityIntegrationTest {

    private String sessionFor(String email) throws Exception {
        registerVerified(email);
        return str(loginOk(email, null), "accessToken");
    }

    @Test
    void getReturnsTheProfileOfTheTokenOwner() throws Exception {
        String email = newEmail();
        String access = sessionFor(email);
        UUID userId = userId(email).orElseThrow();

        MvcResult result = getAuthed("/api/v1/me", access);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        Map<String, Object> body = readJson(result);
        assertThat(body).containsOnlyKeys("id", "email", "displayName", "emailVerified", "createdAt");
        assertThat(body).containsEntry("id", userId.toString()).containsEntry("email", email)
                .containsEntry("displayName", "Người Test").containsEntry("emailVerified", true);
        assertThat((String) body.get("createdAt")).endsWith("Z");
    }

    @Test
    void patchChangesDisplayNameAndAbsentMeansUnchanged() throws Exception {
        String access = sessionFor(newEmail());

        MvcResult renamed = sendAuthed("PATCH", "/api/v1/me", body("displayName", "  Tên Mới  "), access);
        assertThat(renamed.getResponse().getStatus()).isEqualTo(200);
        assertThat(readJson(renamed)).containsEntry("displayName", "Tên Mới");

        MvcResult empty = sendAuthed("PATCH", "/api/v1/me", "{}", access);
        assertThat(readJson(empty)).containsEntry("displayName", "Tên Mới");
    }

    @Test
    void nullBlankOrTooLongDisplayNameIsRejected() throws Exception {
        String access = sessionFor(newEmail());
        Map<String, Object> nullName = new LinkedHashMap<>();
        nullName.put("displayName", null);

        MvcResult explicitNull = sendAuthed("PATCH", "/api/v1/me", json.writeValueAsString(nullName), access);
        assertThat(explicitNull.getResponse().getStatus()).isEqualTo(400);
        assertThat(explicitNull.getResponse().getContentAsString()).contains("\"field\":\"displayName\"")
                .contains("\"code\":\"REQUIRED\"");

        assertThat(sendAuthed("PATCH", "/api/v1/me", body("displayName", "   "), access)
                .getResponse().getContentAsString()).contains("\"code\":\"REQUIRED\"");
        assertThat(sendAuthed("PATCH", "/api/v1/me", body("displayName", "n".repeat(101)), access)
                .getResponse().getContentAsString()).contains("\"code\":\"SIZE\"");
        assertThat(readJson(getAuthed("/api/v1/me", access))).containsEntry("displayName", "Người Test");
    }

    @Test
    void emailIdAndVerificationCannotBeChangedThroughPatch() throws Exception {
        String email = newEmail();
        String access = sessionFor(email);
        UUID userId = userId(email).orElseThrow();

        MvcResult result = sendAuthed("PATCH", "/api/v1/me", body("email", "hijack@example.test",
                "id", UUID.randomUUID().toString(), "userId", UUID.randomUUID().toString(),
                "emailVerified", false, "displayName", "Ok"), access);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(readJson(result)).containsEntry("email", email).containsEntry("id", userId.toString())
                .containsEntry("emailVerified", true);
        assertThat(userRow(userId).get("email")).isEqualTo(email);
    }

    @Test
    void meEndpointsRequireAuthentication() throws Exception {
        assertThat(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/me").with(fromIp(ip))).andReturn().getResponse().getStatus()).isEqualTo(401);
        assertThat(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/me/settings").with(fromIp(ip))).andReturn().getResponse().getStatus()).isEqualTo(401);
    }
}
