# Prompt Mobile đợt 2 — "Dùng được hằng ngày" (`ANTIGRAVITY_DAILY_USE_PROMPT.md`)

> Gửi nguyên văn cho Antigravity. Phạm vi: `mobile/`, `ci/mobile-ci.yml`, `docs/review/MOBILE_*`. **Không sửa** `backend/`, `docs/api/openapi.yaml`, `SYNC_PROTOCOL.md`, `SyncEngine`, `LifeOsApiClient`, `entity_types.dart`, 29 bảng gốc, `schemaVersion` CSDL gốc. Làm một lượt MWP-A → MWP-D, một báo cáo cuối. Nguyên tắc cũ giữ nguyên: không bịa luật, quyết định mới ghi `[PROPOSED]`, điểm chưa rõ ghi `[OPEN]`, báo cáo đúng sự thật với 3 nhãn trạng thái.

---

## Mục tiêu

Giao diện v2 đã nghiệm thu. Đợt này biến app từ demo thành thứ người dùng mở hằng ngày: nhắc nhở thật, đăng nhập đầy đủ, và các form còn hoãn.

## MWP-A — Thông báo thật (nâng PWP9 từ MỘT PHẦN lên XONG)

1. Thêm `flutter_local_notifications` (+ `timezone`): lập lịch nhắc cục bộ cho reminder/thói quen/routine từ dữ liệu SQLite, nổ đúng giờ theo múi giờ thiết bị, kể cả khi app tắt.
2. Android: xin quyền `POST_NOTIFICATIONS` (Android 13+) đúng lúc (khi người dùng bật nhắc đầu tiên, không phải lúc mở app); khai báo kênh thông báo tiếng Việt; dùng inexact alarm mặc định, chỉ đề xuất exact alarm như `[PROPOSED]` nếu thấy cần.
3. iOS: thực thi giới hạn 64 thông báo chờ — ưu tiên theo thời gian gần nhất, lập lịch lại phần còn thừa khi app mở (logic chọn lọc đã có ở tầng dữ liệu thì nối vào, chưa có thì viết).
4. Bấm vào thông báo mở đúng màn hình liên quan (task, habit, sự kiện).
5. FCM: tích hợp `firebase_messaging` **sau một cờ cấu hình, mặc định tắt**, degrade êm khi thiếu file `google-services.json`/`GoogleService-Info.plist` (hai file này người dùng cung cấp sau, KHÔNG tự tạo tài khoản Firebase, không commit placeholder giả). Nếu việc này làm build đỏ khi thiếu file thì hoãn FCM, ghi `MỘT PHẦN` kèm lý do — không ép.
6. Màn Cài đặt: bật/tắt nhắc theo loại, giờ im lặng đọc từ `user_settings` hiện có.
7. Test: unit cho bộ chọn 64 của iOS và cho việc sinh lịch từ dữ liệu; widget test cho màn cài đặt nhắc.

## MWP-B — Bộ màn tài khoản đầy đủ

1. Đăng nhập, Đăng ký (kèm xác minh email — backend đã có luồng này, E2E từng đi qua), Quên mật khẩu, Đổi mật khẩu.
2. Trạng thái đầy đủ: đang gửi, lỗi từ server (hiển thị tiếng Việt theo mã lỗi của OpenAPI, không lộ mã kỹ thuật), offline (nói rõ cần mạng cho việc đăng nhập).
3. Token vào secure storage như hiện tại; đăng nhập xong vào thẳng Hôm nay; đăng xuất giữ dữ liệu cục bộ (Phương án C đã chốt).
4. Theo đúng hướng Botanical Premium và token hiện có; validate blur-first, nhãn hiện rõ (không chỉ placeholder), vùng chạm ≥ 48dp.

## MWP-C — Các form còn hoãn từ đợt trước

1. **Trình chạy Routine:** đi từng bước, đồng hồ cho bước có thời lượng, đánh dấu bỏ qua/hoàn thành, ghi check-in đúng idempotent như spec.
2. **Giao dịch định kỳ:** form tạo/sửa theo đúng recurrence JSON của spec (không RRULE), xem trước 3 lần tới.
3. **Mục tiêu tiết kiệm:** form tạo/sửa, tiến độ nạp từ dữ liệu finance.
4. **Thêm/sửa Milestone** cho Goal.
5. Tất cả ghi qua giao dịch cục bộ + `outbox` như mọi thực thể khác; tiền `Decimal10k`.

## MWP-D — Đánh bóng nhỏ

1. FAB "Thêm nhanh": tự ẩn khi cuộn xuống (hiện lại khi cuộn lên) hoặc đệm cuối danh sách đủ lớn — chọn một, áp dụng mọi tab.
2. Lời chào dùng tên hiển thị của người dùng (từ `user_settings`/profile), fallback "bạn" khi trống; không dùng tên workspace.
3. Rà một lượt các màn mới đợt trước xem còn chuỗi kỹ thuật, nhãn tiếng Anh, hay số liệu demo lọt ra không; sửa hết, liệt kê trong báo cáo.

## Bằng chứng trong báo cáo cuối (`docs/review/MOBILE_DAILY_USE_REPORT.md` + cập nhật `MOBILE_PROGRESS.md`)

- Nguyên văn `dart analyze`, `flutter test` (golden chạy 2 lượt không lệch), và `flutter test --dart-define=LIFEOS_RUN_REAL_E2E=true` với Docker backend (phải vẫn xanh — chứng minh không đụng đồng bộ).
- Ảnh golden mới cho: màn đăng nhập/đăng ký, trình chạy routine, form giao dịch định kỳ (Light/Dark), lưu `docs/review/mobile_screenshots/daily_use/`.
- Bảng MWP-A..D với 3 nhãn; mục nào `MỘT PHẦN`/`CHƯA LÀM` ghi lý do.
- Danh sách quyền hệ thống mới xin (notification, alarm) và lý do từng quyền.
- Xác nhận ranh giới: liệt kê file đã sửa, không file nào thuộc vùng cấm.
