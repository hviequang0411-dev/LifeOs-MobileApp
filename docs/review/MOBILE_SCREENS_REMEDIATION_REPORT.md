# Báo cáo Triển khai Khắc phục Màn hình Chức năng Mobile (Wave 1 – Wave 3)

> **Dự án:** LifeOS Mobile (Flutter)  
> **Thời điểm nghiệm thu:** 2026-11-19  
> **Trạng thái:** Hoàn thành cả 3 đợt triển khai (`Wave 1 P0 → Wave 2 P1 → Wave 3 P2`), sửa triệt để các khiếm khuyết phát hiện khi rà soát màn hình chức năng và nút lưu cố định trên `NoteEditorSheet`. Toàn bộ phân tích tĩnh, 2 lượt kiểm chứng Golden và full test suite đều đạt với `0` lỗi (`1` test E2E backend thật bỏ qua theo mặc định).  
> **Tài liệu tham chiếu:** [`AGENTS.md`](file:///d:/LifeOS/AGENTS.md), [`docs/review/MOBILE_PROGRESS.md`](file:///d:/LifeOS/docs/review/MOBILE_PROGRESS.md), [`docs/review/MOBILE_AI_ASSISTANT_REPORT.md`](file:///d:/LifeOS/docs/review/MOBILE_AI_ASSISTANT_REPORT.md), [`docs/review/PENDING_DECISIONS.md`](file:///d:/LifeOS/docs/review/PENDING_DECISIONS.md).

---

## 1. Tổng hợp Triển khai theo 3 Đợt (`Wave 1 → Wave 2 → Wave 3`)

### 1.1. Wave 1 — Mức độ P0 (Sửa lỗi nền tảng, AI base path, bền vững phiên & Quick Capture)

| Hạng mục | Thay đổi chính đã thực hiện | Tệp mã nguồn |
|---|---|---|
| Sửa trùng prefix đường dẫn AI (`/api/v1/api/v1/ai/*`) | Thay 17 endpoint từ `'/api/v1/ai/...'` thành `'/ai/...'` để ghép chuẩn với `baseUrl` đã chứa `/api/v1`. | [`ai_api.dart`](file:///d:/LifeOS/mobile/lib/features/ai/data/ai_api.dart) |
| Lưu trữ cục bộ bền vững trên thiết bị (`!kIsWeb`) | Bổ sung mở SQLite bền vững qua `baseDirectory` và bảng `local_device_prefs` (`getPref`/`setPref`/`deletePref`, xóa sạch trong `clearAll()`), khôi phục `userId`, `theme_mode` và `notification_prefs` khi khởi động app. | [`local_extras_database.dart`](file:///d:/LifeOS/mobile/lib/core/database/local_extras_database.dart), [`sqlite_runtime.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime.dart), [`sqlite_runtime_io.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime_io.dart), [`sqlite_runtime_web.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime_web.dart), [`sqlite_runtime_stub.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime_stub.dart), [`local_extras_repository.dart`](file:///d:/LifeOS/mobile/lib/features/workspace/data/local_extras_repository.dart), [`main.dart`](file:///d:/LifeOS/mobile/lib/main.dart), [`main_dev.dart`](file:///d:/LifeOS/mobile/lib/main_dev.dart), [`main_staging.dart`](file:///d:/LifeOS/mobile/lib/main_staging.dart), [`main_prod.dart`](file:///d:/LifeOS/mobile/lib/main_prod.dart) |
| Bền vững cài đặt thông báo cục bộ | Đọc/ghi `notification_prefs` vào `local_device_prefs`. | [`local_notification_scheduler.dart`](file:///d:/LifeOS/mobile/lib/features/notification/domain/local_notification_scheduler.dart), [`notification_settings_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/review/presentation/widgets/notification_settings_sheet.dart) |
| Banner hủy xóa tài khoản & đăng nhập lại | Banner hủy xóa gọi `DELETE /account/deletion`; banner hết phiên và `SafeLogoutDialog` đặt `isSignedOutProvider = true`, bổ sung sao chép JSON sao lưu khi outbox còn pending. | [`lifeos_shell_screen.dart`](file:///d:/LifeOS/mobile/lib/features/workspace/presentation/lifeos_shell_screen.dart), [`sync_diagnostics_and_logout_dialogs.dart`](file:///d:/LifeOS/mobile/lib/features/sync/presentation/sync_diagnostics_and_logout_dialogs.dart) |
| Hoàn thiện Quick Capture (Công việc & Thu/Chi) | Bổ sung `quickCaptureDraftProvider`, chọn mức ưu tiên & validate ở tab Việc, tạo ví tiền thật UUID (`financial_account` / ví tiền mặt nhanh), loại giao dịch, danh mục & ghi chú (loại bỏ `'default-wallet'`). | [`quick_capture_bottom_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/workspace/presentation/quick_capture_bottom_sheet.dart) |

### 1.2. Wave 2 — Mức độ P1 (Form CRUD đầy đủ, F1/F6 Finance, Calendar & AI Weekly Plan)

| Hạng mục | Thay đổi chính đã thực hiện | Tệp mã nguồn |
|---|---|---|
| Bổ sung CRUD Offline & quy tắc nghiệp vụ F1/F6 | Thêm CRUD Habit/Project/Routine/Finance/Calendar; F1 chặn chuyển tiền khác currency (`FINANCE_CURRENCY_MISMATCH`); F6 lưu trữ ví tự động chuyển giao dịch định kỳ liên quan sang `PAUSED` và giữ nguyên `nextRunOn`. | [`task_local_repository.dart`](file:///d:/LifeOS/mobile/lib/features/tasks/data/task_local_repository.dart), [`finance_local_repository.dart`](file:///d:/LifeOS/mobile/lib/features/finance/data/finance_local_repository.dart), [`calendar_local_repository.dart`](file:///d:/LifeOS/mobile/lib/features/calendar/data/calendar_local_repository.dart), [`lifeos_local_repository.dart`](file:///d:/LifeOS/mobile/lib/features/workspace/data/lifeos_local_repository.dart), [`vi_strings.dart`](file:///d:/LifeOS/mobile/lib/core/l10n/vi_strings.dart) |
| Editor Công việc, Thói quen & My Day | Tạo `TaskEditorSheet` (gắn AI Breakdown theo consent `taskCalendar`, hỗ trợ xóa mềm) và `HabitEditorSheet` (`SPECIFIC_DAYS` kèm bitmask thứ); nối `referenceDateIso`, tổng chi tiêu tháng và ngân sách thực lên My Day. | [`task_editor_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/tasks/presentation/widgets/task_editor_sheet.dart), [`habit_editor_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/habits/presentation/widgets/habit_editor_sheet.dart), [`habit_heatmap_widget.dart`](file:///d:/LifeOS/mobile/lib/features/habits/presentation/widgets/habit_heatmap_widget.dart), [`my_day_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/tasks/presentation/my_day_tab_view.dart), [`my_day_habits_finance_section.dart`](file:///d:/LifeOS/mobile/lib/features/tasks/presentation/widgets/my_day_habits_finance_section.dart) |
| Tài chính đầy đủ & Insight | Bổ sung các sheet Quản lý Ví/Giao dịch/Ngân sách/Lịch sử, lọc chi tiêu tháng, `DatePicker` & xóa trên giao dịch định kỳ / hũ tiết kiệm, hiển thị trạng thái `Tạm dừng` rõ ràng, nối loading/lỗi AI Finance Insight. | [`finance_forms_sheets.dart`](file:///d:/LifeOS/mobile/lib/features/finance/presentation/widgets/finance_forms_sheets.dart), [`finance_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/finance/presentation/finance_tab_view.dart), [`recurring_transaction_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/finance/presentation/widgets/recurring_transaction_sheet.dart), [`saving_goal_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/finance/presentation/widgets/saving_goal_sheet.dart), [`recurring_and_saving_section.dart`](file:///d:/LifeOS/mobile/lib/features/finance/presentation/widgets/recurring_and_saving_section.dart) |
| Kế hoạch, Lịch & Thói quen chuỗi (Routine) | Bổ sung sheet Dự án, Sự kiện (`THIS_OCCURRENCE` / `FROM_THIS` / `SERIES`), TimeBlock; sửa lỗi rò rỉ `_selectedDayOffset` khi đổi ngày và mở rộng chuỗi lặp; phân tích thời lượng `(Np)` & sửa/xóa Routine. | [`plan_forms_sheets.dart`](file:///d:/LifeOS/mobile/lib/features/calendar/presentation/widgets/plan_forms_sheets.dart), [`daily_timeline_widget.dart`](file:///d:/LifeOS/mobile/lib/features/calendar/presentation/widgets/daily_timeline_widget.dart), [`plan_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/calendar/presentation/plan_tab_view.dart), [`routines_section_card.dart`](file:///d:/LifeOS/mobile/lib/features/tasks/presentation/widgets/routines_section_card.dart) |

### 1.3. Wave 3 — Mức độ P2 (Tri thức/Tag, Validate ngày Goal, Search/Review đa kỳ, Cài đặt tài khoản & Khử lộ mã kỹ thuật)

| Hạng mục | Thay đổi chính đã thực hiện | Tệp mã nguồn |
|---|---|---|
| Tag cho Ghi chú, lọc Lưu trữ & Sticky Save | Bổ sung CRUD Tag & liên kết `note_tag`; lọc ghi chú lưu trữ và ưu tiên ghim trước trên Tab Tri thức; thêm chọn/tạo Nhãn, trạng thái loading Tóm tắt AI trên `NoteEditorSheet` và đặt `btn_save_note` ở footer cố định (`Flexible(child: SingleChildScrollView(...))`) để không bị đẩy khuất trên màn hình `600px` (`notes_and_goals_forms_test.dart`). | [`knowledge_local_repository.dart`](file:///d:/LifeOS/mobile/lib/features/knowledge/data/knowledge_local_repository.dart), [`knowledge_and_goals_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/knowledge/presentation/knowledge_and_goals_tab_view.dart), [`note_editor_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/knowledge/presentation/widgets/note_editor_sheet.dart) |
| Validate ngày Mục tiêu & Cột mốc | Kiểm tra định dạng `YYYY-MM-DD` hợp lệ, `DatePicker` và ràng buộc `target >= start` trên tạo/sửa Mục tiêu và Cột mốc. | [`goal_milestones_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/knowledge/presentation/widgets/goal_milestones_sheet.dart) |
| Tìm kiếm toàn cục & Review đa kỳ | Bọc `Material`, mở rộng tìm kiếm sang Mục tiêu & Thói quen, chạm kết quả mở thẳng form sửa; `ReviewWizardSheet` hỗ trợ `DAY`/`WEEK`/`MONTH` theo `UserDayClock`, đếm cả `DONE` & `COMPLETED` và xem `ReviewHistorySheet`. | [`global_search_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/workspace/presentation/widgets/global_search_sheet.dart), [`review_wizard_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/review/presentation/widgets/review_wizard_sheet.dart) |
| Cài đặt cá nhân, Thiết bị, Quyền riêng tư & Thông báo | Tạo các sheet `PersonalSettingsSheet`, `DevicesSheet`, `AccountPrivacySheet`, `NotificationsInboxSheet` (lọc chưa đọc, đánh dấu đã đọc, định dạng mẫu `finance.recurring.paused`); nối vào Tab Tổng quan và tách riêng `tile_ai_settings` với `tile_ai_usage_limits`, bỏ chuỗi `(DELETE /ai/data)`. | [`account_and_settings_sheets.dart`](file:///d:/LifeOS/mobile/lib/features/review/presentation/widgets/account_and_settings_sheets.dart), [`overview_and_settings_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/review/presentation/overview_and_settings_tab_view.dart) |
| Làm sạch UI AI & Phân trang hội thoại | Loại bỏ chuỗi kỹ thuật `(AWP-3)`, `AI Breakdown`, `Outbox`, raw error string; dịch tên miền trong Context Disclosure sang tiếng Việt; sửa lỗi null trên `createConversation()`, thêm chip `Lập kế hoạch tuần` và phân trang con trỏ danh sách hội thoại / tin nhắn cũ hơn. | [`ai_context_disclosure_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/widgets/ai_context_disclosure_sheet.dart), [`ai_conversation_list_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/widgets/ai_conversation_list_sheet.dart), [`ai_assistant_screen.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/ai_assistant_screen.dart), [`ai_plans_and_insights_sheets.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/widgets/ai_plans_and_insights_sheets.dart), [`ai_usage_and_limits_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/widgets/ai_usage_and_limits_sheet.dart) |

---

## 2. Đề xuất Quyết định Kỹ thuật `[PROPOSED] D-M10`

> **`[PROPOSED]` `D-M10` (Chờ người dùng phê duyệt):**  
> Lưu trữ tùy chọn cục bộ cấp thiết bị (`userId` phiên gần nhất, `theme_mode`, `notification_prefs`) vào bảng riêng `local_device_prefs` bên trong kho CSDL phụ [`LifeOsLocalExtrasDatabase`](file:///d:/LifeOS/mobile/lib/core/database/local_extras_database.dart) (mở tệp SQLite bền vững trên nền tảng `!kIsWeb` qua `baseDirectory`).  
> - **Ranh giới dữ liệu:** Tuyệt đối **không** ghi vào 29 bảng thực thể đồng bộ hay 6 bảng hạ tầng trong `drift_tables.dart`, **không** tăng `schemaVersion` của `LifeOsDriftDatabase`, và **không** phát sinh mutation qua `outbox`.  
> - **Vòng đời xóa dữ liệu:** Được xóa sạch đồng thời trong `LifeOsLocalExtrasDatabase.clearAll()` khi người dùng thực hiện đăng xuất an toàn / "Xóa dữ liệu trên máy" (Phương án C).

---

## 3. Kiểm toán Ranh giới & Vùng cấm (`git status -s`)

Kiểm tra trực tiếp bằng lệnh `git -C d:\LifeOS status -s`:
- **0 tệp vùng cấm bị sửa đổi bởi Wave 1–3:** Không chạm vào [`AGENTS.md`](file:///d:/LifeOS/AGENTS.md), `backend/src/**`, `backend/pom.xml`, [`docs/api/openapi.yaml`](file:///d:/LifeOS/docs/api/openapi.yaml), `docs/engineering/**`, `docs/architecture/**`, `docs/product/**`, [`mobile/pubspec.yaml`](file:///d:/LifeOS/mobile/pubspec.yaml), [`drift_tables.dart`](file:///d:/LifeOS/mobile/lib/core/database/drift_tables.dart), [`drift_connection.dart`](file:///d:/LifeOS/mobile/lib/core/database/drift_connection.dart), hay [`MOBILE_LOCAL_ONLY_FEATURES.md`](file:///d:/LifeOS/docs/review/MOBILE_LOCAL_ONLY_FEATURES.md).
- **Không cài đặt tính năng export `local_extras`:** Tuân thủ trạng thái `[OPEN]` trong [`PENDING_DECISIONS.md`](file:///d:/LifeOS/docs/review/PENDING_DECISIONS.md) §3.
- **Ghi nhận minh bạch working tree ngoài phạm vi Wave 1–3:** Các tệp đã có sẵn trong working tree từ các tác vụ hạ tầng/android trước đợt này (`backend/.gitignore` với 3 dòng comment `.env.tunnel`, `backend/docker-compose.tunnel.yml`, `backend/scripts/`, `docs/ops/SELF_HOST_TUNNEL.md`, `docs/review/PENDING_DECISIONS.md`, `mobile/android/app/build.gradle.kts`, `mobile/android/app/src/main/AndroidManifest.xml`, `mobile/scripts/`) không bị chỉnh sửa trong đợt khắc phục Wave 1–3.

---

## 4. Danh mục Tệp tin Tạo mới, Cập nhật & Ảnh Golden

### 4.1. Tệp tạo mới trong Wave 1–3 (6 tệp + báo cáo này)
1. `mobile/lib/features/tasks/presentation/widgets/task_editor_sheet.dart`
2. `mobile/lib/features/habits/presentation/widgets/habit_editor_sheet.dart`
3. `mobile/lib/features/finance/presentation/widgets/finance_forms_sheets.dart`
4. `mobile/lib/features/calendar/presentation/widgets/plan_forms_sheets.dart`
5. `mobile/lib/features/review/presentation/widgets/account_and_settings_sheets.dart`
6. `mobile/test/screens_remediation_test.dart` (21 ca kiểm thử bảo phủ Wave 1–3)

### 4.2. Tệp cập nhật (`43` tệp trong `mobile/lib/`)
- `core/database/`: `local_extras_database.dart`, `sqlite_runtime.dart`, `sqlite_runtime_io.dart`, `sqlite_runtime_stub.dart`, `sqlite_runtime_web.dart`
- `core/l10n/`: `vi_strings.dart`
- `features/ai/`: `data/ai_api.dart`, `presentation/ai_assistant_screen.dart`, `presentation/widgets/ai_context_disclosure_sheet.dart`, `presentation/widgets/ai_conversation_list_sheet.dart`, `presentation/widgets/ai_plans_and_insights_sheets.dart`, `presentation/widgets/ai_usage_and_limits_sheet.dart`
- `features/calendar/`: `data/calendar_local_repository.dart`, `presentation/plan_tab_view.dart`, `presentation/widgets/daily_timeline_widget.dart`
- `features/finance/`: `data/finance_local_repository.dart`, `presentation/finance_tab_view.dart`, `presentation/widgets/recurring_and_saving_section.dart`, `presentation/widgets/recurring_transaction_sheet.dart`, `presentation/widgets/saving_goal_sheet.dart`
- `features/habits/`: `presentation/widgets/habit_heatmap_widget.dart`
- `features/knowledge/`: `data/knowledge_local_repository.dart`, `presentation/knowledge_and_goals_tab_view.dart`, `presentation/widgets/goal_milestones_sheet.dart`, `presentation/widgets/note_editor_sheet.dart`
- `features/notification/`: `domain/local_notification_scheduler.dart`
- `features/review/`: `presentation/overview_and_settings_tab_view.dart`, `presentation/widgets/notification_settings_sheet.dart`, `presentation/widgets/review_wizard_sheet.dart`
- `features/sync/`: `presentation/sync_diagnostics_and_logout_dialogs.dart`
- `features/tasks/`: `data/task_local_repository.dart`, `presentation/my_day_tab_view.dart`, `presentation/widgets/my_day_habits_finance_section.dart`, `presentation/widgets/routines_section_card.dart`
- `features/workspace/`: `data/lifeos_local_repository.dart`, `data/local_extras_repository.dart`, `presentation/lifeos_shell_screen.dart`, `presentation/quick_capture_bottom_sheet.dart`, `presentation/widgets/global_search_sheet.dart`
- `main*.dart`: `main.dart`, `main_dev.dart`, `main_prod.dart`, `main_staging.dart`

### 4.3. Kiểm soát Ảnh Golden (`mobile/test/goldens/` & `docs/review/mobile_screenshots/ai_chat/`)
- **Giữ nguyên `0px` sai khác (10 ảnh):** Toàn bộ **6 ảnh AI Golden** trong `docs/review/mobile_screenshots/ai_chat/` (`ai_widget_and_golden_test.dart`) và **4 ảnh** `Golden Daily Use Login (light/dark)` + `Golden Daily Use Routine Player (light/dark)` trong `mobile/test/goldens/` không thay đổi.
- **Cập nhật có chủ đích (`17` ảnh golden chính thức trong `mobile/test/goldens/`):**
  - `phone_light_01_my_day.png` .. `phone_light_05_overview.png` (5 ảnh)
  - `phone_dark_01_my_day.png` .. `phone_dark_05_overview.png` (5 ảnh)
  - `tablet_light_01_my_day.png` (1 ảnh)
  - `daily_use_recurring_tx_light.png`, `daily_use_recurring_tx_dark.png` (2 ảnh)
  - `daily_use_note_editor_light.png`, `daily_use_note_editor_dark.png` (2 ảnh)
  - `daily_use_goal_editor_light.png`, `daily_use_goal_editor_dark.png` (2 ảnh)
- **Tái sinh tự động khi chạy full suite (`12` ảnh preview trong `docs/review/mobile_screenshots/ui_upgrade/preview/`):** Bài [`render_preview_goldens_test.dart`](file:///d:/LifeOS/mobile/test/render_preview_goldens_test.dart) tự ghi lại 12 ảnh `preview_dir_{a,b,c}_{light,dark}_{my_day,finance}.png` khi chạy `flutter test --no-pub -j 1`.
- Cả 17 ảnh cập nhật cộng 10 ảnh giữ nguyên + 4 widget test AI (**31/31 ca**) đã pass **2 lượt chạy liên tiếp** không truyền `--update-goldens` sau chỉnh sửa mã nguồn cuối cùng.

---

## 5. Bằng chứng Kiểm thử, Golden (2 Lượt) & Hiệu năng (`D-M08`)

### 5.1. Phân tích tĩnh (`D:\src\flutter\bin\dart.bat analyze mobile`)
```text
Analyzing mobile...
No issues found!
```
*(Exit code 0 — chạy sau chỉnh sửa cuối cùng trên `note_editor_sheet.dart`).*

### 5.2. Kiểm thử Wave 1–3 & Hồi quy Note/Goal (`screens_remediation_test.dart`, `notes_and_goals_forms_test.dart`, `daily_use_golden_test.dart`)
```text
00:36 +35: All tests passed!
```
*(Gồm 4 ca `notes_and_goals_forms_test.dart`, 21 ca `screens_remediation_test.dart`, 10 ca `daily_use_golden_test.dart`).*

### 5.3. Kiểm chứng Golden 2 Lượt Liên tiếp (không `--update-goldens`, sau lần sửa cuối)
Lệnh chạy cả 2 lượt: `D:\src\flutter\bin\flutter.bat test --no-pub -j 1 test/ui_golden_test.dart test/daily_use_golden_test.dart test/ai_widget_and_golden_test.dart`
- **Lượt 1:**
```text
00:33 +31: All tests passed!
```
- **Lượt 2:**
```text
00:36 +31: All tests passed!
```

### 5.4. Benchmark 50.000 Bản ghi Chạy lẻ (`D-M08`)
Lệnh: `D:\src\flutter\bin\flutter.bat test --no-pub test/domain_modules_and_perf_test.dart` *(chạy sau toàn bộ các thay đổi tầng DB `local_extras_database.dart` / repository)*:
```text
00:11 +7: All tests passed!
```
- **Thời gian đo thực tế:** **11 giây** (`+7: All tests passed!`), nằm trong mốc baseline **10–14 giây** và dưới ngưỡng assertion **60.000 ms** của quyết định `D-M08 [CONFIRMED]`.

### 5.5. Toàn bộ Bộ Kiểm thử Mobile (`D:\src\flutter\bin\flutter.bat test --no-pub -j 1`)
```text
02:32 +125 ~1: All tests passed!
```
- **Chi tiết:** **125 passed, 1 skipped, 0 failed** (exit code 0).
- **Ca bị bỏ qua duy nhất (`~1`):** `test/e2e_real_backend_test.dart` (`Skip: Bỏ qua mặc định: cần Docker chạy backend (http://localhost:8080) + Mailpit (http://localhost:8025) và truyền --dart-define=LIFEOS_RUN_REAL_E2E=true`).

---

## 6. Giả định, Điểm cần Người dùng Xem xét kỹ & Open Questions (`AGENTS.md` §1.7)

### Giả định đã đưa ra
1. **Consent AI trong phiên:** `AiPreferencesNotifier` khởi tạo mặc định tất cả 5 miền ở trạng thái `false` (`AiPreferencesResponse.initialDefault()`) theo `D264`, và chỉ phản ánh trạng thái consent thực tế sau khi `loadPreferences()` được gọi trong phiên. Vì vậy các nút AI theo ngữ cảnh (`TaskEditorSheet`, `HabitEditorSheet`, `NoteEditorSheet`, `FinanceTabView`) tiếp tục ẩn khi chưa bật consent hoặc chưa đồng bộ tùy chọn AI từ server trong phiên hiện tại.
2. **Nút lưu cố định trên `NoteEditorSheet`:** Giữ vùng nhập liệu trong `Flexible(child: SingleChildScrollView(...))` và neo `btn_save_note` bên dưới (`const SizedBox(height: 16)` + `FilledButton`), giả định `NoteEditorSheet` luôn được đặt trong vùng chứa có `maxHeight` hữu hạn (`Scaffold`, `showModalBottomSheet`). Trên khung hình golden `390×844` chiều cao tổng giữ `0px` sai khác, còn trên viewport ngắn `800×600` nút `btn_save_note` luôn nằm trong vùng bấm hợp lệ.

### Điểm cần Người dùng Xem xét kỹ
1. **Duyệt `[PROPOSED] D-M10`:** Quyết định thêm bảng `local_device_prefs` vào `LifeOsLocalExtrasDatabase` và khởi tạo SQLite bền vững trên `!kIsWeb` (cấu hình trong `main*.dart`).
2. **Rà soát trực quan 17 ảnh Golden cập nhật** trong [`mobile/test/goldens/`](file:///d:/LifeOS/mobile/test/goldens/) sau khi nối số liệu thực trên Tab Tổng quan/Tài chính/Tri thức và cập nhật form `NoteEditorSheet`/`GoalEditorSheet`/`RecurringTransactionSheet`.
3. **E2E Backend thật (`e2e_real_backend_test.dart`):** Chưa chạy do yêu cầu Docker backend + Mailpit và `--dart-define=LIFEOS_RUN_REAL_E2E=true`.

### Open Questions Mới
- Không phát sinh Open Question mới ngoài đề xuất `[PROPOSED] D-M10` và các mục đã mở sẵn trong [`docs/review/PENDING_DECISIONS.md`](file:///d:/LifeOS/docs/review/PENDING_DECISIONS.md).
