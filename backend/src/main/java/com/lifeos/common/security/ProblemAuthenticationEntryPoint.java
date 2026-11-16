package com.lifeos.common.security;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.web.ProblemResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Trả 401 theo RFC 9457 khi endpoint cần xác thực mà request không có danh tính hợp lệ. JwtAuthFilter đặt mã lỗi cụ
 * thể (AUTH_TOKEN_EXPIRED / AUTH_TOKEN_INVALID) vào request attribute; thiếu token → AUTH_TOKEN_INVALID.
 */
@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** Request attribute chứa ErrorCode do JwtAuthFilter xác định. */
    public static final String ERROR_CODE_ATTRIBUTE = ProblemAuthenticationEntryPoint.class.getName() + ".errorCode";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        ErrorCode code = request.getAttribute(ERROR_CODE_ATTRIBUTE) == ErrorCode.AUTH_TOKEN_EXPIRED
                ? ErrorCode.AUTH_TOKEN_EXPIRED
                : ErrorCode.AUTH_TOKEN_INVALID;
        ProblemResponses.write(response, code);
    }
}
