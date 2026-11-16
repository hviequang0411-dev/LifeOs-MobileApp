package com.lifeos.common.exception;

import java.util.List;
import java.util.Map;

/**
 * Dựng thuộc tính "fieldErrors" cho lỗi kiểm tra ở tầng service, cùng định dạng với lỗi Bean Validation
 * (API_CONVENTIONS §5): field là tên trường JSON, code ∈ REQUIRED | SIZE | PATTERN | INVALID_VALUE.
 */
public final class FieldErrors {

    public static final String PROPERTY = "fieldErrors";

    private FieldErrors() {
    }

    /** Lỗi 400 với một fieldError. */
    public static ValidationException single(ErrorCode errorCode, String field, String code, String message) {
        ValidationException ex = new ValidationException(errorCode, org.springframework.http.HttpStatus.BAD_REQUEST,
                errorCode.title());
        ex.with(PROPERTY, List.of(Map.of("field", field, "code", code, "message", message)));
        return ex;
    }
}
