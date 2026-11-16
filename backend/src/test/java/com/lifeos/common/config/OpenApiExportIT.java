package com.lifeos.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.common.security.JwtService;
import com.lifeos.support.IntegrationTestBase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Hợp đồng OpenAPI (API_CONVENTIONS §10, D68: "đổi hợp đồng = đổi fixture + OpenAPI trong cùng PR").
 * <ul>
 *   <li>Luôn ghi bản sinh ra vào target/openapi.yaml.</li>
 *   <li>Chạy với -Dopenapi.export=true: ghi đè docs/api/openapi.yaml (cách cập nhật file sau khi đổi API).</li>
 *   <li>Ngược lại: docs/api/openapi.yaml phải trùng từng byte với bản sinh ra — API đổi mà quên xuất lại thì đỏ.</li>
 * </ul>
 * Ngoài ra: mọi route /api/v1 của RequestMappingHandlerMapping đều có trong tài liệu, và hai lần lấy cho cùng kết quả.
 */
class OpenApiExportIT extends IntegrationTestBase {

    private static final Path COMMITTED = Path.of("..", "docs", "api", "openapi.yaml");
    private static final Path GENERATED = Path.of("target", "openapi.yaml");
    private static final String AI_PATHS_BEGIN = "  # BEGIN AIP1 CONTRACT-FIRST AI PATHS\n";
    private static final String AI_PATHS_END = "  # END AIP1 CONTRACT-FIRST AI PATHS\n";
    private static final String AI_SCHEMAS_BEGIN = "    # BEGIN AIP1 CONTRACT-FIRST AI SCHEMAS\n";
    private static final String AI_SCHEMAS_END = "    # END AIP1 CONTRACT-FIRST AI SCHEMAS\n";
    private static final Set<String> RUNTIME_AI_PATHS = Set.of(
            "/api/v1/ai/preferences", "/api/v1/ai/conversations", "/api/v1/ai/conversations/{id}",
            "/api/v1/ai/conversations/{id}/messages", "/api/v1/ai/messages/{id}/context", "/api/v1/ai/data",
            "/api/v1/ai/plans/daily", "/api/v1/ai/plans/weekly", "/api/v1/ai/tasks/{id}/breakdown",
            "/api/v1/ai/insights/habits", "/api/v1/ai/insights/finance", "/api/v1/ai/notes/{id}/summary",
            "/api/v1/ai/usage", "/api/v1/ai/limits");
    private static final Set<String> RUNTIME_AI_OPERATIONS = Set.of(
            "aiGetPreferences", "aiReplacePreferences", "aiCreateConversation", "aiListConversations",
            "aiGetConversation", "aiDeleteConversation", "aiCreateMessage", "aiGetMessageContext",
            "aiDeleteAllData", "aiCreateDailyPlan", "aiCreateWeeklyPlan", "aiBreakDownTask",
            "aiCreateHabitInsight", "aiCreateFinanceInsight", "aiSummarizeNote", "aiGetUsage", "aiGetLimits");

    @Value("${local.server.port}")
    int port;

    @Autowired
    JwtService jwtService;

    @Autowired
    JsonMapper json;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    private String fetch(String path) throws Exception {
        String token = jwtService.issue(UUID.randomUUID(), UUID.randomUUID()).token();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return response.body();
    }

    private static String stripContractFirstBlock(String source, String begin, String end) {
        int beginAt = source.indexOf(begin);
        int endAt = source.indexOf(end, beginAt + begin.length());
        assertThat(beginAt).as("thiếu marker contract-first " + begin.trim()).isGreaterThanOrEqualTo(0);
        assertThat(endAt).as("thiếu marker contract-first " + end.trim()).isGreaterThan(beginAt);
        return source.substring(0, beginAt) + source.substring(endAt + end.length());
    }

    private static String withoutContractFirstAiOverlay(String source) {
        String withoutPaths = stripContractFirstBlock(source, AI_PATHS_BEGIN, AI_PATHS_END);
        return stripContractFirstBlock(withoutPaths, AI_SCHEMAS_BEGIN, AI_SCHEMAS_END);
    }

    /** AIP6 đang hoãn: bỏ phần AI runtime ở phía sinh trước khi kiểm regression contract cũ. */
    private static String withoutGeneratedAi(String source) {
        StringBuilder result = new StringBuilder(source.length());
        boolean aiPath = false;
        boolean aiSchema = false;
        for (String line : source.split("(?<=\\n)", -1)) {
            if (line.startsWith("  /api/v1/ai")) aiPath = true;
            if (aiPath) {
                if (line.startsWith("  /api/v1/") && !line.startsWith("  /api/v1/ai")) aiPath = false;
                else continue;
            }
            if (line.matches("^    Ai[A-Za-z0-9_]*:\\r?\\n?$")) aiSchema = true;
            if (aiSchema) {
                if (line.matches("^    (?!Ai)[A-Za-z0-9_]+:\\r?\\n?$")) aiSchema = false;
                else continue;
            }
            result.append(line);
        }
        return result.toString();
    }

