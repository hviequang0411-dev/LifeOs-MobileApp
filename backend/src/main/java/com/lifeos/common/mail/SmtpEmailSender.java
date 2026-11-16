package com.lifeos.common.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Gửi email qua SMTP (spring.mail.*: SMTP_HOST/PORT/USER/PASSWORD; dev: mailpit cổng 1025). Thời gian chờ kết nối/đọc
 * /ghi giới hạn trong cấu hình để một lần gửi không giữ worker quá lâu (nhỏ hơn nhiều so với lease outbox).
 * Message-ID = {@code <idempotencyKey@outbox.lifeos>}: gửi lại cùng sự kiện mang cùng định danh.
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSenderImpl mailSender;
    private final MailProperties properties;

    public SmtpEmailSender(JavaMailSenderImpl mailSender, MailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage message) throws MessagingException {
        String messageId = "<" + message.idempotencyKey() + "@outbox.lifeos>";
        MimeMessage mime = new FixedIdMimeMessage(mailSender.getSession(), messageId);
        mime.setFrom(new InternetAddress(properties.from(), true));
        mime.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(message.to(), true));
        mime.setSubject(message.subject(), StandardCharsets.UTF_8.name());
        mime.setText(message.text(), StandardCharsets.UTF_8.name());
        mailSender.send(mime);
    }

    /** JavaMail tự sinh Message-ID khi lưu; lớp này giữ định danh theo sự kiện outbox. */
    private static final class FixedIdMimeMessage extends MimeMessage {

        private final String messageId;

        FixedIdMimeMessage(Session session, String messageId) {
            super(session);
            this.messageId = messageId;
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            setHeader("Message-ID", messageId);
        }
    }
}
