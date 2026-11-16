# Báo cáo Nghiệm thu Kỹ thuật: Trợ lý AI trên Mobile (AWP-1 đến AWP-6)

> **Dự án:** LifeOS Mobile (Flutter)  
> **Thời điểm nghiệm thu:** 2026-11-15  
> **Trạng thái:** Hoàn thành 100% các hạng mục AWP-1..AWP-6 theo yêu cầu. Toàn bộ test suite và phân tích tĩnh đạt chuẩn.  
> **Tài liệu tham chiếu:** [`docs/phases/PHASE_8_AI_ASSISTANT.md`](file:///d:/LifeOS/docs/phases/PHASE_8_AI_ASSISTANT.md), [`docs/api/openapi.yaml`](file:///d:/LifeOS/docs/api/openapi.yaml), [`docs/api/fixtures/ai-*.json`](file:///d:/LifeOS/docs/api/fixtures/), [`docs/engineering/DEPLOY_AI_MASTER_PLAN.md`](file:///d:/LifeOS/docs/engineering/DEPLOY_AI_MASTER_PLAN.md).

---

## 1. Tổng quan Kiến trúc & Nguyên tắc Vùng cấm

Trợ lý AI là một **tính năng trực tuyến (online-only feature)** độc lập với hệ thống đồng bộ dữ liệu ngoại tuyến (Offline Sync). Triển khai trên Mobile tuân thủ nghiêm ngặt các ranh giới kiến trúc:

1. **Không can thiệp Outbox:** Mọi lệnh gọi API của AI đi thẳng qua `AiApiClient` (gắn kèm `LifeOsApiClient.normalizeError`), tuyệt đối không qua hàng đợi `outbox`, không xếp hàng ngoại tuyến khi mất mạng.
2. **Không bổ sung bảng vào CSDL SQLite gốc:** Giữ nguyên 29 bảng Drift thực thể đồng bộ và 6 bảng hạ tầng; `schemaVersion` giữ nguyên không đổi. Dữ liệu hội thoại và ngữ cảnh AI lưu trữ tập trung trên server.
3. **Bộ nhớ phiên (Session-only Cache):** Danh sách hội thoại và tin nhắn chat chỉ lưu trong bộ nhớ RAM của phiên làm việc hiện tại (`StateNotifier` / Riverpod).
4. **Cách ly 4 path `action-proposal`:** Đúng theo hướng dẫn (AIP6 hoãn), không xây dựng UI cho các endpoint `/ai/action-proposals*`. Các nút "Tạo việc từ gợi ý" trong Kế hoạch và Chia nhỏ việc được thực hiện trực tiếp bởi người dùng qua `localRepository.createTask` (luồng task ngoại tuyến thông thường với Outbox).
5. **Không dùng `double` cho chi phí AI:** Mọi trường chi phí tính bằng `micro-USD` được định dạng nguyên vẹn bằng phép chia nguyên `formatMicroUsd` không dùng số thực `double`.

---

## 2. Bảng Nghiệm thu Chi tiết AWP-1 đến AWP-6 (3 Nhãn Quy ước)

| Gói | Hạng mục | Trạng thái | Nhãn quy ước | Bằng chứng & Chi tiết triển khai |
|---|---|---|---|---|
| **AWP-1** | **Consent theo miền (Cửa vào bắt buộc)** | `XONG` | `[CONFIRMED]` | - Biểu mẫu [`ai_consent_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/widgets/ai_consent_sheet.dart) hiển thị 5 công tắc theo hợp đồng `GET/PUT /api/v1/ai/preferences`: *Công việc & Lịch*, *Thói quen*, *Mục tiêu & Cột mốc*, *Ghi chú*, *Tài chính cá nhân*.<br>- **Tất cả 5 miền mặc định TẮT** (`initialDefault`).<br>- **Miền Tài chính là công tắc độc lập** có khung viền cảnh báo nổi bật, giải thích rõ: chỉ đọc số liệu tổng hợp, tuyệt đối không có giao dịch tự động nào.<br>- Có checkbox xác nhận chính sách xử lý dữ liệu AI (phiên bản 2026-11-10; không chia sẻ huấn luyện mô hình).<br>- Dòng banner cố định [`ai_disclaimer_banner.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/widgets/ai_disclaimer_banner.dart): *"Nội dung do AI tạo, có thể sai — luôn kiểm tra trước khi làm theo"*. |
| **AWP-2** | **Chat và Hội thoại** | `XONG` | `[CONFIRMED]`<br>(D-M09 đã duyệt) | - Màn hình chat [`ai_assistant_screen.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/ai_assistant_screen.dart): Quản lý hội thoại (`GET /ai/conversations`, tạo mới, `DELETE /ai/conversations/{id}` kèm dialog xác nhận).<br>- **Non-streaming (O-AI-08):** Đếm giờ chờ tối đa 30s với thanh tiến trình và skeleton typing; quá 30s hiển thị bong bóng lỗi kèm nút "Thử lại".<br>- **Cổng kiểm soát gửi chồng (Concurrency gate):** Khóa ô nhập và nút gửi khi đang có request chờ phản hồi.<br>- **Ánh xạ 15 mã lỗi OpenAPI sang tiếng Việt:** Đầy đủ trong [`vi_strings.dart`](file:///d:/LifeOS/mobile/lib/core/l10n/vi_strings.dart) (`mapAiError`, `AiErrorDescriptor`) kèm hành động gợi ý (*Mở cài đặt quyền riêng tư*, *Xem hạn mức sử dụng*, *Thử lại*), không lộ mã kỹ thuật thô.<br>- **Chế độ ngoại tuyến:** Giao diện xám đi khi mất mạng với thông báo cần kết nối internet.<br>- **Lối vào `D-M09 [CONFIRMED]`:** Bố trí tại Thẻ `tile_ai_assistant` trên Tab Tổng quan và các nút theo ngữ cảnh, không chèn AppBar toàn cục để bảo toàn 100% pixel các màn hình nền tảng. |
| **AWP-3** | **Minh bạch ngữ cảnh (Context Disclosure)** | `XONG` | `[CONFIRMED]` | - Nút *"AI đã đọc gì?"* trên mỗi tin nhắn của Assistant mở bottom sheet [`ai_context_disclosure_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/widgets/ai_context_disclosure_sheet.dart).<br>- Gọi `GET /api/v1/ai/messages/{id}/context`: hiển thị trung thực các miền đã đọc, danh sách bản ghi và các trường sau redaction; không suy diễn thêm dữ liệu. |
| **AWP-4** | **Planning & Insight (Read-only)** | `XONG` | `[CONFIRMED]` | - Triển khai các bottom sheet trong [`ai_plans_and_insights_sheets.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/widgets/ai_plans_and_insights_sheets.dart): Lập kế hoạch ngày/tuần (`POST /ai/plans/daily|weekly`), Chia nhỏ việc (`POST /ai/tasks/{id}/breakdown`), Phân tích Thói quen & Tài chính (`POST /ai/insights/...`), Tóm tắt Ghi chú (`POST /ai/notes/{id}/summary`).<br>- Mọi kết quả chỉ hiển thị gợi ý (read-only).<br>- **Tạo việc từ gợi ý:** Nút bấm tạo việc gọi thẳng `localRepository.createTask` (luồng tạo task ngoại tuyến thông thường với Outbox), không gọi các endpoint action-proposal.<br>- **Consent gating:** Nút phân tích tài chính trên Tab Tài chính và nút Tóm tắt AI trên NoteEditorSheet chỉ hiển thị khi người dùng đã bật consent cho miền tương ứng. |
| **AWP-5** | **Hạn mức và Dữ liệu AI** | `XONG` | `[CONFIRMED]` | - Bottom sheet [`ai_usage_and_limits_sheet.dart`](file:///d:/LifeOS/mobile/lib/features/ai/presentation/widgets/ai_usage_and_limits_sheet.dart): Tích hợp `GET /ai/usage` và `GET /ai/limits`.<br>- Hiển thị hạn mức yêu cầu, hạn mức token (cửa sổ phút/ngày), thanh tiến độ và cảnh báo nổi bật khi đạt $\ge 80\%$.<br>- Chi phí micro-USD hiển thị định dạng `$0.xxxx USD` hoặc `x µUSD` hoàn toàn bằng phép chia nguyên `int`.<br>- **Xóa dữ liệu AI trên server (`DELETE /ai/data`):** Hộp thoại nguy hiểm bắt buộc người dùng gõ chính xác chữ `XOA DU LIEU AI`, giải thích rõ chỉ xóa dữ liệu AI trên server và không ảnh hưởng dữ liệu LifeOS khác. |
| **AWP-6** | **Kiểm thử và Bằng chứng** | `XONG` | `[CONFIRMED]` | - **Unit/Contract test:** [`test/ai_fixtures_and_unit_test.dart`](file:///d:/LifeOS/mobile/test/ai_fixtures_and_unit_test.dart) (13/13 passed) khớp 100% cả 8 fixture `ai-*.json`, đủ 15 mã lỗi và định dạng micro-USD.<br>- **Widget & Golden test:** [`test/ai_widget_and_golden_test.dart`](file:///d:/LifeOS/mobile/test/ai_widget_and_golden_test.dart) (10/10 passed) bao gồm kiểm tra consent gating, concurrency gate, timeout 30s, dialog xóa nguy hiểm, và **6 ảnh Golden Light/Dark** lưu tại `docs/review/mobile_screenshots/ai_chat/`.<br>- **Full test suite:** Chạy `flutter test --no-pub -j 1` đạt **104 passed, 1 skipped, 0 failed**.<br>- **Phân tích tĩnh:** `dart analyze` đạt **`No issues found!`** (0 cảnh báo, 0 lỗi). |

---

## 3. Báo cáo Tình trạng E2E Backend Docker (Theo AWP-6 Mục 3)

- **Trạng thái:** `CHƯA CHẠY`
- **Lý do trung thực theo chỉ đạo:** Lớp `FakeLLMProvider` trong backend Spring Boot mang chú thích `@Profile("test")`. Trong khi đó, stack Docker local (`docker-compose.yml`) khởi chạy backend với profile `dev`. Do đó, `OpenAIProvider` được nạp và đòi hỏi khóa `OPENAI_API_KEY` thật trên môi trường máy chủ. Chúng tôi tuân thủ quy tắc không can thiệp sửa đổi backend Java và không tự ý giả lập kết quả rồi gọi là chạy thật.
- **Lệnh cần chạy để kiểm thử E2E trực tiếp khi backend hỗ trợ profile test:**
  ```bash
  # Kích hoạt backend với profile test để bật FakeLLMProvider:
  mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.profiles=test,dev
  # Sau đó chạy kiểm thử E2E trên Mobile:
  flutter test test/e2e_real_backend_test.dart --dart-define=LIFEOS_RUN_REAL_E2E=true
  ```

---

## 4. Xác nhận Nguyên tắc Vùng cấm (Zero Forbidden Files Touched)

Chúng tôi cam kết xác nhận 100%:
- **Backend:** Tuyệt đối không chạm vào bất kỳ file nào trong `backend/` hay các file cấu hình backend.
- **Hợp đồng & Tài liệu chuẩn:** Tuyệt đối không sửa `docs/api/openapi.yaml`, `docs/engineering/SYNC_PROTOCOL.md`, các file trong `docs/architecture/` và `docs/phases/`.
- **CSDL & Tầng Đồng bộ:** Không thay đổi `schemaVersion`, không thêm bảng SQLite mới vào `drift_tables.dart`, không sửa `SyncEngine`, không phát sinh mutation outbox cho AI.
- **Tầng mạng lõi:** `api_client.dart` giữ nguyên vẹn hành vi cũ; các phương thức gọi AI được đóng gói riêng biệt trong `lib/features/ai/data/ai_api.dart`.

---

## 5. Bằng chứng Kiểm thử Hiệu năng & Điều kiện D-M08

Theo quy định bắt buộc của quyết định `D-M08 [CONFIRMED]`:
- Tầng CSDL SQLite gốc không bị thay đổi (0 bảng mới, 0 mutation).
- **Kết quả đo bài kiểm thử Benchmark 50.000 bản ghi đồng bộ ngoại tuyến chạy đơn lẻ (`test/domain_modules_and_perf_test.dart`):**
  - **Thời gian thực tế:** **11.0 giây** (`00:11 +7: All tests passed!`).
  - **Mốc đối chiếu:** Nằm trọn vẹn trong khoảng baseline chuẩn **10–14 giây** đã cam kết.

---

## 6. Danh mục Tệp tin Tạo mới & Cập nhật

### Tệp tạo mới:
1. `mobile/lib/features/ai/data/ai_models.dart`: Toàn bộ DTO, request/response models và hàm format micro-USD.
2. `mobile/lib/features/ai/data/ai_api.dart`: `AiApiClient` cài đặt 14 API endpoint AI trực tuyến với timeout 35s.
3. `mobile/lib/features/ai/data/ai_providers.dart`: Riverpod providers cho AI Client, Preferences, Active Conversation.
4. `mobile/lib/features/ai/presentation/widgets/ai_disclaimer_banner.dart`: Dòng thông báo cố định về nội dung do AI tạo.
5. `mobile/lib/features/ai/presentation/widgets/ai_consent_sheet.dart`: Biểu mẫu 5 công tắc consent theo miền.
6. `mobile/lib/features/ai/presentation/widgets/ai_context_disclosure_sheet.dart`: Bottom sheet "AI đã đọc gì?".
7. `mobile/lib/features/ai/presentation/widgets/ai_plans_and_insights_sheets.dart`: Bộ sheet Lập kế hoạch, Chia nhỏ việc, Insight, Tóm tắt ghi chú.
8. `mobile/lib/features/ai/presentation/widgets/ai_usage_and_limits_sheet.dart`: Màn hình mức dùng, hạn mức và dialog xóa nguy hiểm.
9. `mobile/lib/features/ai/presentation/widgets/ai_conversation_list_sheet.dart`: Sheet quản lý danh sách hội thoại.
10. `mobile/lib/features/ai/presentation/ai_assistant_screen.dart`: Màn hình trò chuyện AI Assistant chính.
11. `mobile/test/ai_fixtures_and_unit_test.dart`: Bộ test fixture và unit test cho models & mã lỗi.
12. `mobile/test/ai_widget_and_golden_test.dart`: Bộ test widget, concurrency gate, dialog xóa và 6 ảnh Golden.
13. `docs/review/mobile_screenshots/ai_chat/`: Thư mục chứa 6 ảnh Golden đã kết xuất.

### Tệp cập nhật:
1. `mobile/lib/core/l10n/vi_strings.dart`: Bổ sung `enum AiErrorActionType`, `class AiErrorDescriptor`, và ánh xạ 15 mã lỗi AI OpenAPI sang tiếng Việt kèm hành động gợi ý.
2. `mobile/lib/features/review/presentation/overview_and_settings_tab_view.dart`: Bổ sung thẻ `tile_ai_assistant`, `tile_ai_settings`, `tile_delete_ai_data`.
3. `mobile/lib/features/finance/presentation/finance_tab_view.dart`: Bổ sung nút `btn_ai_finance_insight` tuân thủ consent gating.
4. `mobile/lib/features/knowledge/presentation/widgets/note_editor_sheet.dart`: Bổ sung nút `btn_ai_summarize_note` tuân thủ consent gating.
5. `mobile/test/daily_use_notification_test.dart`: Tinh chỉnh scroll test đảm bảo click chính xác widget.

---

## 7. Trích dẫn Nguyên văn Kết quả Kiểm thử (Verbatim Outputs)

### 7.1. Phân tích tĩnh (`dart analyze`)
```text
Analyzing mobile...
No issues found!
```

### 7.2. Bộ kiểm thử AI Fixtures & Models (`test/ai_fixtures_and_unit_test.dart`)
```text
00:00 +0: loading D:/LifeOS/mobile/test/ai_fixtures_and_unit_test.dart
00:00 +0: AWP-6: AI Contract Fixtures Gold Standard (8 fixtures ai-*.json) 1. Parse ai-preferences.response.json
00:00 +1: AWP-6: AI Contract Fixtures Gold Standard (8 fixtures ai-*.json) 2. Parse ai-chat.request.json
00:00 +2: AWP-6: AI Contract Fixtures Gold Standard (8 fixtures ai-*.json) 3. Parse ai-chat.response.json
00:00 +3: AWP-6: AI Contract Fixtures Gold Standard (8 fixtures ai-*.json) 4. Parse ai-context.response.json
00:00 +4: AWP-6: AI Contract Fixtures Gold Standard (8 fixtures ai-*.json) 5. Parse ai-error.response.json (RFC 9457)
00:00 +5: AWP-6: AI Contract Fixtures Gold Standard (8 fixtures ai-*.json) 6. Parse ai-plan.response.json
00:00 +6: AWP-6: AI Contract Fixtures Gold Standard (8 fixtures ai-*.json) 7. Parse ai-usage.response.json
00:00 +7: AWP-6: AI Contract Fixtures Gold Standard (8 fixtures ai-*.json) 8. Parse ai-action-proposal.response.json
00:00 +8: AWP-2 & AWP-6: Ánh xạ 15 Mã lỗi AI OpenAPI sang tiếng Việt & Gợi ý hành động Bao phủ đủ 15 mã lỗi trong ViStrings.mapBackendErrorCode và ViStrings.mapAiError
00:00 +9: AWP-2 & AWP-6: Ánh xạ 15 Mã lỗi AI OpenAPI sang tiếng Việt & Gợi ý hành động Kiểm tra đúng loại hành động gợi ý cho các mã quan trọng
00:00 +10: AWP-5: Tính toán micro-USD và hạn mức không dùng số thực double formatMicroUsd định dạng chính xác bằng phép chia nguyên
00:00 +11: AWP-5: Tính toán micro-USD và hạn mức không dùng số thực double AiQuotaWindow.isNearLimit báo cảnh báo khi đạt >= 80% (integer math)
00:00 +12: AWP-5: Tính toán micro-USD và hạn mức không dùng số thực double AiPreferencesResponse.initialDefault mặc định toàn bộ 5 miền TẮT (D264)
00:00 +13: All tests passed!
```

### 7.3. Bộ kiểm thử AI Widgets & Golden (`test/ai_widget_and_golden_test.dart`)
```text
00:00 +0: loading D:/LifeOS/mobile/test/ai_widget_and_golden_test.dart
00:00 +0: (setUpAll)
00:00 +0: AWP-1: Consent Sheet & Consent Gating Widget Tests Toggles phản ánh đúng trạng thái và gửi cập nhật lên server
00:03 +1: AWP-2: Concurrency Gate & Waiting State Widget Tests Chặn gửi chồng: khi đang chờ thì ô nhập và nút gửi bị vô hiệu hóa
00:05 +2: AWP-2: Concurrency Gate & Waiting State Widget Tests Quá thời hạn 30s hiển thị bong bóng lỗi kèm nút Thử lại
00:05 +3: AWP-5: Dangerous Delete AI Data Dialog Tests Yêu cầu nhập đúng chữ XOA DU LIEU AI mới kích hoạt nút xóa
00:06 +4: AWP-6: Golden Tests (Consent, Chat Waiting, Usage Limits Light/Dark) Golden 1 — AI Consent Sheet (Light)
00:06 +5: AWP-6: Golden Tests (Consent, Chat Waiting, Usage Limits Light/Dark) Golden 2 — AI Consent Sheet (Dark)
00:06 +6: AWP-6: Golden Tests (Consent, Chat Waiting, Usage Limits Light/Dark) Golden 3 — AI Chat & Waiting State (Light)
00:07 +7: AWP-6: Golden Tests (Consent, Chat Waiting, Usage Limits Light/Dark) Golden 4 — AI Chat & Waiting State (Dark)
00:07 +8: AWP-6: Golden Tests (Consent, Chat Waiting, Usage Limits Light/Dark) Golden 5 — AI Usage & Limits (Light)
00:07 +9: AWP-6: Golden Tests (Consent, Chat Waiting, Usage Limits Light/Dark) Golden 6 — AI Usage & Limits (Dark)
00:07 +10: (tearDownAll)
00:08 +10: All tests passed!
```

### 7.4. Toàn bộ Bộ kiểm thử Mobile (`flutter test --no-pub -j 1`)
```text
01:38 +104 ~1: All tests passed!
```
*(Chi tiết: 104 passed, 1 skipped (bài benchmark 50k được cô lập chạy riêng), 0 failed).*

---

## 8. Quyết định Kỹ thuật `D-M09` (Đã duyệt: `[CONFIRMED]`)

> **`D-M09 [CONFIRMED]` (Duyệt 2026-11-15):** Lối vào Trợ lý AI được bố trí tại: (1) Thẻ `tile_ai_assistant` trên Tab Tổng quan; (2) Nút ngữ cảnh trên Tab Tài chính (`btn_ai_finance_insight`), Biểu mẫu Ghi chú (`btn_ai_summarize_note`) tuân thủ triệt để consent gating (chỉ hiển thị khi miền tương ứng đã được người dùng bật consent); không chèn AppBar toàn cục vào `LifeOsShellScreen` để đảm bảo tính nhất quán của hệ thống và bảo toàn 100% pixel các màn hình nền tảng.

