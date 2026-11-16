package com.lifeos.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** AIP1: contract-first OpenAPI/fixture cho Flutter, chưa cần AI runtime hay provider thật. */
class AiContractPublicationTest {

    private static final Path OPENAPI = Path.of("..", "docs", "api", "openapi.yaml");
    private static final Path FIXTURES = Path.of("..", "docs", "api", "fixtures");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "userId", "email", "password", "apiKey", "accessToken", "refreshToken", "deviceToken", "fcmToken");

    private static final Map<String, List<String>> REQUIRED_ROOT_FIELDS = Map.of(
            "ai-preferences.response.json", List.of("taskCalendar", "finance", "policyVersion", "features"),
            "ai-chat.request.json", List.of("message", "mode", "contextDomains"),
            "ai-chat.response.json", List.of("conversationId", "userMessage", "assistantMessage", "actionProposals"),
            "ai-context.response.json", List.of("messageId", "capturedAt", "domains", "entities", "truncated"),
            "ai-plan.response.json", List.of("periodType", "periodStart", "summary", "items", "contextMessageId"),
            "ai-action-proposal.response.json", List.of("id", "actionType", "status", "payloadHash", "changes"),
            "ai-usage.response.json", List.of("from", "to", "days", "totals"),
            "ai-error.response.json", List.of("type", "status", "errorCode", "correlationId"));

    private static void assertNoForbiddenField(JsonNode node, String fixture) {
        if (node.isObject()) {
            node.properties().forEach(entry -> {
                assertThat(FORBIDDEN_FIELDS)
                        .as(fixture + " không được công bố trường nhạy cảm/được server quản lý: " + entry.getKey())
                        .doesNotContain(entry.getKey());
                assertNoForbiddenField(entry.getValue(), fixture);
            });
        } else if (node.isArray()) {
            node.forEach(child -> assertNoForbiddenField(child, fixture));
        }
    }

    @Test
    void openApiPublishesAllApprovedAiSurfacesBeforeImplementation() throws Exception {
        String spec = Files.readString(OPENAPI);
        assertThat(spec).contains(
                "# BEGIN AIP1 CONTRACT-FIRST AI PATHS",
                "/api/v1/ai/preferences:",
                "/api/v1/ai/conversations:",
                "/api/v1/ai/conversations/{id}/messages:",
                "/api/v1/ai/messages/{id}/context:",
                "/api/v1/ai/data:",
                "/api/v1/ai/plans/daily:",
                "/api/v1/ai/plans/weekly:",
                "/api/v1/ai/tasks/{id}/breakdown:",
                "/api/v1/ai/insights/habits:",
                "/api/v1/ai/insights/finance:",
                "/api/v1/ai/notes/{id}/summary:",
                "/api/v1/ai/action-proposals/{id}/approve:",
                "/api/v1/ai/action-proposals/{id}/cancel:",
                "/api/v1/ai/usage:",
                "/api/v1/ai/limits:",
                "AiErrorCode:",
                "AI_CONSENT_REQUIRED",
                "AI_ACTION_VERSION_CONFLICT");
    }

    @Test
    void publishedFixturesAreValidJsonHaveStableRootsAndContainNoForbiddenFields() throws Exception {
        for (Map.Entry<String, List<String>> expected : REQUIRED_ROOT_FIELDS.entrySet()) {
            Path fixture = FIXTURES.resolve(expected.getKey());
            assertThat(fixture).as("thiếu fixture AIP1 " + expected.getKey()).exists();
            JsonNode root = JSON.readTree(Files.readString(fixture));
            assertThat(root.isObject()).as(expected.getKey() + " phải là JSON object").isTrue();
            for (String field : expected.getValue()) {
                assertThat(root.has(field)).as(expected.getKey() + " thiếu trường " + field).isTrue();
            }
            assertNoForbiddenField(root, expected.getKey());
        }
    }
}