    @Test
    void openApiDocumentIsCompleteStableAndMatchesTheCommittedFile() throws Exception {
        String yaml = fetch("/v3/api-docs.yaml");
        assertThat(fetch("/v3/api-docs.yaml")).as("xuất hai lần cho cùng kết quả").isEqualTo(yaml);
        assertThat(yaml).contains("title: LifeOS API").contains("bearerFormat: JWT").contains("url: /");

        JsonNode spec = json.readTree(fetch("/v3/api-docs"));
        Set<String> documented = new TreeSet<>();
        spec.get("paths").properties().forEach(e -> documented.add(e.getKey()));
        Set<String> routes = new TreeSet<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            info.getPatternValues().stream().filter(p -> p.startsWith("/api/v1/")).forEach(routes::add);
        }
        assertThat(documented).containsAll(routes);
        Set<String> runtimeAiPaths = new TreeSet<>();
        Set<String> runtimeAiOperations = new TreeSet<>();
        spec.get("paths").properties().forEach(path -> {
            if (path.getKey().startsWith("/api/v1/ai")) {
                runtimeAiPaths.add(path.getKey());
                path.getValue().properties().forEach(operation ->
                        runtimeAiOperations.add(operation.getValue().get("operationId").asText()));
            }
        });
        assertThat(runtimeAiPaths).as("AIP2-AIP4/AIP7 runtime; 4 action path còn hoãn theo AIP6")
                .isEqualTo(RUNTIME_AI_PATHS);
        assertThat(runtimeAiOperations).as("operationId runtime phải giữ đúng contract AIP1 cho Flutter")
                .isEqualTo(RUNTIME_AI_OPERATIONS);

        // Mã thành công thật (không phải "200" suy đoán), lỗi RFC 9457, bearer đúng với SecurityConfig.
        JsonNode paths = spec.get("paths");
        assertThat(paths.at("/~1api~1v1~1tasks/post/responses").has("201")).isTrue();
        assertThat(paths.at("/~1api~1v1~1tasks/post/responses").has("200")).isFalse();
        assertThat(paths.at("/~1api~1v1~1tasks~1{id}/delete/responses").has("204")).isTrue();
        assertThat(paths.at("/~1api~1v1~1auth~1register/post/responses").has("202")).isTrue();
        assertThat(paths.at("/~1api~1v1~1auth~1login/post/security").isArray()).isTrue();
        assertThat(paths.at("/~1api~1v1~1auth~1login/post/security").isEmpty()).as("login công khai").isTrue();
        assertThat(paths.at("/~1api~1v1~1sync~1push/post/security").isMissingNode()).as("sync dùng bearer chung")
                .isTrue();
        paths.properties().forEach(path -> path.getValue().properties().forEach(op -> assertThat(
                op.getValue().at("/responses/default/content/application~1problem+json").isMissingNode())
                .as(op.getKey() + " " + path.getKey() + " có phản hồi lỗi RFC 9457").isFalse()));
        assertThat(yaml).doesNotContain("'*/*'");

        Files.createDirectories(GENERATED.getParent());
        Files.writeString(GENERATED, yaml, StandardCharsets.UTF_8);
        if (Boolean.getBoolean("openapi.export")) {
            if (Files.exists(COMMITTED)) {
                assertThat(Files.readString(COMMITTED, StandardCharsets.UTF_8))
                        .as("AIP1 contract-first overlay đang hoạt động; AIP2 phải sinh endpoint/schema rồi bỏ marker trước khi export")
                        .doesNotContain(AI_PATHS_BEGIN, AI_SCHEMAS_BEGIN);
            }
            Files.writeString(COMMITTED, yaml, StandardCharsets.UTF_8);
        }
        assertThat(Files.exists(COMMITTED))
                .as("chưa có docs/api/openapi.yaml: chạy verify với -Dopenapi.export=true").isTrue();
        String committed = Files.readString(COMMITTED, StandardCharsets.UTF_8);
        assertThat(committed)
                .as("AIP1 phải công bố contract AI cho Flutter trước implementation")
                .contains("/api/v1/ai/preferences:", "/api/v1/ai/conversations/{id}/messages:",
                        "/api/v1/ai/messages/{id}/context:", "/api/v1/ai/plans/daily:",
                        "/api/v1/ai/action-proposals/{id}/approve:", "/api/v1/ai/usage:",
                        "/api/v1/ai/limits:", "AI_CONSENT_REQUIRED", "AI_PROVIDER_UNAVAILABLE");
        RUNTIME_AI_PATHS.forEach(path -> assertThat(committed).contains("  " + path + ":"));
        assertThat(withoutContractFirstAiOverlay(committed))
                .as("ngoài AI runtime/overlay AIP1, docs/api/openapi.yaml phải trùng API runtime; "
                        + "xóa ngoại lệ khi AIP6 sinh đủ 18 path")
                .isEqualTo(withoutGeneratedAi(yaml));
    }
}
