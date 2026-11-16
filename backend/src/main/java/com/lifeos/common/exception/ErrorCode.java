package com.lifeos.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Danh mục errorCode ổn định (API_CONVENTIONS.md §5.1). Tên enum chính là errorCode trả cho client.
 * Thêm mã mới phải cập nhật tài liệu cùng PR.
 */
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không đúng định dạng"),

    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    AUTH_EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "Email chưa được xác minh"),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn"),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc phiên không hợp lệ"),
    AUTH_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Phiên làm mới không hợp lệ"),
    AUTH_VERIFICATION_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Mã xác minh không hợp lệ hoặc đã hết hạn"),
    AUTH_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Mã đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"),
    AUTH_PASSWORD_TOO_WEAK(HttpStatus.BAD_REQUEST, "Mật khẩu chưa đạt yêu cầu"),
    AUTH_CURRENT_PASSWORD_WRONG(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng"),

    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Quá nhiều yêu cầu"),
    CLIENT_UPGRADE_REQUIRED(HttpStatus.UPGRADE_REQUIRED, "Cần cập nhật ứng dụng"),

    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy công việc"),
    PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dự án"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục"),
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thiết bị"),

    // Phase 2 Habit & Routine (PHASE_2_HABIT_ROUTINE.md §6)
    HABIT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thói quen"),
    HABIT_CHECKIN_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy lượt đánh dấu"),
    HABIT_INVALID_SCHEDULE(HttpStatus.UNPROCESSABLE_CONTENT, "Lịch thói quen không hợp lệ"),
    HABIT_DATE_OUT_OF_RANGE(HttpStatus.UNPROCESSABLE_CONTENT, "Ngày đánh dấu nằm ngoài khoảng cho phép"),
    ROUTINE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy routine"),
    ROUTINE_STEP_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bước"),
    ROUTINE_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy lượt chạy"),
    ROUTINE_INVALID_SCHEDULE(HttpStatus.UNPROCESSABLE_CONTENT, "Lịch routine không hợp lệ"),
    LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_CONTENT, "Vượt giới hạn số lượng cho phép"),
    // Phase 3 Finance (PHASE_3_FINANCE.md §6, D114). *_NOT_FOUND là 422 khi là tham chiếu trong body/payload.
    FINANCE_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản"),
    FINANCE_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục thu chi"),
    FINANCE_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch"),
    FINANCE_TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy chuyển khoản"),
    FINANCE_BUDGET_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy ngân sách"),
    FINANCE_RECURRING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy giao dịch định kỳ"),
    FINANCE_GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy mục tiêu tiết kiệm"),
    FINANCE_ACCOUNT_ARCHIVED(HttpStatus.UNPROCESSABLE_CONTENT, "Tài khoản đã lưu trữ"),
    FINANCE_ACCOUNT_HAS_TRANSACTIONS(HttpStatus.CONFLICT, "Tài khoản đã có giao dịch"),
    FINANCE_CATEGORY_IN_USE(HttpStatus.CONFLICT, "Danh mục đang được dùng"),
    FINANCE_CATEGORY_KIND_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "Danh mục không đúng loại thu/chi"),
    FINANCE_CURRENCY_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "Tiền tệ không khớp"),
    FINANCE_CURRENCY_LOCKED(HttpStatus.CONFLICT, "Không thể đổi tiền tệ khi đã có giao dịch"),
    FINANCE_BUDGET_EXISTS(HttpStatus.CONFLICT, "Đã có ngân sách đang hoạt động cho danh mục và tiền tệ này"),
    FINANCE_NAME_TAKEN(HttpStatus.CONFLICT, "Tên đã được dùng"),
    // Phase 4 Calendar & Notification (PHASE_4_CALENDAR_NOTIFICATION.md §6)
    CALENDAR_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy sự kiện"),
    CALENDAR_INVALID_RECURRENCE(HttpStatus.UNPROCESSABLE_CONTENT, "Quy tắc lặp không hợp lệ"),
    CALENDAR_INVALID_TIMEZONE(HttpStatus.UNPROCESSABLE_CONTENT, "Múi giờ không hợp lệ"),
    CALENDAR_OCCURRENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy lần xuất hiện"),
    TIME_BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy khối thời gian"),
    REMINDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy nhắc nhở"),
    REMINDER_TARGET_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "Không tìm thấy đối tượng của nhắc nhở"),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"),
    NOTIFICATION_PREFERENCE_LOCKED(HttpStatus.UNPROCESSABLE_CONTENT, "Không thể tắt thông báo bảo mật"),
    // Phase 6 Knowledge & Goals (PHASE_6_KNOWLEDGE_GOALS.md §6)
    NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy ghi chú"),
    TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thẻ"),
    TAG_NAME_TAKEN(HttpStatus.CONFLICT, "Tên thẻ đã được dùng"),
    NOTE_TAG_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_CONTENT, "Vượt số thẻ hoặc liên kết tối đa của ghi chú"),
    GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy mục tiêu"),
    MILESTONE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy cột mốc"),
    GOAL_INVALID_STATUS_TRANSITION(HttpStatus.UNPROCESSABLE_CONTENT, "Không thể chuyển trạng thái mục tiêu"),
    MILESTONE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_CONTENT, "Vượt số cột mốc tối đa của mục tiêu"),
    GOAL_LINK_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "Cột mốc không thuộc mục tiêu"),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bản tổng kết"),
    REVIEW_EXISTS(HttpStatus.CONFLICT, "Kỳ này đã có bản tổng kết"),
    ANALYTICS_RANGE_TOO_LARGE(HttpStatus.UNPROCESSABLE_CONTENT, "Khoảng thời gian thống kê quá lớn"),
    EXPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu xuất dữ liệu"),
    EXPORT_LIMIT_REACHED(HttpStatus.TOO_MANY_REQUESTS, "Đã đạt số lần xuất dữ liệu cho phép trong ngày"),
    EXPORT_NOT_READY(HttpStatus.CONFLICT, "Tệp xuất dữ liệu chưa sẵn sàng"),
    EXPORT_EXPIRED(HttpStatus.GONE, "Liên kết tải dữ liệu đã hết hạn"),
    ACCOUNT_DELETION_PENDING(HttpStatus.FORBIDDEN, "Tài khoản đang chờ xóa"),
    ACCOUNT_DELETION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không có yêu cầu xóa tài khoản"),
    ENTITY_VERSION_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu đã được thay đổi ở nơi khác"),
    ENTITY_ALREADY_EXISTS(HttpStatus.CONFLICT, "Dữ liệu đã tồn tại"),
    CATEGORY_NAME_TAKEN(HttpStatus.CONFLICT, "Tên danh mục đã được dùng"),

    PAYLOAD_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "Dữ liệu gửi lên quá lớn"),
    SYNC_BATCH_TOO_LARGE(HttpStatus.BAD_REQUEST, "Lô đồng bộ vượt giới hạn"),
    SYNC_UNKNOWN_ENTITY_TYPE(HttpStatus.BAD_REQUEST, "Loại dữ liệu đồng bộ không được hỗ trợ"),
    // Phase 5 (PHASE_5_SYNC_ADVANCED.md §6)
    SYNC_FULL_RESYNC_REQUIRED(HttpStatus.GONE, "Cần đồng bộ lại toàn bộ"),
    SYNC_SNAPSHOT_EXPIRED(HttpStatus.GONE, "Phiên tải dữ liệu đã hết hạn"),
    // Chỉ xuất hiện trong kết quả mutation sync (D88); HTTP status không dùng.
    SYNC_OPERATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Thao tác không được phép với loại dữ liệu này"),
    USER_SETTINGS_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy cài đặt"),

    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Hệ thống tạm thời chưa sẵn sàng"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống"),

    // Mã chung cho lỗi giao thức chuẩn của Spring (skeleton bổ sung; xem API_CONVENTIONS.md §5.1)
    NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức không được hỗ trợ"),
    REQUEST_REJECTED(HttpStatus.BAD_REQUEST, "Yêu cầu bị từ chối");

    private final HttpStatus defaultStatus;
    private final String title;

    ErrorCode(HttpStatus defaultStatus, String title) {
        this.defaultStatus = defaultStatus;
        this.title = title;
    }

    public HttpStatus defaultStatus() {
        return defaultStatus;
    }

    public String title() {
        return title;
    }

    /** URI ổn định cho trường "type" của Problem Details. */
    public String typeUri() {
        return "https://lifeos.app/problems/" + name().toLowerCase().replace('_', '-');
    }
}
