# LifeOS — Database Rules

> **Trạng thái:** v1 — tách từ `CODE_GUIDELINES.md`. **Giữ nguyên số mục** của Engineering Guidelines v1 để các tham chiếu `§x` vẫn đúng.
> **Phạm vi:** Persistence, timezone, cột chuẩn cho entity đồng bộ (§8).
> Quy ước nhãn `[CONFIRMED]` / `[CONFIRMED]` / `[OPEN]` và quy trình review: xem `CODE_GUIDELINES.md` §0 và §16. Agent chỉ áp dụng `[CONFIRMED]`.

---

## 8. Persistence và Database

1. `[CONFIRMED]` UUID cho entity ID (hỗ trợ tạo offline). `TIMESTAMPTZ` cho thời điểm; `DATE` cho ngày nghiệp vụ (vd habit check-in); `NUMERIC` cho tiền.
2. `[CONFIRMED]` Foreign key & unique constraint rõ ràng; index theo query pattern thực tế; cache không thay thế nguồn gốc.
3. `[CONFIRMED]` Soft delete/tombstone cho dữ liệu đồng bộ; `version` cho optimistic locking.
4. `[CONFIRMED]` Migration bằng **Flyway** (D7); migration đã áp dụng là bất biến, sửa bằng migration mới. `ddl-auto` = `validate`.
5. `[CONFIRMED]` Ràng buộc quan trọng đặt ở DB (NOT NULL, CHECK, UNIQUE), không chỉ ở code.
6. `[CONFIRMED]` Tránh N+1; không dùng `FetchType.EAGER` mặc định; phân trang cho mọi danh sách; không trả danh sách không giới hạn.
7. `[CONFIRMED]` Quy ước đặt tên: bảng `snake_case` số nhiều (khớp baseline: `tasks`, `financial_transactions`), cột `snake_case`.
8. `[CONFIRMED]` Finance: transfer không phải income/expense; giao dịch đã xóa không vào báo cáo; có cơ chế đối soát số dư.
9. `[CONFIRMED]` **Quy ước tiền** (PHASE_3 D100–D103, BACKEND_MASTER_PLAN §5 #8): cột tiền là `NUMERIC(19,4)` kèm `CHECK` giới hạn (số tiền giao dịch/ngân sách/mục tiêu `> 0 AND < 10^13`; số dư đầu kỳ `|x| < 10^13`), luôn đi kèm cột `currency char(3)` (ISO 4217). Java dùng `BigDecimal` dựng từ chuỗi; cấm `float/double` ở mọi nơi trong module `finance` (ArchUnit A14). Không làm tròn ngầm: số chữ số thập phân vượt mức của tiền tệ bị từ chối. Số dư **không lưu**: luôn tính từ lịch sử bằng truy vấn có chỉ mục một phần `WHERE deleted_at IS NULL`. So sánh hai số tiền theo giá trị (`compareTo`), không theo scale.
10. `[CONFIRMED]` **Extension `unaccent` và tìm kiếm toàn văn** (PHASE_6 D182, BACKEND_MASTER_PLAN §5 #7, triển khai V10): extension `unaccent` được phép; dùng qua hàm bọc `lifeos_unaccent(text)` (IMMUTABLE, từ điển cố định) để đưa vào cột `GENERATED ALWAYS AS … STORED` và chỉ mục. Cấu hình FTS là `simple` (không stemming). Truy vấn người dùng chỉ đi qua `websearch_to_tsquery('simple', lifeos_unaccent(:q))` tham số hóa — cấm nối chuỗi vào `tsquery`; biểu thức viết thẳng trong câu lệnh (không qua CTE) để planner dùng chỉ mục GIN một phần `WHERE deleted_at IS NULL`.

### 8.1. Timezone & User Settings

- `[CONFIRMED]` `user_settings` (module `identity`) gồm `timezone` (IANA, vd `Asia/Ho_Chi_Minh`), `locale`, `default_currency` (mặc định `VND`), `week_start_day`.
- `[CONFIRMED]` Timezone theo **user**, không theo thiết bị: app tự nhận từ thiết bị ở lần đăng nhập đầu, người dùng đổi tay được. `user_settings` là entity đồng bộ như các entity khác.
- `[CONFIRMED]` Mọi thời điểm cụ thể lưu `TIMESTAMPTZ` (UTC). Ngày nghiệp vụ (habit check-in, "hôm nay", tuần, tháng của dashboard/review/budget) tính theo `timezone` của user.
- `[CONFIRMED]` Habit check-in gửi lên kèm `date` (ngày nghiệp vụ theo giờ user lúc check-in, vì có thể tạo offline). Server từ chối ngày ngoài khoảng hợp lý quanh "hôm nay" theo timezone user (đề xuất ±1 ngày) trừ khi cho phép bổ sung check-in quá khứ có chủ đích.
- `[CONFIRMED]` Đổi timezone **không** viết lại các ngày nghiệp vụ đã lưu; chỉ áp dụng cho dữ liệu và cách tính từ đó về sau.
- `[CONFIRMED]` Quiet hours và lịch reminder được đánh giá theo timezone user. Reminder lặp lưu giờ địa phương (wall-clock) kèm timezone thay vì chỉ một instant `[OPEN]` xác nhận.
- `[CONFIRMED]` Giao diện chỉ tiếng Việt, nhưng mọi chuỗi UI nằm trong file localization (không hard-code) để thêm ngôn ngữ sau.

### 8.2. Cột chuẩn cho entity đồng bộ (schema từ Phase 1) `[CONFIRMED]`

Mọi bảng chứa dữ liệu cá nhân cần đồng bộ (tasks, projects, categories, habits, habit_checkins, routines… và các bảng sau này) có:

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | `UUID` PK | Client tạo được khi offline |
| `user_id` | `UUID` NOT NULL | FK `users`, mọi truy vấn scope theo cột này |
| `version` | `BIGINT` NOT NULL, bắt đầu 1 | Tăng 1 mỗi lần thay đổi, do server quản lý |
| `deleted_at` | `TIMESTAMPTZ` NULL | Tombstone; NULL = đang tồn tại |
| `created_at`, `updated_at` | `TIMESTAMPTZ` NOT NULL | Thông tin hiển thị/diagnostic; **không** dùng để quyết định bản nào mới hơn |

Quy tắc đi kèm:
- Bảng hạ tầng sync: `sync_mutations`, `change_logs`, `user_sync_state` (xem §9.3) tồn tại từ Phase 1.
- `[CONFIRMED]` Ràng buộc UNIQUE trên bảng có soft delete dùng partial unique index `WHERE deleted_at IS NULL` để cho phép tạo lại bản ghi trùng tên sau khi xóa.
- `[CONFIRMED]` Truy vấn thông thường luôn lọc `deleted_at IS NULL`; việc đọc tombstone chỉ dành cho pull sync.
- `[CONFIRMED]` Bảng chỉ-đọc/dẫn xuất (báo cáo, cache, `notification_deliveries`…) không cần các cột này.
