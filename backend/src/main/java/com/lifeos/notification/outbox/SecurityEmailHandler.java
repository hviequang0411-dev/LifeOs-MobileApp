package com.lifeos.notification.outbox;

import com.lifeos.common.mail.EmailMessage;
import com.lifeos.common.mail.EmailSender;
import com.lifeos.common.mail.MailProperties;
import com.lifeos.common.outbox.OutboxEvent;
import com.lifeos.common.outbox.OutboxEventHandler;
import com.lifeos.common.outbox.OutboxPermanentFailure;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Email bảo mật (loại SECURITY, PHASE_4 D140): chuyển từ module identity sang notification ở WP4 (BACKEND_MASTER_PLAN §5
 * #5, D120) — hành vi D76 KHÔNG đổi. identity chỉ ghi sự kiện outbox (EMAIL_VERIFICATION, PASSWORD_RESET,
 * PASSWORD_CHANGED); handler đọc payload, dựng email văn bản thuần từ mẫu cố định (SECURITY_RULES §9.5–9.6) và gửi qua
 * {@link EmailSender} — NGOÀI transaction DB. SECURITY bỏ qua tùy chọn và giờ yên tĩnh.
 * <ul>
 *   <li>Payload thiếu trường → INVALID_PAYLOAD (FAILED ngay).</li>
 *   <li>Liên kết có token đã hết hạn → TOKEN_EXPIRED (FAILED ngay, không gửi liên kết chết) [PROPOSED].</li>
 * </ul>
 */
abstract class SecurityEmailHandler implements OutboxEventHandler {

    static final String INVALID_PAYLOAD = "INVALID_PAYLOAD";
    static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";

    private final EmailSender sender;
    private final MailProperties mail;
    private final Clock clock;

    SecurityEmailHandler(EmailSender sender, MailProperties mail, Clock clock) {
        this.sender = sender;
        this.mail = mail;
        this.clock = clock;
    }

    @Override
    public final void handle(OutboxEvent event) throws Exception {
        String to = required(event, "email");
        sender.send(new EmailMessage(event.id(), to, subject(), body(event, displayName(event))));
    }

    abstract String subject();

    abstract String body(OutboxEvent event, String displayName);

    /** Liên kết {@code APP_BASE_URL/path#token=…}: token nằm sau '#' nên không đi vào log máy chủ/proxy. */
    String link(String path, OutboxEvent event) {
        Instant expiresAt = instant(event, "expiresAt");
        if (!expiresAt.isAfter(clock.instant())) {
            throw new OutboxPermanentFailure(TOKEN_EXPIRED);
        }
        return mail.appBaseUrl() + path + "#token=" + required(event, "token");
    }

    static String required(OutboxEvent event, String key) {
        Object value = event.payload().get(key);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new OutboxPermanentFailure(INVALID_PAYLOAD);
        }
        return text;
    }

    static Instant instant(OutboxEvent event, String key) {
        try {
            return Instant.parse(required(event, key));
        } catch (DateTimeParseException e) {
            throw new OutboxPermanentFailure(INVALID_PAYLOAD);
        }
    }

    /** Tên hiển thị là dữ liệu người dùng: bỏ ký tự điều khiển (CR/LF…), giới hạn độ dài (SECURITY_RULES §9.5). */
    static String displayName(OutboxEvent event) {
        Object raw = event.payload().get("displayName");
        String name = raw instanceof String text ? text.replaceAll("\\p{Cntrl}", " ").strip() : "";
        if (name.isEmpty()) {
            return "bạn";
        }
        return name.codePointCount(0, name.length()) > 100 ? name.substring(0, name.offsetByCodePoints(0, 100)) : name;
    }
}
