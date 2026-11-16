package com.lifeos.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lifeos.common.exception.BusinessException;
import com.lifeos.common.exception.ConflictException;
import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.GlobalExceptionHandler;
import com.lifeos.common.exception.NotFoundException;
import com.lifeos.common.exception.RateLimitedException;
import com.lifeos.common.web.CorrelationIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Kiểm tra định dạng lỗi RFC 9457 và correlationId (API_CONVENTIONS.md §5). Dùng MockMvc standalone
 * để không phụ thuộc cấu hình Spring Boot.
 */
class ErrorHandlingTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new CorrelationIdFilter())
                .build();
    }

    @Test
    void businessExceptionIsReturnedAsProblemDetailWithErrorCode() throws Exception {
        mvc.perform(get("/probe/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errorCode").value("TASK_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("https://lifeos.app/problems/task-not-found"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void extraPropertiesAreExposedAtTopLevel() throws Exception {
        mvc.perform(get("/probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ENTITY_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.currentVersion").value(7));
    }

    @Test
    void validationFailureListsFieldErrors() throws Exception {
        mvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED"));
    }

    @Test
    void tooLongValueMapsToSizeCode() throws Exception {
        mvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"abcdefghijk\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("SIZE"));
    }

    @Test
    void malformedJsonIsMalformedRequest() throws Exception {
        mvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MALFORMED_REQUEST"));
    }

    @Test
    void unexpectedExceptionDoesNotLeakInternals() throws Exception {
        mvc.perform(get("/probe/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("select * from secret_table"))));
    }

    @Test
    void rateLimitedHas429AndRetryAfterHeader() throws Exception {
        mvc.perform(get("/probe/rate-limited"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "42"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMITED"));
    }

    @Test
    void missingRequiredQueryParameterIsValidationFailedWithFieldError() throws Exception {
        mvc.perform(get("/probe/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("version"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED"));
    }

    @Test
    void wronglyTypedParameterIsValidationFailedWithFieldError() throws Exception {
        mvc.perform(get("/probe/param").param("version", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("version"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_VALUE"));
    }

    @Test
    void validCorrelationIdFromClientIsEchoed() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(get("/probe/not-found").header("X-Correlation-Id", id))
                .andExpect(header().string("X-Correlation-Id", id))
                .andExpect(jsonPath("$.correlationId").value(id));
    }

    @Test
    void invalidCorrelationIdIsReplacedNotLogged() throws Exception {
        mvc.perform(get("/probe/not-found").header("X-Correlation-Id", "evil\nvalue"))
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.correlationId").value(
                        org.hamcrest.Matchers.matchesPattern("[0-9a-f\\-]{36}")));
    }

    record Body(@NotBlank @Size(max = 10) String title) {
    }

    /**
     * Controller chỉ dành cho test này: đăng ký tường minh qua standaloneSetup. Không lọt vào context tích hợp vì
     * TestTypeExcludeFilter của Spring Boot loại mọi lớp lồng trong lớp test khỏi component scan;
     * AuthorizationMatrixIT kiểm tra danh mục route không chứa /probe.
     */
    @RestController
    static class ProbeController {

        @GetMapping("/probe/not-found")
        String notFound() {
            throw new NotFoundException(ErrorCode.TASK_NOT_FOUND);
        }

        @GetMapping("/probe/conflict")
        String conflict() {
            BusinessException ex = new ConflictException(ErrorCode.ENTITY_VERSION_CONFLICT);
            throw ex.with("currentVersion", 7);
        }

        @PostMapping("/probe/validate")
        String validate(@Valid @RequestBody Body body) {
            return body.title();
        }

        @GetMapping("/probe/param")
        String param(@org.springframework.web.bind.annotation.RequestParam long version) {
            return Long.toString(version);
        }

        @GetMapping("/probe/rate-limited")
        String rateLimited() {
            throw new RateLimitedException(42);
        }

        @GetMapping("/probe/boom")
        String boom() {
            throw new IllegalStateException("select * from secret_table");
        }
    }
}
