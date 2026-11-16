package com.lifeos.common.exception;

/** 429 RATE_LIMITED kèm header Retry-After (SECURITY_RULES §6). */
public class RateLimitedException extends BusinessException {

    private final long retryAfterSeconds;

    public RateLimitedException(long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMITED);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
