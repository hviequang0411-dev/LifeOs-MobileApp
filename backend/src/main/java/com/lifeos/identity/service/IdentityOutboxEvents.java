package com.lifeos.identity.service;

import com.lifeos.common.outbox.OutboxWriter;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Sự kiện outbox của module identity (SYSTEM_ARCHITECTURE §3.11, D37, D76). Payload chứa token thô TẠM THỜI để
 * worker dựng liên kết từ APP_BASE_URL (Giai đoạn 4); worker ghi đè payload thành {} sau khi gửi.
 */
@Component
public class IdentityOutboxEvents {

    public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";
    static final String AGGREGATE_USER = "user";

    private final OutboxWriter outboxWriter;

    public IdentityOutboxEvents(OutboxWriter outboxWriter) {
        this.outboxWriter = outboxWriter;
    }

    public void emailVerification(UUID userId, String email, String displayName, String rawToken,
            Instant expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId.toString());
        payload.put("email", email);
        payload.put("displayName", displayName);
        payload.put("token", rawToken);
        payload.put("expiresAt", expiresAt.toString());
        outboxWriter.write(EMAIL_VERIFICATION, AGGREGATE_USER, userId, payload);
    }

    public void passwordReset(UUID userId, String email, String displayName, String rawToken, Instant expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId.toString());
        payload.put("email", email);
        payload.put("displayName", displayName);
        payload.put("token", rawToken);
        payload.put("expiresAt", expiresAt.toString());
        outboxWriter.write(PASSWORD_RESET, AGGREGATE_USER, userId, payload);
    }

    public static final String ACCOUNT_DELETION_CODE = "ACCOUNT_DELETION_CODE";
    public static final String ACCOUNT_DELETION_SCHEDULED = "ACCOUNT_DELETION_SCHEDULED";
    public static final String SYSTEM_NOTIFICATION = "SYSTEM_NOTIFICATION";

    /** PHASE_7 D205: mã xác nhận xóa tài khoản (payload chứa mã TẠM THỜI; worker ghi đè {} sau khi gửi). */
    public void accountDeletionCode(UUID userId, String email, String displayName, String code, Instant expiresAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId.toString());
        payload.put("email", email);
        payload.put("displayName", displayName);
        payload.put("token", code);
        payload.put("expiresAt", expiresAt.toString());
        outboxWriter.write(ACCOUNT_DELETION_CODE, AGGREGATE_USER, userId, payload);
    }

    /** PHASE_7 D205: email bảo mật "tài khoản sẽ bị xóa lúc …, đăng nhập để hủy". */
    public void accountDeletionScheduled(UUID userId, String email, String displayName, Instant deletionAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId.toString());
        payload.put("email", email);
        payload.put("displayName", displayName);
        payload.put("deletionAt", deletionAt.toString());
        outboxWriter.write(ACCOUNT_DELETION_SCHEDULED, AGGREGATE_USER, userId, payload);
    }

    /** Thông báo in-app loại SYSTEM theo schema D149 (ví dụ tệp xuất dữ liệu đã sẵn sàng, UC-INT-05). */
    public void systemNotification(UUID userId, String templateKey, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId.toString());
        payload.put("category", "SYSTEM");
        payload.put("templateKey", templateKey);
        payload.put("variables", Map.of());
        payload.put("data", data);
        outboxWriter.write(SYSTEM_NOTIFICATION, AGGREGATE_USER, userId, payload);
    }

    /** Email "mật khẩu đã thay đổi" (D54, D76): không chứa token. */
    public void passwordChanged(UUID userId, String email, String displayName, Instant changedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", userId.toString());
        payload.put("email", email);
        payload.put("displayName", displayName);
        payload.put("changedAt", changedAt.toString());
        outboxWriter.write(PASSWORD_CHANGED, AGGREGATE_USER, userId, payload);
    }
}
