package com.lifeos.notification.outbox;

import com.lifeos.common.mail.EmailSender;
import com.lifeos.common.mail.MailProperties;
import com.lifeos.common.outbox.OutboxEvent;
import java.time.Clock;
import org.springframework.stereotype.Component;

/** Email xác minh địa chỉ (UC-ID-01/02; token hạn 24 giờ, SECURITY_RULES §3.4). */
@Component
class VerificationEmailHandler extends SecurityEmailHandler {

    static final String PATH = "/verify-email";

    VerificationEmailHandler(EmailSender sender, MailProperties mail, Clock clock) {
        super(sender, mail, clock);
    }

    @Override
    public String eventType() {
        return "EMAIL_VERIFICATION";
    }

    @Override
    String subject() {
        return "Xác minh địa chỉ email LifeOS";
    }

    @Override
    String body(OutboxEvent event, String displayName) {
        return "Chào " + displayName + ",\n\n"
                + "Vui lòng xác minh địa chỉ email để bắt đầu dùng LifeOS bằng cách mở liên kết sau trong vòng"
                + " 24 giờ:\n\n"
                + link(PATH, event) + "\n\n"
                + "Nếu bạn không đăng ký LifeOS, hãy bỏ qua email này.\n\n"
                + "— LifeOS\n";
    }
}
