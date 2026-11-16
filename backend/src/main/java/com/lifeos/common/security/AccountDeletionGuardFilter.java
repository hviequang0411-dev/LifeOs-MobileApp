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
 * PHASE_7 D205: trong thời gian chờ xóa, mọi API cần xác thực trả 403 ACCOUNT_DELETION_PENDING — trừ xem trạng thái và
 * hủy ({@code GET/DELETE /api/v1/account/deletion}). Đặt sau JwtAuthFilter.
 */
public class AccountDeletionGuardFilter extends OncePerRequestFilter {

    static final String DELETION_PATH = "/api/v1/account/deletion";

    private final AccountStatusLookup accounts;

    public AccountDeletionGuardFilter(AccountStatusLookup accounts) {
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user
                && !allowed(request) && accounts.deletionPending(user.userId())) {
            ProblemResponses.write(response, ErrorCode.ACCOUNT_DELETION_PENDING);
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean allowed(HttpServletRequest request) {
        String method = request.getMethod();
        return DELETION_PATH.equals(request.getRequestURI()) && ("GET".equals(method) || "DELETE".equals(method));
    }
}
