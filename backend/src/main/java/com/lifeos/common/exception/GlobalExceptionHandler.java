package com.lifeos.common.exception;

import com.lifeos.common.web.CorrelationIdFilter;
import com.lifeos.common.web.RequestSizeLimitFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Điểm duy nhất chuyển exception thành RFC 9457 Problem Details (JAVA_SPRING_RULES.md §6.2,
 * API_CONVENTIONS.md §5). Không bao giờ đưa stack trace, SQL hay thông điệp nội bộ ra client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
        ProblemDetail problem = problem(ex.getStatus(), ex.getErrorCode(), ex.getMessage());
        ex.getProperties().forEach(problem::setProperty);
        if (ex instanceof RateLimitedException limited) {
            return ResponseEntity.status(problem.getStatus())
                    .header(HttpHeaders.RETRY_AFTER, Long.toString(limited.getRetryAfterSeconds()))
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problem);
        }
        return respond(problem);
    }

    /** Body không khai báo Content-Length vượt 1 MB (RequestSizeLimitFilter) → 413 thay vì 400. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof RequestSizeLimitFilter.PayloadTooLargeException) {
                ProblemDetail problem = problem(HttpStatus.CONTENT_TOO_LARGE, ErrorCode.PAYLOAD_TOO_LARGE,
                        ErrorCode.PAYLOAD_TOO_LARGE.title());
                return ResponseEntity.status(problem.getStatus())
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body(problem);
            }
        }
        return super.handleHttpMessageNotReadable(ex, headers, status, request);
    }

    /** @Version của Hibernate là lớp bảo vệ thứ hai cho kiểm tra phiên bản tường minh ở service. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking failure: {}", ex.getClass().getSimpleName());
        return respond(problem(HttpStatus.CONFLICT, ErrorCode.ENTITY_VERSION_CONFLICT,
                ErrorCode.ENTITY_VERSION_CONFLICT.title()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) throws Exception {
        // Để Spring Security xử lý (401/403) thay vì biến thành 500.
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        // Hết lock_timeout / deadlock (D42, D85, D87): 503 + Retry-After cho cả REST và sync, không lộ chi tiết.
        if (LockFailures.isLockFailure(ex)) {
            log.warn("Lock not acquired: {}", ex.getClass().getSimpleName());
            ProblemDetail problem = problem(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE,
                    "Hệ thống đang bận, vui lòng thử lại sau.");
            return ResponseEntity.status(problem.getStatus())
                    .header(HttpHeaders.RETRY_AFTER, Long.toString(LockFailures.RETRY_AFTER_SECONDS))
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problem);
        }
        log.error("Unhandled exception", ex);
        return respond(problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(Map.of(
                    "field", fe.getField(),
                    "code", fieldErrorCode(fe.getCode()),
                    "message", fe.getDefaultMessage() == null ? "Giá trị không hợp lệ." : fe.getDefaultMessage()));
        }
        ProblemDetail body = ex.getBody();
        body.setDetail("Một hoặc nhiều trường không hợp lệ.");
        body.setProperty("fieldErrors", fieldErrors);
        return super.handleMethodArgumentNotValid(ex, headers, status, request);
    }

    /** Thiếu tham số truy vấn bắt buộc (vd DELETE …?version=) → 400 VALIDATION_FAILED, fieldErrors[param, REQUIRED]. */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ex.getBody();
        body.setDetail("Một hoặc nhiều trường không hợp lệ.");
        body.setProperty("fieldErrors", List.of(Map.of("field", ex.getParameterName(), "code", "REQUIRED",
                "message", "Trường này là bắt buộc.")));
        return super.handleMissingServletRequestParameter(ex, headers, status, request);
    }

    /** Tham số sai kiểu (vd limit=abc, id không phải UUID) → 400 VALIDATION_FAILED, fieldErrors[tên, INVALID_VALUE]. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String field = ex.getPropertyName() == null ? "parameter" : ex.getPropertyName();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Một hoặc nhiều trường không hợp lệ.");
        problem.setProperty("fieldErrors", List.of(Map.of("field", field, "code", "INVALID_VALUE",
                "message", "Giá trị không hợp lệ.")));
        return handleExceptionInternal(ex, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Bổ sung type/title/errorCode/correlationId cho mọi lỗi giao thức chuẩn của Spring. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            ErrorCode code = codeFor(ex, statusCode);
            problem.setType(URI.create(code.typeUri()));
            problem.setTitle(code.title());
            problem.setProperty("errorCode", code.name());
            problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        }
        return response;
    }

    private static ErrorCode codeFor(Exception ex, HttpStatusCode status) {
        if (ex instanceof MethodArgumentNotValidException || ex instanceof HandlerMethodValidationException
                || ex instanceof MissingServletRequestParameterException || ex instanceof TypeMismatchException) {
            return ErrorCode.VALIDATION_FAILED;
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return ErrorCode.MALFORMED_REQUEST;
        }
        int value = status.value();
        if (value == HttpStatus.NOT_FOUND.value()) {
            return ErrorCode.NOT_FOUND;
        }
        if (value == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (value == HttpStatus.CONTENT_TOO_LARGE.value()) {
            return ErrorCode.PAYLOAD_TOO_LARGE;
        }
        if (value == HttpStatus.SERVICE_UNAVAILABLE.value()) {
            return ErrorCode.SERVICE_UNAVAILABLE;
        }
        return value >= 500 ? ErrorCode.INTERNAL_ERROR : ErrorCode.REQUEST_REJECTED;
    }

    /** Ánh xạ mã ràng buộc Bean Validation sang mã ổn định của API (REQUIRED, SIZE, PATTERN, INVALID_VALUE). */
    static String fieldErrorCode(String constraint) {
        if (constraint == null) {
            return "INVALID_VALUE";
        }
        return switch (constraint) {
            case "NotNull", "NotBlank", "NotEmpty" -> "REQUIRED";
            case "Size", "Length", "Min", "Max", "Range" -> "SIZE";
            case "Pattern" -> "PATTERN";
            default -> "INVALID_VALUE";
        };
    }

    private static ProblemDetail problem(HttpStatusCode status, ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(code.typeUri()));
        problem.setTitle(code.title());
        problem.setProperty("errorCode", code.name());
        problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
        return problem;
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
