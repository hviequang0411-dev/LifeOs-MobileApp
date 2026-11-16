# Báo cáo Cuối — Nâng cấp Giao diện LifeOS Mobile Giai đoạn 2 (UPW1–UPW8)

> **Ngày hoàn thành:** 2026-11-10
> **Kế hoạch tham chiếu:** [`docs/engineering/MOBILE_UI_UPGRADE_PLAN.md`](file:///d:/LifeOS/docs/engineering/MOBILE_UI_UPGRADE_PLAN.md) v2 (đã duyệt 2026-11-10)
> **Hướng thiết kế áp dụng:** **Hướng A — Botanical Premium** `[CONFIRMED]`
> **Xóa dữ liệu cục bộ:** **Phương án C** `[CONFIRMED]` (Không bao giờ tự xóa; nút chủ động "Xóa dữ liệu trên máy" trong Cài đặt)
> **Kho tính năng cục bộ:** [`docs/review/MOBILE_LOCAL_ONLY_FEATURES.md`](file:///d:/LifeOS/docs/review/MOBILE_LOCAL_ONLY_FEATURES.md)

---

## 1. Tóm tắt tuân thủ ranh giới bất khả xâm phạm

- Không chỉnh sửa `backend/`, `docs/api/openapi.yaml`, `docs/engineering/SYNC_PROTOCOL.md`.
- Không sửa lược đồ CSDL gốc (29 bảng thực thể đồng bộ, 6 bảng hạ tầng trong `drift_tables.dart` & `app_database.dart`), không đổi `schemaVersion`, không thêm `SyncEntityType`, không sửa `SyncEngine`, `LifeOsApiClient`, `ThreeWayMerger`, `MutationCoalescerDao` (kiểm chứng `LastWriteTime` toàn bộ lõi giữ nguyên mốc `2026-11-08`).
- Tính toán tài chính tuân thủ `Decimal10k` (0 `double` cho tiền tệ).
- Giữ trạng thái HOÃN đối với 4 tính năng ngoài phạm vi (`local_mood_logs`, `local_rituals`, `onboarding`, `local_badges`) và giữ `[OPEN]` đối với xuất dữ liệu `LOCAL_ONLY` (không triển khai).

---

## 2. Danh sách tệp tạo mới / cập nhật trong `mobile/`

| Tệp | Vai trò | Số dòng (< 600) |
|---|---|---|
| [`app_tokens.dart`](file:///d:/LifeOS/mobile/lib/core/theme/app_tokens.dart) | Bảng màu Direction A Botanical Premium v2, domain accents, shadow 3 lớp, motion tokens | 313 |
| [`app_theme.dart`](file:///d:/LifeOS/mobile/lib/core/theme/app_theme.dart) | Khắc phục tương phản `onSecondary`, `secondaryContainer` / `onSecondaryContainer` Light & Dark | 245 |
| [`lifeos_card_and_states.dart`](file:///d:/LifeOS/mobile/lib/core/widgets/lifeos_card_and_states.dart) | `LifeOsCard` (Material + 3-layer shadow), `LifeOsShimmerSkeleton`, `LifeOsEmptyState`, `LifeOsErrorBanner` | 215 |
| [`lifeos_visual_charts.dart`](file:///d:/LifeOS/mobile/lib/core/widgets/lifeos_visual_charts.dart) | `LifeOsDonutChart` & `LifeWheelRadarChart` thuần `CustomPainter`, hỗ trợ `textScaler 2.0` | 351 |
| [`local_extras_database.dart`](file:///d:/LifeOS/mobile/lib/core/database/local_extras_database.dart) | CSDL SQLite cô lập `lifeos_local_extras_<userId>.sqlite` + `clearAll()` | 264 |
| [`local_extras_repository.dart`](file:///d:/LifeOS/mobile/lib/features/workspace/data/local_extras_repository.dart) | Riverpod provider & repository `LOCAL_ONLY` | 62 |
| [`my_day_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/tasks/presentation/my_day_tab_view.dart) | Tab Hôm nay tích hợp Life Rings, shortcut pills, nhiệm vụ trọng tâm | 426 |
| [`my_day_habits_finance_section.dart`](file:///d:/LifeOS/mobile/lib/features/tasks/presentation/widgets/my_day_habits_finance_section.dart) | Tài chính hôm nay (có nút ẩn/hiện `btn_toggle_mask_amount`), `BudgetPaceGauge`, `HabitHeatmapWidget` & nút điểm danh 3 trạng thái | 350 |
| [`life_rings_widget.dart`](file:///d:/LifeOS/mobile/lib/features/tasks/presentation/widgets/life_rings_widget.dart) | Vòng đời hôm nay (`LifeRingsWidget`) | 264 |
| [`habit_heatmap_widget.dart`](file:///d:/LifeOS/mobile/lib/features/habits/presentation/widgets/habit_heatmap_widget.dart) | Bản đồ nhiệt thói quen 8 tuần + chuỗi liên tục | 183 |
| [`finance_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/finance/presentation/finance_tab_view.dart) | Tab Tài chính v2: Tổng tài sản, `BudgetPaceGauge`, `LifeOsDonutChart` chi tiêu thực tế, thẻ ví | 517 |
| [`budget_pace_gauge.dart`](file:///d:/LifeOS/mobile/lib/features/finance/presentation/widgets/budget_pace_gauge.dart) | Thước đo tốc độ tiêu dùng so với ngày trong tháng | 175 |
| [`plan_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/calendar/presentation/plan_tab_view.dart) | Tab Kế hoạch v2: Thanh tuần ngang 7 ngày, `DailyTimelineWidget`, Dự án & Sự kiện lặp | 519 |
| [`daily_timeline_widget.dart`](file:///d:/LifeOS/mobile/lib/features/calendar/presentation/widgets/daily_timeline_widget.dart) | Dòng thời gian kết hợp sự kiện, khối tập trung & công việc | 211 |
| [`knowledge_and_goals_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/knowledge/presentation/knowledge_and_goals_tab_view.dart) | Tab Tri thức & Mục tiêu v2: thẻ liên kết, thanh tiến độ & nhãn nguy cơ | 399 |
| [`overview_and_settings_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/review/presentation/overview_and_settings_tab_view.dart) | Tab Tổng quan v2: Trình hướng dẫn Phản tư 3 bước, Tìm kiếm Offline, Bánh xe cuộc sống, Khu vườn tập trung & Phương án C `"Xóa dữ liệu trên máy"` | 436 |
| [`review_wizard_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/review/presentation/widgets/review_wizard_sheet.dart) | Wizard phản tư 3 bước lưu `life_review` | 368 |
| [`life_wheel_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/workspace/presentation/widgets/life_wheel_sheet.dart) | Bánh xe cuộc sống (`LOCAL_ONLY`) | 296 |
| [`focus_garden_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/workspace/presentation/widgets/focus_garden_sheet.dart) | Khu vườn tập trung Pomodoro (`LOCAL_ONLY`) | 327 |
| [`global_search_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/workspace/presentation/widgets/global_search_sheet.dart) | Tìm kiếm toàn cục offline (tasks, notes, calendar_events, financial_transactions) | 358 |
| [`sign_in_welcome_view.dart`](file:///d:/LifeOS/mobile/lib/features/auth/presentation/sign_in_welcome_view.dart) | Màn hình đăng nhập sau khi xóa dữ liệu trên máy (Phương án C) | 177 |

---

## 3. Bằng chứng kiểm thử & Bộ ảnh Trước / Sau

1. **Phân tích tĩnh:** `dart analyze` → **`No issues found!`** (0 lỗi, 0 cảnh báo).
2. **Toàn bộ unit / contract / WCAG / widget / a11y / performance / golden tests:**
   - Lệnh: `flutter test --no-pub -j 1` (trên `D:\temp` do ổ `C:` của máy trống 0 bytes)
   - Kết quả: **`00:57 +55 ~1: All tests passed!`** (`55` ca đạt, `1` ca `e2e_real_backend_test.dart` bỏ qua mặc định khi không bật cờ `LIFEOS_RUN_REAL_E2E=true`).
3. **Golden Test xác định 2 lượt liên tiếp (`matchesGoldenFile`):**
   - Lần 1 (trong lượt full suite `flutter test --no-pub -j 1`): 11/11 ảnh đạt (`01_my_day`..`05_overview` × Light/Dark + Tablet `1024×768`).
   - Lần 2 (`flutter test --no-pub test/ui_golden_test.dart`): **`All tests passed!`** — 0 pixel sai khác, không sinh thư mục `test/failures/`.
4. **E2E với Backend thật (Docker Compose `http://localhost:8080` + Mailpit `http://localhost:8025`):**
   - Đã cài đặt Flutter SDK (`Flutter 3.47.9`, `Dart 3.13.7`) vào `D:\src\flutter`, cập nhật biến `Path` và chạy `flutter pub get`.
   - Khởi động 3 container `backend-postgres-1`, `backend-mailpit-1`, `backend-backend-1` (`Started LifeOsApplication`, `/actuator/health` trả `HTTP 200`).
   - Sửa ngày kiểm thử trong [`test/e2e_real_backend_test.dart`](file:///d:/LifeOS/mobile/test/e2e_real_backend_test.dart) từ `'2026-11-08'` cố định sang `todayIso` động theo `UserDayClock` (khắc phục lỗi backend `HABIT_DATE_OUT_OF_RANGE` khi ngày chạy thực tế là `2026-11-10`).
   - Lệnh: `D:\src\flutter\bin\flutter.bat test --no-pub --dart-define=LIFEOS_RUN_REAL_E2E=true test/e2e_real_backend_test.dart`
   - Kết quả: **`00:22 +1: All tests passed!`** (`EMAIL_VERIFICATION`, `sync_push` / `sync_pull`, task, habit check-in, transfer/reconcile, calendar split/exception, note/goal/review, conflict resolution, account export ZIP, account deletion code/cancel đều đi qua server thật).
   - Kiểm tra lại `dart analyze` trên Flutter `3.47.9`: **`No issues found!`**.
5. **Bộ ảnh so sánh Trước / Sau:**
   - Trước nâng cấp (`before`): `docs/review/mobile_screenshots/ui_upgrade/before/` (9 ảnh)
   - Sau nâng cấp (`after`): `docs/review/mobile_screenshots/ui_upgrade/after/` (11 ảnh) và đồng bộ về `docs/review/mobile_screenshots/`
   - Tư liệu 3 hướng thiết kế A/B/C: `docs/review/mobile_screenshots/ui_upgrade/preview/` (12 ảnh)
6. **Mục chưa đo trong phiên này (báo cáo trung thực):**
   - **Tốc độ khung hình cuộn (`Profile Mode`):** *Chưa đo trên profile mode* (cần thiết bị/emulator gắn thật để chạy `flutter run --profile`).
