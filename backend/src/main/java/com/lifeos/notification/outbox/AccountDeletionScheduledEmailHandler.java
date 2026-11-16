package com.lifeos.notification.outbox;

import com.lifeos.common.mail.EmailSender;
import com.lifeos.common.mail.MailProperties;
import com.lifeos.common.outbox.OutboxEvent;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** PHASE_7 D205: email bảo mật "tài khoản sẽ bị xóa lúc …; đăng nhập để hủy". Không chứa token. */
@Component
class AccountDeletionScheduledEmailHandler extends SecurityEmailHandler {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("HH:mm dd/MM/uuuu 'UTC'")
            .withZone(ZoneOffset.UTC);

    AccountDeletionScheduledEmailHandler(EmailSender sender, MailProperties mail, Clock clock) {
        super(sender, mail, clock);
    }

    @Override
    public String eventType() {
        return "ACCOUNT_DELETION_SCHEDULED";
    }

    @Override
    String subject() {
        return "Tài khoản LifeOS của bạn đang chờ xóa";
    }

    @Override
    String body(OutboxEvent event, String displayName) {
        return "Chào " + displayName + ",\n\n"
                + "Tài khoản LifeOS của bạn sẽ bị xóa vĩnh viễn cùng toàn bộ dữ liệu lúc "
                + WHEN.format(instant(event, "deletionAt")) + ".\n"
                + "Mọi phiên đăng nhập đã bị đăng xuất. Để hủy, hãy đăng nhập lại và chọn \"Hủy xóa tài khoản\".\n\n"
                + "Nếu không phải bạn thực hiện, hãy đăng nhập, hủy yêu cầu và đổi mật khẩu ngay.\n\n"
                + "— LifeOS\n";
    }
}
