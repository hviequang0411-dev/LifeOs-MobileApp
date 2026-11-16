package com.lifeos.common.security;

import java.text.Normalizer;

/**
 * Băm mật khẩu (SECURITY_RULES §3.2, D16/D46). Mật khẩu được chuẩn hóa NFKC trước khi băm/so khớp (D47).
 * Không bao giờ lưu hoặc log mật khẩu thô.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String encodedHash);

    /** true nếu hash được tạo với tham số yếu hơn cấu hình hiện tại: băm lại khi đăng nhập thành công. */
    boolean needsRehash(String encodedHash);

    /**
     * Chạy một phép so khớp giả với chi phí như thật (D47): dùng khi email không tồn tại để thời gian phản hồi
     * không lộ sự tồn tại của tài khoản. Kết quả luôn bị bỏ qua.
     */
    void simulateMatch(String rawPassword);

    /** Chuẩn hóa NFKC dùng chung cho băm và kiểm tra chính sách độ dài. */
    static String normalize(String rawPassword) {
        return Normalizer.normalize(rawPassword, Normalizer.Form.NFKC);
    }
}
