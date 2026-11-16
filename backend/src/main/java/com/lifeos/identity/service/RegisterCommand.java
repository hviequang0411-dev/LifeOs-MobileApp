package com.lifeos.identity.service;

/** Đầu vào use case đăng ký (UC-ID-01). clientIp lấy từ kết nối (RemoteIpValve), không từ body. */
public record RegisterCommand(String email, String password, String displayName, String timezone, String clientIp) {

    @Override
    public String toString() {
        // Không bao giờ in mật khẩu (SECURITY_RULES §11.1).
        return "RegisterCommand[timezone=" + timezone + "]";
    }
}
