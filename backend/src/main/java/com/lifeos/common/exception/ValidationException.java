package com.lifeos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Vi phạm quy tắc nghiệp vụ. Mặc định 422 (API_CONVENTIONS.md §4); dùng constructor có status để trả 400.
 * Lỗi định dạng/ràng buộc trường là việc của Bean Validation (400 VALIDATION_FAILED), không dùng lớp này.
 */
public class ValidationException extends BusinessException {

    public ValidationException(ErrorCode errorCode, String detail) {
        super(errorCode, HttpStatus.UNPROCESSABLE_CONTENT, detail);
    }

    public ValidationException(ErrorCode errorCode, HttpStatus status, String detail) {
        super(errorCode, status, detail);
    }
}
