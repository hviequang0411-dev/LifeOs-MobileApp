package com.lifeos.common.outbox;

/**
 * Sự kiện không thể xử lý thành công dù thử lại (payload thiếu trường, token đã hết hạn, không có handler…): worker
 * đánh dấu FAILED ngay. reasonCode là mã cố định do mã nguồn đặt (ghi vào last_error), KHÔNG chứa dữ liệu người dùng.
 */
public class OutboxPermanentFailure extends RuntimeException {

    private final String reasonCode;

    public OutboxPermanentFailure(String reasonCode) {
        super(reasonCode);
        this.reasonCode = reasonCode;
    }

    public String reasonCode() {
        return reasonCode;
    }
}
