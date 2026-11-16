package com.lifeos.common.exception;

import org.springframework.http.HttpStatus;

/** 403. Hiếm dùng: dữ liệu của user khác phải trả 404, không phải 403. */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(ErrorCode errorCode, String detail) {
        super(errorCode, HttpStatus.FORBIDDEN, detail);
    }
}
