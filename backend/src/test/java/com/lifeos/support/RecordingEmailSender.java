package com.lifeos.support;

import com.lifeos.common.mail.EmailMessage;
import com.lifeos.common.mail.EmailSender;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stub gửi email cho test tích hợp (TESTING_RULES §2.3, §4.5: không gọi mạng ngoài; email dùng stub ghi lại lời gọi).
 * Ghi mọi email "đã gửi"; có thể cấu hình để một người nhận thất bại N lần (lỗi tạm thời) trước khi thành công.
 */
public class RecordingEmailSender implements EmailSender {

    /** Lỗi gửi giả lập (lỗi tạm thời: worker thử lại). */
    public static final class SimulatedSmtpFailure extends RuntimeException {
        SimulatedSmtpFailure() {
            super("simulated");
        }
    }

    private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();
    private final Map<String, AtomicInteger> failuresLeft = new ConcurrentHashMap<>();

    @Override
    public void send(EmailMessage message) {
        AtomicInteger left = failuresLeft.get(message.to());
        if (left != null && left.getAndDecrement() > 0) {
            throw new SimulatedSmtpFailure();
        }
        sent.add(message);
    }

    /** Lần gửi tới {@code recipient} thất bại {@code times} lần tiếp theo. */
    public void failNext(String recipient, int times) {
        failuresLeft.put(recipient, new AtomicInteger(times));
    }

    public List<EmailMessage> sentTo(String recipient) {
        return sent.stream().filter(m -> m.to().equals(recipient)).toList();
    }

    public List<EmailMessage> all() {
        return List.copyOf(sent);
    }
}
