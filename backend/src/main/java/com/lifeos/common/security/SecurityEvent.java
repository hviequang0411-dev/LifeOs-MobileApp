package com.lifeos.common.security;

/** Loại sự kiện bảo mật ghi vào log ở Phase 1 (SECURITY_RULES §11.3, D53). */
public enum SecurityEvent {
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    RATE_LIMITED,
    REFRESH_TOKEN_REUSED,
    PASSWORD_CHANGED,
    PASSWORD_RESET,
    DEVICE_REVOKED,
    EMAIL_VERIFIED,
    ACCESS_DENIED
}
