package com.lifeos.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

/**
 * Khung bảo mật: stateless, mặc định mọi endpoint cần xác thực, chỉ các endpoint công khai được mở
 * (SECURITY_RULES §7). Thứ tự filter: CorrelationIdFilter → RequestSizeLimitFilter (servlet filter, trước Spring
 * Security) → JwtAuthFilter → UserRateLimitFilter → phân quyền.
 * Header mặc định của Spring Security được giữ: Cache-Control no-store, X-Content-Type-Options nosniff (§10.4).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Danh sách endpoint công khai (API_CONVENTIONS.md §3, SECURITY_RULES §7.1; logout công khai theo quyết định (a)).
     * Thêm route công khai phải cập nhật danh sách này; test AUTHZ-02 kiểm tra.
     */
    public static final String[] PUBLIC_POST_ENDPOINTS = {
        "/api/v1/auth/register",
        "/api/v1/auth/verify-email",
        "/api/v1/auth/resend-verification",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
        "/api/v1/auth/forgot-password",
        "/api/v1/auth/reset-password"
    };

    /** Endpoint GET công khai: liên kết hủy đăng ký email (PHASE_4 D143, token ký HMAC, giới hạn theo IP). */
    public static final String[] PUBLIC_GET_ENDPOINTS = {
        "/api/v1/notifications/unsubscribe"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemAuthenticationEntryPoint entryPoint,
            JwtService jwtService, RevokedDeviceRegistry revokedDevices, RateLimiter rateLimiter,
            SecurityEventLogger securityEvents, AccountStatusLookup accountStatus) throws Exception {
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtService, revokedDevices);
        UserRateLimitFilter userRateLimitFilter = new UserRateLimitFilter(rateLimiter, securityEvents);
        AccountDeletionGuardFilter deletionGuard = new AccountDeletionGuardFilter(accountStatus);
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(handling -> handling.authenticationEntryPoint(entryPoint))
                .addFilterBefore(jwtAuthFilter, AnonymousAuthenticationFilter.class)
                .addFilterAfter(userRateLimitFilter, JwtAuthFilter.class)
                .addFilterAfter(deletionGuard, UserRateLimitFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .anyRequest().authenticated());
        return http.build();
    }

    /** Không có người dùng trong bộ nhớ: chặn Spring Boot tạo user mặc định kèm mật khẩu in ra log. */
    @Bean
    public UserDetailsService noInMemoryUsers() {
        return username -> {
            throw new UsernameNotFoundException("Không hỗ trợ xác thực bằng tên người dùng");
        };
    }
}
