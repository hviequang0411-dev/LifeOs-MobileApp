package com.lifeos.common.security;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.UnauthorizedException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Nguồn duy nhất của userId cho mọi truy vấn dữ liệu cá nhân (AGENTS.md §4: user_id không nhận từ request body).
 */
@Component
public class CurrentUser {

    public AuthenticatedUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new UnauthorizedException(ErrorCode.AUTH_TOKEN_INVALID);
    }

    public UUID userId() {
        return require().userId();
    }
}
