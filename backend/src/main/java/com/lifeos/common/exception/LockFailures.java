package com.lifeos.common.exception;

import java.sql.SQLException;
import org.springframework.dao.PessimisticLockingFailureException;

/**
 * Nhận diện lỗi tranh chấp khóa của PostgreSQL (SYNC_PROTOCOL §5.1, D85, D87): hết {@code lock_timeout} (SQLSTATE
 * 55P03) hoặc deadlock (40P01), dù lỗi đến từ SELECT … FOR UPDATE/SHARE, từ flush của Hibernate hay từ JdbcClient.
 * Các lỗi này không phải lỗi dữ liệu của client: trả 503 + Retry-After để client gửi lại.
 */
public final class LockFailures {

    /** Giây client nên chờ trước khi gửi lại (SYNC_PROTOCOL §5.1). */
    public static final long RETRY_AFTER_SECONDS = 2;

    private static final String LOCK_NOT_AVAILABLE = "55P03";
    private static final String DEADLOCK_DETECTED = "40P01";

    private LockFailures() {
    }

    public static boolean isLockFailure(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof PessimisticLockingFailureException) {
                return true;
            }
            if (t instanceof SQLException sql
                    && (LOCK_NOT_AVAILABLE.equals(sql.getSQLState()) || DEADLOCK_DETECTED.equals(sql.getSQLState()))) {
                return true;
            }
            if (t.getCause() == t) {
                return false;
            }
        }
        return false;
    }
}
