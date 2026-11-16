package com.lifeos.identity.service;

import com.lifeos.common.exception.ErrorCode;
import com.lifeos.common.exception.FieldErrors;
import com.lifeos.common.security.PasswordHasher;

/**
 * Chính sách mật khẩu (SECURITY_RULES §3.1, D47): 8–128 ký tự, đếm theo code point SAU khi chuẩn hóa NFKC; không
 * bắt buộc quy tắc ký tự. Vi phạm → 400 AUTH_PASSWORD_TOO_WEAK kèm fieldErrors.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private PasswordPolicy() {
    }

    /** @param field tên trường JSON để báo lỗi (password, newPassword) */
    public static void check(String rawPassword, String field) {
        String normalized = rawPassword == null ? "" : PasswordHasher.normalize(rawPassword);
        int length = normalized.codePointCount(0, normalized.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw FieldErrors.single(ErrorCode.AUTH_PASSWORD_TOO_WEAK, field, "SIZE",
                    "Mật khẩu phải có từ " + MIN_LENGTH + " đến " + MAX_LENGTH + " ký tự.");
        }
    }
}
