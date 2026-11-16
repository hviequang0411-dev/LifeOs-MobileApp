package com.lifeos.common.security;

import com.lifeos.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Xác thực "Authorization: Bearer &lt;accessToken&gt;" (SYSTEM_ARCHITECTURE §3.10). Không truy vấn DB: chỉ kiểm chữ ký,
 * hạn, iss/aud và tập thiết bị bị thu hồi trong bộ nhớ (D45).
 * Token sai/hết hạn KHÔNG chặn ngay: request tiếp tục như ẩn danh để endpoint công khai (vd /auth/refresh khi access
 * token đã hết hạn) vẫn hoạt động; endpoint cần xác thực sẽ nhận 401 từ entry point với đúng mã lỗi.
 * Không đăng ký làm @Component để không bị Spring Boot gắn thêm vào servlet chain ngoài Spring Security.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final RevokedDeviceRegistry revokedDevices;

    public JwtAuthFilter(JwtService jwtService, RevokedDeviceRegistry revokedDevices) {
        this.jwtService = jwtService;
        this.revokedDevices = revokedDevices;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            authenticate(request, header.substring(BEARER.length()).trim());
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            AuthenticatedUser user = jwtService.verify(token);
            if (revokedDevices.isRevoked(user.deviceId())) {
                log.debug("Access token rejected: device revoked (userId={}, deviceId={})",
                        user.userId(), user.deviceId());
                reject(request, ErrorCode.AUTH_TOKEN_INVALID);
                return;
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
            SecurityContextHolder.setContext(context);
        } catch (JwtValidationException e) {
            // Chỉ log lý do, không bao giờ log token.
            log.debug("Access token rejected: {}", e.getMessage());
            reject(request, e.reason() == JwtValidationException.Reason.EXPIRED
                    ? ErrorCode.AUTH_TOKEN_EXPIRED
                    : ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    private static void reject(HttpServletRequest request, ErrorCode code) {
        SecurityContextHolder.clearContext();
        request.setAttribute(ProblemAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE, code);
    }
}
