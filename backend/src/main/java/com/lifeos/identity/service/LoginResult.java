package com.lifeos.identity.service;

import com.lifeos.identity.dto.UserSummary;

/** Kết quả đăng nhập: cặp token và tóm tắt user (API_CONVENTIONS §8.1). */
public record LoginResult(TokenPair tokens, UserSummary user) {
}
