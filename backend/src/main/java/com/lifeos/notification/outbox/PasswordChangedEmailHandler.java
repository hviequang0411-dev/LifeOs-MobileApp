package com.lifeos.notification.outbox;

import com.lifeos.common.mail.EmailSender;
import com.lifeos.common.mail.MailProperties;
import com.lifeos.common.outbox.OutboxEvent;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** Email "mật khẩu đã được thay đổi" (D54, D76): không chứa token. */
@Component
class PasswordChangedEmailHandler extends SecurityEmailHandler {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("HH:mm dd/MM/uuuu 'UTC'")
            .withZone(ZoneOffset.UTC);

    PasswordChangedEmailHandler(EmailSender sender, MailProperties mail, Clock clock) {
        super(sender, mail, clock);
    }

    @Override
    public String eventType() {
        return "PASSWORD_CHANGED";
    }

    @Override
    String subject() {
        return "Mật khẩu LifeOS của bạn đã được thay đổi";
    }

    @Override
    String body(OutboxEvent event, String displayName) {
        return "Chào " + displayName + ",\n\n"
                + "Mật khẩu tài khoản LifeOS của bạn vừa được thay đổi lúc " + WHEN.format(instant(event, "changedAt"))
                + ".\n\n"
                + "Nếu không phải bạn thực hiện, hãy dùng chức năng \"Quên mật khẩu\" để đặt lại mật khẩu ngay.\n\n"
                + "— LifeOS\n";
    }
}
