# Theo dõi Tính năng Cục bộ Chưa có Backend (`LOCAL_ONLY`) — LifeOS Mobile

> **Trạng thái:** v1 — Đã duyệt theo `MOBILE_UI_UPGRADE_PLAN.md` (2026-11-10)  
> **Nguyên tắc bất khả xâm phạm:** Dữ liệu thuộc các tính năng này được lưu hoàn toàn trong tệp SQLite phụ riêng biệt `lifeos_local_extras_<userId>.sqlite` qua lớp `LifeOsLocalExtrasDatabase`. Tuyệt đối không có cột `sync_state`, không ghi vào `outbox`, không thêm `SyncEntityType`, không gọi API ngoại lai.

---

## 1. Danh sách Tính năng `LOCAL_ONLY` Triển khai trong Đợt này

### 1.1. Bánh xe Cuộc sống (Life Wheel Assessment)
- **Mục đích:** Giúp người dùng định kỳ tự chấm điểm 8 khía cạnh cốt lõi của cuộc sống (Sự nghiệp, Tài chính, Sức khỏe, Mối quan hệ, Phát triển bản thân, Gia đình, Vui chơi giải trí, Không gian sống) trên thang điểm 1–10; vẽ biểu đồ Radar đa giác trực quan và so sánh sự chuyển biến qua các tháng.
- **Bảng dữ liệu:** `local_life_wheel_assessments`
- **Cấu trúc cột:**
  - `id TEXT PRIMARY KEY` (UUID v4)
  - `recorded_at INTEGER NOT NULL` (epoch ms UTC)
  - `career INTEGER NOT NULL` (1–10)
  - `finance INTEGER NOT NULL` (1–10)
  - `health INTEGER NOT NULL` (1–10)
  - `relationships INTEGER NOT NULL` (1–10)
  - `growth INTEGER NOT NULL` (1–10)
  - `family INTEGER NOT NULL` (1–10)
  - `leisure INTEGER NOT NULL` (1–10)
  - `environment INTEGER NOT NULL` (1–10)
  - `notes TEXT NULL` (Ghi chú phản tư)
- **Ghi chú kỹ thuật nếu đồng bộ sau:** Cần backend bổ sung module mới hoặc mở rộng `Review/Analytics` với bảng `life_wheel_assessments` có khóa `(user_id, recorded_at)`, thêm `SyncEntityType.life_wheel` và hợp nhất 3 chiều theo trường.

---

### 1.2. Khu vườn Tập trung (Focus Garden / Pomodoro Timer)
- **Mục đích:** Đồng hồ bấm giờ tập trung (phiên 25 hoặc 50 phút). Mỗi phiên hoàn thành sẽ gieo một "mầm cây" ảo vào khu vườn của người dùng, liên kết tùy chọn với một Công việc (`task_id` từ bảng `tasks` hiện có) để ghi nhận nỗ lực làm việc sâu (Deep Work).
- **Bảng dữ liệu:** `local_focus_sessions`
- **Cấu trúc cột:**
  - `id TEXT PRIMARY KEY` (UUID v4)
  - `task_id TEXT NULL` (tham chiếu ID của `tasks` nếu có)
  - `started_at INTEGER NOT NULL` (epoch ms UTC)
  - `duration_minutes INTEGER NOT NULL` (ví dụ 25, 50)
  - `completed INTEGER NOT NULL` (1: Hoàn tất, 0: Bị gián đoạn)
  - `tag TEXT NULL` (ví dụ: `Học tập`, `Lập trình`, `Đọc sách`)
- **Ghi chú kỹ thuật nếu đồng bộ sau:** Cần backend bổ sung `SyncEntityType.focus_session` với chính sách thêm mới (`CREATE` append-only) tương tự `routine_log`.

---

## 2. Danh sách Tính năng Mở rộng Chuyển sang Trạng thái HOÃN (`DEFERRED`)

Theo quyết định của người dùng ngày 2026-11-10:
1. **Nhật ký Tâm trạng (`local_mood_logs`):** Đã được tích hợp trọn vẹn vào Review Wizard thông qua thực thể đồng bộ `reviews` (`moodScore` 1–5, `reflection`) của backend Java. Không tạo bảng local trùng lặp.
2. **Bộ Nghi thức Sáng & Tối (Rituals Player):** Hoãn, chờ người dùng duyệt đợt sau.
3. **Màn Onboarding chào mừng lần đầu:** Hoãn, chờ người dùng duyệt đợt sau.
4. **Huy hiệu & Cột mốc cá nhân (Badges):** Hoãn, chờ người dùng duyệt đợt sau.

---

## 3. Chính sách Vòng đời Dữ liệu

1. **Đăng xuất (Logout):** Giữ nguyên tệp `lifeos_local_extras_<userId>.sqlite` trên thiết bị (`[CONFIRMED]`). Khi tài khoản này đăng nhập lại thì nạp lại nguyên vẹn. Tài khoản khác đăng nhập sẽ mở tệp `lifeos_local_extras_<otherUserId>.sqlite` riêng biệt.
2. **Xóa tài khoản (Phương án C `[CONFIRMED]`):** Ứng dụng **không bao giờ tự ý xóa** tệp CSDL local khi gặp lỗi 401/403/404 hay khi hết hạn 7 ngày cooldown. Trong Cài đặt có nút **"Xóa dữ liệu trên máy"** để người dùng chủ động xóa kèm hộp thoại xác nhận bắt buộc.
3. **Xuất dữ liệu `LOCAL_ONLY` (`[OPEN]`):** Tạm thời chưa đưa vào tệp JSON xuất dữ liệu; trong Cài đặt không đưa ra cam kết này cho đến khi người dùng quyết định.

