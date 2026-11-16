package com.lifeos.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * Gốc của lỗi nghiệp vụ. Mang errorCode ổn định và (tùy chọn) thuộc tính bổ sung đưa vào thân lỗi,
 * ví dụ currentVersion cho ENTITY_VERSION_CONFLICT. Thông điệp (detail) KHÔNG được chứa dữ liệu nhạy cảm.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;
    private final transient Map<String, Object> properties = new LinkedHashMap<>();

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultStatus(), errorCode.title());
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        this(errorCode, errorCode.defaultStatus(), detail);
    }

    public BusinessException(ErrorCode errorCode, HttpStatus status, String detail) {
        super(detail);
        this.errorCode = errorCode;
        this.status = status;
    }

    public BusinessException with(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
