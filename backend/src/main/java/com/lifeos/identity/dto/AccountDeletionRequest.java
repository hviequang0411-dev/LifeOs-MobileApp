package com.lifeos.identity.dto;

import jakarta.validation.constraints.NotNull;

/** POST /account/deletion: mật khẩu hiện tại + mã xác nhận gửi qua email (D205). toString không in bí mật. */
public record AccountDeletionRequest(@NotNull String password, @NotNull String code) {

    @Override
    public String toString() {
        return "AccountDeletionRequest[]";
    }
}
