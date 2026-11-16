package com.lifeos.common.exception;

/** 404. Cũng dùng khi tài nguyên thuộc user khác (không phân biệt để không lộ sự tồn tại). */
public class NotFoundException extends BusinessException {

    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
