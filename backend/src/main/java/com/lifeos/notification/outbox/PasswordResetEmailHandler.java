package com.lifeos.notification.outbox;

import com.lifeos.common.mail.EmailSender;
import com.lifeos.common.mail.MailProperties;
import com.lifeos.common.outbox.OutboxEvent;
import java.time.Clock;
import org.springframework.stereotype.Component;

/** Email đặt lại mật khẩu (UC-ID-06; token hạn 1 giờ, SECURITY_RULES §3.4). */
@Component
class PasswordResetEmailHandler extends SecurityEmailHandler {

    static final String PATH = "/reset-password";

    PasswordResetEmailHandler(EmailSender sender, MailProperties mail, Clock clock) {
        super(sender, mail, clock);
    }

    @Override
    public String eventType() {
        return "PASSWORD_RESET";
    }

    @Override
    String subject() {
        return "Đặt lại mật khẩu LifeOS";
    }

    @Override
    String body(OutboxEvent event, String displayName) {
        return "Chào " + displayName + ",\n\n"
                + "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản LifeOS của bạn. Mở liên kết sau trong"
                + " vòng 1 giờ để đặt mật khẩu mới:\n\n"
                + link(PATH, event) + "\n\n"
                + "Nếu bạn không yêu cầu, hãy bỏ qua email này; mật khẩu hiện tại không thay đổi.\n\n"
                + "— LifeOS\n";
    }
}
