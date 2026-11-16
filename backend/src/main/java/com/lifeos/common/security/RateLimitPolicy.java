package com.lifeos.common.security;

import java.time.Duration;

/**
 * Bảng giới hạn tần suất (SECURITY_RULES §6, D49, D74). Mỗi hằng số là MỘT dòng của bảng = một bucket; các endpoint
 * cùng dòng dùng chung bucket theo khóa của dòng đó. Đổi giá trị ở đây phải đổi tài liệu trước.
 */
public enum RateLimitPolicy {

    /** login, khóa (email, IP): 5 lần sai / 15 phút. */
    LOGIN_FAILURE_EMAIL_IP(5, Duration.ofMinutes(15)),
    /** login, khóa email (mọi IP): 20 lần sai / 15 phút. */
    LOGIN_FAILURE_EMAIL(20, Duration.ofMinutes(15)),
    /** login, khóa IP (mọi email): 50 lần sai / 15 phút. */
    LOGIN_FAILURE_IP(50, Duration.ofMinutes(15)),
    /** register, khóa IP: 10 / giờ (bucket riêng của register). */
    REGISTER_IP(10, Duration.ofHours(1)),
    /** forgot-password + resend-verification, khóa email: 5 / giờ; vượt thì phản hồi thành công giả, không gửi email. */
    EMAIL_SEND_PER_EMAIL(5, Duration.ofHours(1)),
    /** forgot-password + resend-verification + register, khóa IP: 20 / giờ (bucket chung). */
    EMAIL_ENDPOINTS_IP(20, Duration.ofHours(1)),
    /** reset-password + verify-email, khóa IP: 20 / giờ (bucket chung). */
    TOKEN_ENDPOINTS_IP(20, Duration.ofHours(1)),
    /** logout, khóa IP: 60 / giờ (bucket riêng, D74). */
    LOGOUT_IP(60, Duration.ofHours(1)),
    /** change-password, khóa user, chỉ đếm lần nhập sai mật khẩu hiện tại: 5 / 15 phút (D74). */
    CHANGE_PASSWORD_FAILURE_USER(5, Duration.ofMinutes(15)),
    /** refresh, khóa IP: 60 / giờ (quyết định (c)). */
    REFRESH_IP(60, Duration.ofHours(1)),
    /** API đã xác thực, khóa user: 600 / phút. */
    API_USER(600, Duration.ofMinutes(1)),
    /** PUT /devices/{id}/push-token, khóa user: 20 / giờ (PHASE_4 §8). */
    PUSH_TOKEN_USER(20, Duration.ofHours(1)),
    /** GET /notifications/unsubscribe (công khai), khóa IP: 20 / giờ (D143). */
    UNSUBSCRIBE_IP(20, Duration.ofHours(1)),
    /** POST /sync/snapshot (bắt đầu phiên), khóa user: 6 / giờ (PHASE_5 D174). */
    SYNC_SNAPSHOT_USER(6, Duration.ofHours(1)),
    /** GET /sync/entities/{type}/{id}, khóa user: 120 / phút (D174). */
    SYNC_ENTITY_USER(120, Duration.ofMinutes(1)),
    /** POST /sync/diagnostics, khóa user: 5 / giờ (D170). */
    SYNC_DIAGNOSTICS_USER(5, Duration.ofHours(1)),
    /** GET /notes?q= (tìm kiếm toàn văn), khóa user: 60 / phút (PHASE_6 §8). */
    NOTE_SEARCH_USER(60, Duration.ofMinutes(1)),
    /** PHASE_7 D208: GET /dashboard 30 lần/phút/user. */
    DASHBOARD_USER(30, Duration.ofMinutes(1)),
    /** PHASE_7 D205: mã xác nhận xóa tài khoản gửi qua email, 5 lần/giờ/user. */
    ACCOUNT_DELETION_CODE_USER(5, Duration.ofHours(1)),
    /** PHASE_7 D205: sai mật khẩu/mã khi xác nhận xóa, 5 lần/15 phút/user (đếm thất bại). */
    ACCOUNT_DELETION_FAILURE_USER(5, Duration.ofMinutes(15));

    private final int limit;
    private final Duration window;

    RateLimitPolicy(int limit, Duration window) {
        this.limit = limit;
        this.window = window;
    }

    public int limit() {
        return limit;
    }

    public Duration window() {
        return window;
    }
}
