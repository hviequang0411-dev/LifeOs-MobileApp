package com.lifeos.common.config;

import com.lifeos.common.security.SecurityConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mô tả chung của tài liệu OpenAPI (D63: chỉ bật ở dev/test, nên bean chỉ tạo khi springdoc bật). Server cố định "/"
 * thay vì địa chỉ suy ra từ request (cổng ngẫu nhiên) để docs/api/openapi.yaml xuất ra ổn định giữa các lần chạy.
 * Mã thành công khác 200 khai báo bằng @ApiResponse ở controller; lỗi chung (RFC 9457) và việc gỡ bearer khỏi endpoint
 * công khai (theo SecurityConfig.PUBLIC_POST_ENDPOINTS) thêm ở {@link #problemsAndPublicEndpoints()}.
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";
    static final String PROBLEM = "Problem";
    static final String PROBLEM_JSON = "application/problem+json";

    @Bean
    OpenAPI lifeosOpenApi() {
        return new OpenAPI()
                .info(new Info().title("LifeOS API").version("v1"))
                .servers(List.of(new Server().url("/")))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        .addSchemas(PROBLEM, problemSchema()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /**
     * operationId = tên controller (bỏ hậu tố Controller) + tên method, ví dụ taskDelete, projectGet. Mặc định springdoc
     * dùng tên method rồi thêm _1, _2 theo thứ tự duyệt handler khi trùng — thứ tự đó không được bảo đảm giữa các máy,
     * nên file xuất ra có thể đổi dù API không đổi.
     */
    @Bean
    OperationCustomizer stableOperationIds() {
        return (operation, handlerMethod) -> {
            String controller = handlerMethod.getBeanType().getSimpleName().replaceFirst("Controller$", "");
            String method = handlerMethod.getMethod().getName();
            operation.setOperationId(Character.toLowerCase(controller.charAt(0)) + controller.substring(1)
                    + Character.toUpperCase(method.charAt(0)) + method.substring(1));
            return operation;
        };
    }

    /**
     * Mọi operation có phản hồi "default" = lỗi RFC 9457 (API_CONVENTIONS §5; mã lỗi cụ thể ở errorCode). Endpoint
     * công khai không yêu cầu bearer (security rỗng), đúng SecurityConfig.
     */
    @Bean
    OpenApiCustomizer problemsAndPublicEndpoints() {
        Set<String> publicPaths = Set.of(SecurityConfig.PUBLIC_POST_ENDPOINTS);
        return openApi -> openApi.getPaths().forEach((path, item) -> {
            item.readOperations().forEach(OpenApiConfig::addProblemResponse);
            if (publicPaths.contains(path) && item.getPost() != null) {
                item.getPost().setSecurity(List.of());
            }
        });
    }

    private static void addProblemResponse(Operation operation) {
        operation.getResponses().addApiResponse("default", new ApiResponse()
                .description("Lỗi theo RFC 9457 (errorCode ổn định, API_CONVENTIONS §5)")
                .content(new Content().addMediaType(PROBLEM_JSON,
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM)))));
    }

    private static Schema<?> problemSchema() {
        return new ObjectSchema()
                .addProperty("type", new StringSchema())
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema())
                .addProperty("errorCode", new StringSchema())
                .addProperty("correlationId", new StringSchema())
                .addProperty("currentVersion", new IntegerSchema().format("int64"))
                .addProperty("fieldErrors", new ArraySchema().items(new ObjectSchema()
                        .addProperty("field", new StringSchema())
                        .addProperty("code", new StringSchema())
                        .addProperty("message", new StringSchema())))
                .required(List.of("type", "title", "status", "errorCode"));
    }
}
