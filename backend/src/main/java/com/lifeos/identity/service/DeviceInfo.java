package com.lifeos.identity.service;

/**
 * Thông tin thiết bị do client gửi khi đăng nhập (chỉ để hiển thị, không tin cậy). platform là chuỗi đã qua kiểm tra
 * cú pháp ANDROID|IOS ở DTO; service đổi sang enum của entity (controller không phụ thuộc entity, luật A1).
 */
public record DeviceInfo(String name, String platform, String appVersion) {
}
