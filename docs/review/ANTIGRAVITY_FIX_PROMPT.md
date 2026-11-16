# Prompt sửa lỗi Mobile cho Antigravity (`ANTIGRAVITY_FIX_PROMPT.md`)

> Gửi nguyên văn cho Antigravity. Phạm vi: chỉ `mobile/`, `ci/mobile-ci.yml`, và các file `docs/review/MOBILE_*`. **Không sửa `backend/`.**

---

Bạn vừa báo hoàn tất PWP1–PWP15. Tôi đã review và **chưa nghiệm thu**. Hãy xử lý lần lượt các mục dưới đây. Nguyên tắc cũ vẫn giữ: không tự bịa luật; mọi thay đổi ngoài tài liệu phải ghi `[PROPOSED]` và dừng chờ duyệt; không đổi nhãn `[CONFIRMED]`/`[PROPOSED]`/`[OPEN]` khi chưa có câu duyệt của người dùng. Báo cáo phải nói đúng điều đã làm và chưa làm. "Chưa làm" được chấp nhận, "đã làm" mà không có bằng chứng thì không.

## Nhóm 1 — Sửa báo cáo cho đúng sự thật (làm trước)

1. `test/e2e_real_backend_test.dart` đang chạy trên server giả `_StatefulProtocolServerHarness`; không có code nào đọc cờ `LIFEOS_RUN_REAL_E2E`. Hãy:
   - Đổi tên file thành `e2e_protocol_harness_test.dart` (hoặc tương đương) và sửa mọi chỗ nhắc tên cũ.
   - Sửa `MOBILE_FINAL_REPORT.md` và `MOBILE_PROGRESS.md`: không dùng cụm "backend thật" cho bài test này.
   - Sửa `MOBILE_BACKEND_ISSUES.md`: thay câu "0 lỗi hợp đồng" bằng "chưa chạy với backend thật nên chưa kết luận được".
   - Server giả không được tự đặt trường mà `docs/api/openapi.yaml` không có (ví dụ `dayStartHour`, `verificationTokenForMailpit`). Đối chiếu từng response của harness với OpenAPI và sửa.
2. Với mỗi gói PWP, ghi trạng thái là một trong: `XONG (có bằng chứng: test/lệnh)`, `MỘT PHẦN (nêu phần thiếu)`, `CHƯA LÀM`. Không dùng ✅ chung chung.

## Nhóm 2 — E2E với backend thật

3. Dựng backend bằng `docker compose` của `backend/` (xem `docs/engineering/`), viết test E2E thật gọi `http://localhost:8080`, đọc cờ qua `--dart-define=LIFEOS_RUN_REAL_E2E=true`, mặc định bỏ qua khi không bật. Chạy đủ 9 luồng (a)–(i).
4. Nếu Docker bị chặn trên máy bạn, **ghi rõ "chưa chạy" cùng lý do** vào báo cáo, và để lại lệnh chạy chính xác cho người dùng. Không giả lập rồi gọi là thật.
5. Mọi sai khác hợp đồng phát hiện được ghi vào `MOBILE_BACKEND_ISSUES.md` (không sửa backend).

## Nhóm 3 — Thiếu so với bản giao

6. Tạo `android/` và `ios/` (`flutter create --platforms=android,ios .` trong `mobile/`), giữ nguyên `lib/` và `pubspec.yaml`. Kiểm tra `flutter build apk --debug` chạy được và ghi kết quả.
7. Tạo `ci/mobile-ci.yml` (analyze + test + build debug). Người dùng sẽ tự copy sang `.github/workflows/`.
8. Golden test: hoặc thêm `matchesGoldenFile` thật cho các màn chính (My Day, Tài chính, Kế hoạch, Tri thức × Light/Dark × phone/tablet) kèm file `.png` golden, hoặc đổi tên file test thành `ui_widget_a11y_test.dart` cho đúng nội dung. Làm cả hai nếu được.
9. Chụp ảnh màn hình các màn chính (Light/Dark) lưu vào `docs/review/mobile_screenshots/` để người dùng đối chiếu hướng "Calm Botanical" đã duyệt.

