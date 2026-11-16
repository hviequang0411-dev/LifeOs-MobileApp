package com.lifeos.common.mail;

/**
 * Cổng gửi email (SYSTEM_ARCHITECTURE §3.11). Chỉ được gọi NGOÀI transaction DB (từ OutboxEventHandler). Ném lỗi khi
 * không gửi được; worker outbox quyết định thử lại. Test thay bằng stub ghi lại lời gọi (TESTING_RULES §2.3).
 */
public interface EmailSender {

    void send(EmailMessage message) throws Exception;
}
