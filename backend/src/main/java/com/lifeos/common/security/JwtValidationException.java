package com.lifeos.common.security;

/** Access token không dùng được. Không mang nội dung token để không lọt vào log. */
public class JwtValidationException extends Exception {

    public enum Reason {
        /** Chữ ký đúng nhưng đã hết hạn (quá cả độ lệch đồng hồ cho phép) → AUTH_TOKEN_EXPIRED. */
        EXPIRED,
        /** Sai định dạng, sai thuật toán, sai chữ ký, sai iss/aud, thiếu claim, kid lạ… → AUTH_TOKEN_INVALID. */
        INVALID
    }

    private final Reason reason;

    public JwtValidationException(Reason reason, String message) {
        super(message, null, false, false);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
