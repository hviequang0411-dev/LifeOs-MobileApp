package com.lifeos.identity.dto;

import java.util.UUID;

/** Phần "user" trong phản hồi đăng nhập. */
public record UserSummary(UUID id, String email, String displayName) {
}
