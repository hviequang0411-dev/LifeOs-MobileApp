package com.lifeos.common.security;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.web.ProblemResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Giới hạn 600 yêu cầu/phút cho mỗi user đã xác thực (SECURITY_RULES §6, API_USER). Chạy sau JwtAuthFilter; request
 * ẩn danh không bị đếm ở đây (endpoint công khai có bucket riêng theo IP/email trong service).
 * Không đăng ký làm @Component (được gắn vào chuỗi Spring Security trong SecurityConfig).
 */
public class UserRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final SecurityEventLogger securityEvents;

    public UserRateLimitFilter(RateLimiter rateLimiter, SecurityEventLogger securityEvents) {
        this.rateLimiter = rateLimiter;
        this.securityEvents = securityEvents;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            RateLimiter.Decision decision = rateLimiter.tryAcquire(RateLimitPolicy.API_USER, user.userId().toString());
            if (!decision.allowed()) {
                securityEvents.log(SecurityEvent.RATE_LIMITED, user.userId(), user.deviceId(),
                        request.getRemoteAddr());
                ProblemResponses.write(response, ErrorCode.RATE_LIMITED, decision.retryAfterSeconds());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
