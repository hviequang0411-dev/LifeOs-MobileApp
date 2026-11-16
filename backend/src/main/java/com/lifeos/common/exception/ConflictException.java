package com.lifeos.common.exception;

/** 409: xung đột phiên bản, id đã tồn tại, trùng tên... */
public class ConflictException extends BusinessException {

    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConflictException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }
}
