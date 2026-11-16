package com.lifeos.identity.service;

import java.util.UUID;

/** Đầu vào use case đăng nhập (UC-ID-03). deviceId tùy chọn: id server đã cấp ở lần trước. */
public record LoginCommand(String email, String password, UUID deviceId, DeviceInfo device, String clientIp) {

    @Override
    public String toString() {
        return "LoginCommand[deviceId=" + deviceId + "]";
    }
}
