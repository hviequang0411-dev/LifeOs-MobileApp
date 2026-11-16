package com.lifeos.common.web;

import com.lifeos.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;

/**
 * Ghi lỗi RFC 9457 trực tiếp ra response cho các filter nằm ngoài Spring MVC (không đi qua GlobalExceptionHandler).
 * Nội dung chỉ gồm giá trị cố định của ErrorCode và correlationId (UUID đã chuẩn hóa), không chèn dữ liệu từ request.
 */
public final class ProblemResponses {

    private ProblemResponses() {
    }

    public static void write(HttpServletResponse response, ErrorCode code) throws IOException {
        write(response, code, null);
    }

    public static void write(HttpServletResponse response, ErrorCode code, Long retryAfterSeconds)
            throws IOException {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        int status = code.defaultStatus().value();
        String body = "{\"type\":\"" + escape(code.typeUri()) + "\","
                + "\"title\":\"" + escape(code.title()) + "\","
                + "\"status\":" + status + ","
                + "\"errorCode\":\"" + code.name() + "\","
                + "\"correlationId\":" + (correlationId == null ? "null" : "\"" + escape(correlationId) + "\"")
                + "}";
        response.setStatus(status);
        if (retryAfterSeconds != null) {
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/problem+json");
        response.getWriter().write(body);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
