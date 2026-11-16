package com.lifeos.ai.service;

import com.lifeos.common.exception.BusinessException;
import com.lifeos.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

final class AiErrors {
    private AiErrors() {
    }

    static BusinessException error(String code, HttpStatus status, String detail) {
        return new BusinessException(ErrorCode.REQUEST_REJECTED, status, detail).with("errorCode", code);
    }

    static BusinessException conversationNotFound() {
        return error("AI_CONVERSATION_NOT_FOUND", HttpStatus.NOT_FOUND,
                "Không tìm thấy cuộc hội thoại AI");
    }
}
