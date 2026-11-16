package com.lifeos.notification.outbox;

import com.lifeos.common.mail.EmailSender;
import com.lifeos.common.mail.MailProperties;
import com.lifeos.common.outbox.OutboxEvent;
import com.lifeos.common.outbox.OutboxPermanentFailure;
import java.time.Clock;
import org.springframework.stereotype.Component;

/** PHASE_7 D205: email chứa mã xác nhận xóa tài khoản (6 chữ số, 15 phút). Mã hết hạn ⇒ không gửi (TOKEN_EXPIRED). */
@Component
class AccountDeletionCodeEmailHandler extends SecurityEmailHandler {

    private final Clock clock;

    AccountDeletionCodeEmailHandler(EmailSender sender, MailProperties mail, Clock clock) {
        super(sender, mail, clock);
        this.clock = clock;
    }

    @Override
    public String eventType() {
        return "ACCOUNT_DELETION_CODE";
    }

    @Override
    String subject() {
        return "Mã xác nhận xóa tài khoản LifeOS";
    }

    @Override
    String body(OutboxEvent event, String displayName) {
        if (!instant(event, "expiresAt").isAfter(clock.instant())) {
            throw new OutboxPermanentFailure(TOKEN_EXPIRED);
        }
        return "Chào " + displayName + ",\n\n"
                + "Mã xác nhận yêu cầu xóa tài khoản LifeOS của bạn là: " + required(event, "token") + "\n"
                + "Mã có hiệu lực trong 15 phút.\n\n"
                + "Nếu không phải bạn yêu cầu, hãy bỏ qua email này và đổi mật khẩu ngay.\n\n"
                + "— LifeOS\n";
    }
}
