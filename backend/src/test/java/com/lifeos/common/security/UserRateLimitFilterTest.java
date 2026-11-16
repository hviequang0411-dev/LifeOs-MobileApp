package com.lifeos.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lifeos.support.MutableClock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** API đã xác thực: 600 yêu cầu / phút / user (SECURITY_RULES §6). */
class UserRateLimitFilterTest {

    private final RateLimiter limiter = new RateLimiter(new MutableClock(Instant.parse("2026-11-06T10:00:00Z")));
    private final UserRateLimitFilter filter = new UserRateLimitFilter(limiter, new SecurityEventLogger());

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(userId, UUID.randomUUID()), null, List.of()));
    }

    private MockHttpServletResponse call() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/me"), response, new MockFilterChain());
        return response;
    }

    @Test
    void request601WithinAMinuteIsRejectedWith429AndRetryAfter() throws Exception {
        UUID user = UUID.randomUUID();
        authenticateAs(user);
        for (int i = 0; i < 600; i++) {
            assertThat(call().getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse rejected = call();
        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        assertThat(rejected.getContentType()).startsWith("application/problem+json");
        assertThat(rejected.getContentAsString()).contains("\"errorCode\":\"RATE_LIMITED\"");
    }

    @Test
    void usersHaveSeparateBucketsAndAnonymousRequestsAreNotCounted() throws Exception {
        UUID first = UUID.randomUUID();
        authenticateAs(first);
        for (int i = 0; i < 601; i++) {
            call();
        }
        assertThat(call().getStatus()).isEqualTo(429);

        authenticateAs(UUID.randomUUID());
        assertThat(call().getStatus()).isEqualTo(200);

        SecurityContextHolder.clearContext();
        assertThat(call().getStatus()).isEqualTo(200);
    }
}
