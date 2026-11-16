package com.lifeos.common.privacy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Danh mục MỌI bảng có cột user_id (BACKEND_MASTER_PLAN §3.6; PHASE_2 HAB-12; Phase 7 INT-15). Xuất (D204) và xóa tài
 * khoản (D205) duyệt đúng danh mục này; UserDataCatalogIT so với information_schema và đỏ khi một bảng mới có user_id
 * chưa được khai báo. Thứ tự là thứ tự XÓA an toàn theo khóa ngoại: bảng con trước bảng cha (users xóa sau cùng, ngoài
 * danh mục).
 */
public final class UserDataCatalog {

    /** Bảng có user_id, theo thứ tự xóa (con trước cha). */
    public static final List<String> TABLES = List.of(
            // Phase 8 AI (proposal/usage/message trước conversation; preference độc lập)
            "ai_action_proposals", "ai_usage_logs", "ai_messages", "ai_conversations", "ai_user_preferences",
            // Phase 7 Analytics & tài khoản
            "reviews", "data_exports", "account_deletion_codes",
            // Outbox (đợt sửa F8): sự kiện còn mang userId của người dùng (ví dụ BUDGET_THRESHOLD đang chờ)
            "outbox_events",
            // Phase 2–4
            "notification_deliveries", "notifications", "device_push_tokens", "notification_preferences",
            "reminders", "time_blocks", "event_exceptions", "calendar_events",
            "saving_contributions", "saving_goals", "budget_alerts", "budgets", "financial_transactions",
            "recurring_transactions", "account_transfers", "financial_categories", "financial_accounts",
            "routine_step_logs", "routine_logs", "routine_steps", "routines", "habit_checkins", "habits",
            // Phase 6 Knowledge (note_links/note_tags trước notes/tags)
            "note_links", "note_tags", "notes", "tags",
            // Phase 1 Task (tasks trước milestones/goals: tasks.goal_id, tasks.milestone_id)
            "tasks", "categories", "projects",
            // Phase 6 Goals
            "milestones", "goals",
            // Sync
            "sync_diagnostics", "sync_mutations", "change_logs", "user_sync_state",
            // Identity
            "refresh_tokens", "email_verification_tokens", "password_reset_tokens", "devices", "user_settings");

    /**
     * Bảng KHÔNG xuất (D204) kèm lý do; mọi bảng khác của {@link #TABLES} đều được xuất (INT-15 kiểm hai danh sách phủ
     * kín danh mục). Bảng bị loại vẫn bị xóa khi xóa tài khoản.
     */
    public static final Map<String, String> EXPORT_EXCLUDED = excluded();

    /** Cột không bao giờ xuất (bí mật hoặc dẫn xuất kỹ thuật), theo bảng. */
    public static final Map<String, List<String>> EXPORT_EXCLUDED_COLUMNS = Map.of(
            "notes", List.of("search_vector"));

    private static Map<String, String> excluded() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("data_exports", "Siêu dữ liệu của chính việc xuất");
        m.put("account_deletion_codes", "Băm mã xác nhận (bí mật)");
        m.put("outbox_events", "Hàng đợi sự kiện kỹ thuật; payload có thể tạm chứa token (D37)");
        m.put("notification_deliveries", "Nhật ký gửi kỹ thuật");
        m.put("device_push_tokens", "Token push (bí mật, D204)");
        m.put("budget_alerts", "Dấu đã gửi cảnh báo, dẫn xuất");
        m.put("sync_diagnostics", "Chẩn đoán kỹ thuật");
        m.put("sync_mutations", "Kết quả mutation kỹ thuật (D204)");
        m.put("change_logs", "Nhật ký đồng bộ kỹ thuật (D204)");
        m.put("user_sync_state", "Con trỏ đồng bộ kỹ thuật");
        m.put("refresh_tokens", "Token phiên (bí mật, D204)");
        m.put("email_verification_tokens", "Token (bí mật)");
        m.put("password_reset_tokens", "Token (bí mật)");
        return java.util.Collections.unmodifiableMap(m);
    }

    public static List<String> exportedTables() {
        return TABLES.stream().filter(t -> !EXPORT_EXCLUDED.containsKey(t)).toList();
    }

    private UserDataCatalog() {
    }
}