## Nhóm 4 — Lệch stack và phạm vi

10. **Drift:** stack đã chốt là Drift, nhưng `lib/` không import Drift, chỉ dùng `sqlite3` thô. Chọn một:
    - (a) chuyển lớp DB sang Drift (kèm `drift_dev`, `build_runner`), hoặc
    - (b) giữ `sqlite3` thô và ghi `MP-04 [PROPOSED]` nêu lý do, rủi ro, và gỡ `drift` khỏi `pubspec`.
    Không tự chọn (b) ngầm. Dừng chờ duyệt nếu chọn (b).
11. **Wiki-link / Backlinks:** `PHASE_6` ghi rõ "liên kết hai chiều kiểu wiki" nằm ngoài phạm vi. Gỡ `saveNoteWithWikiLinks`, `extractWikiLinkTitles`, `_syncWikiLinksForNote` và bảng `note_link` ở client (trừ khi `docs/api/openapi.yaml` có entity tương ứng; nếu có, trích dẫn dòng). Nếu muốn giữ, ghi `[PROPOSED]` riêng.
12. **Notification (PWP9):** hoặc cài thật (`flutter_local_notifications` + lập lịch theo múi giờ + giới hạn 64 thông báo iOS + FCM nhận push, khai báo đúng trong `pubspec`), hoặc ghi PWP9 là `MỘT PHẦM` và liệt kê rõ phần thiếu. Không ghi "hoàn thành" khi chưa có hạ tầng.

## Nhóm 5 — Đối chiếu hợp đồng với spec (không đoán, trích dòng tài liệu)

Với mỗi mục, mở tài liệu nêu bên cạnh, trích số dòng hoặc quyết định (D...), rồi sửa client cho khớp. Nếu spec mâu thuẫn hoặc không nói, ghi `[OPEN]` và hỏi.

13. **`dayStartHour`:** đối chiếu `docs/api/openapi.yaml` (schema `user_settings`) và `docs/phases/PHASE_7_INTELLIGENCE_ANALYTICS.md`. Backend hiện có `weekStartDay`; nếu không có `dayStartHour` thì client không được dựa vào nó để tính "ngày của người dùng". Sửa `UserDayClock` theo trường thực tế.
14. **Chuyển khoản:** đối chiếu `PHASE_3_FINANCE.md` (D100–D119, đặc biệt D104 và D118) về thực thể chuyển khoản và việc có quy đổi tiền tệ hay không. `createTransferPair` đang tạo 2 giao dịch `TRANSFER` nối bằng `transferPairId` kèm `exchangeRate`. Sửa theo spec; nếu spec dùng entity `account_transfer` riêng, dùng đúng entity đó.
15. **Recurrence:** đối chiếu D126 và `docs/api/openapi.yaml` về định dạng quy tắc lặp gửi lên server (JSON hay chuỗi `RRULE`). Client phải gửi và nhận đúng định dạng đó.
16. **`Idempotency-Key`:** backend dùng `mutationId` cho idempotency của sync. Giữ header chỉ khi OpenAPI nêu nó; nếu không, bỏ khỏi danh sách tính năng trong báo cáo.

## Nhóm 6 — Cấu trúc code

17. Tách `app_database.dart` (1.272 dòng), `lifeos_local_repository.dart` (1.265 dòng), `lifeos_shell_screen.dart` (1.599 dòng) theo `FLUTTER_RULES.md`: một thư mục `features/<module>/{data,domain,presentation}` cho từng module. Không đổi hành vi; giữ toàn bộ test xanh sau mỗi bước tách.

## Bằng chứng bắt buộc khi báo cáo lại

- Dán nguyên văn đầu ra của `dart analyze` và `flutter test` (kèm số ca).
- Nếu chạy E2E thật: dán đầu ra; nếu không: ghi "chưa chạy" cùng lý do.
- Danh sách file đã tạo, đổi, xóa, theo từng mục 1–17.
- Mục nào chưa làm: ghi "chưa làm" và lý do, không bỏ trống.
