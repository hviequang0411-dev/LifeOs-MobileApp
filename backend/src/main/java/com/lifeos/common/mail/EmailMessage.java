package com.lifeos.common.mail;

import java.util.Objects;
import java.util.UUID;

/**
 * Một email văn bản thuần (SECURITY_RULES §9.5–9.6: mẫu cố định, không HTML nên không có chèn mã). idempotencyKey (id
 * sự kiện outbox) thành Message-ID để lần gửi lại do at-least-once mang cùng định danh. toString không in địa chỉ hay
 * nội dung (SECURITY_RULES §11.1).
 */
public record EmailMessage(UUID idempotencyKey, String to, String subject, String text) {

    public EmailMessage {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(text, "text");
        if (subject.indexOf('\r') >= 0 || subject.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Tiêu đề email không được chứa CR/LF");
        }
    }

    @Override
    public String toString() {
        return "EmailMessage[idempotencyKey=" + idempotencyKey + "]";
    }
}
