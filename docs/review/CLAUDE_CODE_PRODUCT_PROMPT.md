# Prompt giao việc cho Claude Code — Giai đoạn Sản phẩm (Flutter + UI + Deploy)

> **ĐÃ THAY THẾ (2026-11-07):** theo quyết định của người dùng, giai đoạn sản phẩm được tách thành hai prompt riêng: **Flutter + UI → `ANTIGRAVITY_FLUTTER_UI_PROMPT.md`**; **deploy + AI (Phase 8) → `CHATGPT_DEPLOY_AI_PROMPT.md`**. Giữ file này chỉ để tham khảo; không dùng để giao việc.

Dán phần dưới dòng `---` vào Claude Code. Có **đúng một điểm dừng**: sau khi nộp kế hoạch (Phần A). Sau khi người dùng duyệt kế hoạch, làm liên tục đến hết (Phần B).

---

Bạn là kỹ sư mobile + DevOps cấp cao. Backend LifeOS đã hoàn thiện (`docs/review/BACKEND_FINAL_REPORT.md`: 12 module, 29 `entityType` đồng bộ, 847 test xanh). Nhiệm vụ của bạn: **đưa sản phẩm tới tay người dùng**: ứng dụng **Flutter (Android + iOS)** offline-first, **giao diện** hoàn chỉnh, và **triển khai** (backend, hạ tầng, phát hành ứng dụng). Làm việc như với backend: bám sát tài liệu, không bịa quy tắc, kiểm thử thật.

## 0. Bước 0 — kiểm tra và đọc (trước khi làm bất cứ điều gì)

1. Xác nhận trên đĩa các tài liệu sau tồn tại và đúng phiên bản mới nhất (nếu thiếu/mâu thuẫn: dừng và báo): `AGENTS.md`, `docs/review/BACKEND_FINAL_REPORT.md`, `docs/engineering/BACKEND_MASTER_PLAN.md`.
2. Đọc kỹ, theo thứ tự ưu tiên:
   - **Hợp đồng với server:** `docs/api/openapi.yaml`, `docs/api/fixtures/*` (kể cả `habit-vectors.json`, `recurrence-vectors.json`, `analytics-vectors.json`), `docs/engineering/API_CONVENTIONS.md`, `SYNC_PROTOCOL.md`, `SYNC_RULES.md`, `SECURITY_RULES.md`.
   - **Client:** `docs/engineering/FLUTTER_RULES.md`, `docs/architecture/SYSTEM_ARCHITECTURE.md` (§ Flutter/offline/sync client), `DATABASE_DESIGN.md` (SQLite local), `TESTING_RULES.md`.
   - **Sản phẩm/UX:** `docs/product/PRODUCT_CONTEXT.md` (các mục My Day, My Finance, My Life Review, nền tảng, ngôn ngữ, tiền tệ), `PRD.md`, `USE_CASES.md`, các đặc tả `docs/phases/PHASE_2…PHASE_7` (chỉ phần **client** và các mục "Flutter/offline" trong từng đặc tả).
   - **Rủi ro đã biết:** `BACKEND_FINAL_REPORT.md` §8 (rủi ro), §10 (hoãn), §12 (câu hỏi mở).
3. Ghi nhận ràng buộc môi trường: máy phát triển là Windows (`D:\LifeOS`). **Không build/ký được iOS trên Windows** — kế hoạch phải nêu rõ cách (CI macOS runner hoặc Mac của người dùng).

## Phần A — KẾ HOẠCH (điểm dừng duy nhất)

Chưa viết mã ứng dụng. Viết **`docs/engineering/PRODUCT_MASTER_PLAN.md`** (một tài liệu, theo cấu trúc của `BACKEND_MASTER_PLAN.md`), gồm:

1. **Phạm vi và thứ tự gói công việc `PWP1…PWPn`**, mỗi gói có tiêu chí hoàn thành đo được. Thứ tự gợi ý (được phép đề xuất khác, nêu lý do): khung dự án + CI → xác thực + lưu trữ an toàn → SQLite/Drift + outbox + SyncEngine (kể cả 410/full resync, hợp nhất 3 chiều, hàng đợi xung đột) → Task/Project/Category → My Day → Habit/Routine → Finance → Calendar/Reminder + local notification (iOS cap 64) → Notes/Tags/Goals → Review/Analytics/Dashboard → Thông báo in-app + FCM + cài đặt thiết bị → Cài đặt/xuất dữ liệu/xóa tài khoản → hoàn thiện UI/a11y → triển khai và phát hành.
2. **Kiến trúc client:** phân tầng, quản lý trạng thái (Riverpod theo `FLUTTER_RULES`), điều hướng, DI, ánh xạ schema SQLite ↔ 29 `entityType` (ngày `TEXT YYYY-MM-DD`, tiền = số nguyên ×10⁴, bitmask, v.v.), lưu token (secure storage), xử lý `errorCode` → thông điệp tiếng Việt, i18n (chỉ tiếng Việt, chuỗi trong file localization), múi giờ/"hôm nay" theo `user_settings`.
3. **Thiết kế đồng bộ phía client:** outbox, `baseVersion`, retry/backoff/`Retry-After`, mutation phụ thuộc (BLOCKED), giới hạn 30 ngày outbox, đăng xuất an toàn, tạm dừng sync, toàn vẹn local, nhật ký/chẩn đoán, `merge-vectors.json` mới (bạn tạo, chạy ở Dart; nếu muốn dùng chung với Java thì đề xuất).
4. **Tính toán offline phải khớp server:** My Day, chuỗi Habit, thống kê, số dư tài khoản, ngân sách, tiến độ Goal, analytics cơ bản — chứng minh bằng vector dùng chung Java–Dart.
5. **Hệ thống giao diện (design system):** token (màu/chữ/khoảng cách/bo góc/độ cao), theme sáng/tối, kiểu chữ hỗ trợ tiếng Việt, bộ component dùng lại, trạng thái rỗng/lỗi/đang tải, xác nhận hành động nguy hiểm (nêu tên đối tượng), chỉ báo offline/đang đồng bộ, **không bao giờ gây cảm giác mất dữ liệu**; phone + tablet; WCAG AA (tương phản, cỡ chạm ≥ 48dp, đọc màn hình, cỡ chữ hệ thống); quyền riêng tư (ẩn số tiền/nội dung trong chuyển ứng dụng, thông báo không lộ nội dung).
6. **Danh mục màn hình và bản đồ điều hướng** cho: Auth (đăng ký, xác minh email, đăng nhập, quên/đặt lại mật khẩu), My Day, Task/Project/Category, Habit/Routine, Finance (tài khoản, giao dịch nhập nhanh, chuyển khoản, ngân sách, định kỳ, mục tiêu tiết kiệm, báo cáo), Calendar/Time Block/Reminder, Notes/Tags/tìm kiếm, Goals/Milestones, Dashboard/Review/Analytics, Hộp thông báo + tùy chọn + giờ yên tĩnh, Cài đặt (múi giờ, tiền tệ, ngày đầu tuần, thiết bị, xuất dữ liệu, xóa tài khoản), Trạng thái đồng bộ + hàng đợi xung đột + màn hình giải quyết xung đột.
7. **Chiến lược kiểm thử:** unit, widget, golden, tích hợp với **backend thật** qua `docker compose` (+ mailpit), các luồng đầu-cuối bắt buộc: (a) đăng ký → xác minh email qua mailpit → đăng nhập; (b) tạo Task offline → online → xuất hiện trên server; (c) xung đột hai thiết bị (giả lập hai client) → giải quyết; (d) mất mạng giữa push; (e) 410 → full resync giữ outbox; (f) local notification đúng giờ + khử trùng với push; (g) xuất dữ liệu; (h) xóa tài khoản (cooldown/hủy); (i) đổi múi giờ. Ngưỡng độ phủ và cổng CI (`flutter analyze` sạch, test xanh).
8. **Kế hoạch triển khai (deploy):**
   - Môi trường dev/staging/production; **ít nhất 2 phương án hạ tầng backend** (ví dụ VPS + Docker Compose; PaaS như Render; dịch vụ container của nhà cung cấp lớn) với chi phí ước tính, ưu/nhược, **một đề xuất có lý do** (một instance, ngân sách nhỏ, người vận hành một người).
   - `Dockerfile` production, `docker-compose` production (profile **prod**, không dùng khóa mặc định), `TZ=UTC`, biến môi trường/secret (JWT, `NOTIFICATION_UNSUBSCRIBE_SECRET`, SMTP, FCM, `EXPORT_DIR`, DB), TLS/domain, reverse proxy và cấu hình `X-Forwarded-For` đúng cho rate limit, kích thước body, health/readiness, giới hạn tài nguyên, sao lưu và khôi phục PostgreSQL (RPO/RTO đề xuất, **thử khôi phục thật** trong kế hoạch), giám sát/cảnh báo dùng `docs/ops/ALERTS.md` + Prometheus/Grafana (`ops/`), nhật ký, xoay secret.
   - CI/CD: workflow cho backend (đã có `ci/backend-ci.yml`), mobile (analyze/test/build), job nightly (mô phỏng đa thiết bị đầy đủ + `BackendBenchmark`), triển khai có thể quay lui (rollback), migration an toàn khi nâng cấp. Ghi workflow vào `ci/` và nêu rõ phần người dùng phải chép vào `.github/workflows/` nếu bạn không được phép ghi.
   - Phát hành app: Android (keystore, `aab`, Google Play kiểm thử nội bộ → kín → production), iOS (chứng chỉ, TestFlight, App Store), FCM/APNs, tên gói, biểu tượng/splash, chính sách quyền riêng tư và mô tả cửa hàng (bản nháp), đánh số phiên bản và chính sách `minSupportedClientVersion`.
9. **Việc chỉ con người làm được** (tài khoản Apple Developer, Google Play Console, dự án Firebase/FCM, tên miền, nhà cung cấp SMTP production, hosting, ngân sách): liệt kê, nêu **khi nào cần** và **cách làm không chặn công việc** (ví dụ dùng mailpit/emulator trước).
10. **Việc backend phát sinh** và quyết định mặc định đề xuất cho các Open Questions của `BACKEND_FINAL_REPORT.md` §12 (FR-GOAL-06, sửa `AGENTS.md` §3 về module, gauge `lifeos.job.last_success`, CI nightly, kho tệp xuất, `TZ=UTC`). Nêu thay đổi hợp đồng backend nào là cần thiết (nếu có) và vì sao; mặc định: **không đổi hợp đồng**.
11. **Rủi ro và giả định**, ước lượng độ phức tạp từng gói (nhỏ/vừa/lớn), thứ tự phụ thuộc, **cắt giảm phạm vi nếu cần** (đề xuất bản phát hành đầu tiên tối thiểu "MVP" gồm Phase 1–2 và bản đầy đủ).
12. **Quyết định cần người dùng chọn** (tối đa 10, mỗi mục có phương án khuyến nghị) — thiết kế sao cho người dùng chỉ cần trả lời một dòng.

**Khi xong kế hoạch: DỪNG**, thông báo người dùng trong phiên và chờ câu duyệt. Không viết mã ứng dụng trước khi được duyệt.

## Phần B — THỰC THI (sau khi người dùng duyệt kế hoạch)

Quy tắc tự chủ **giống `BACKEND_MASTER_PLAN.md` §3 và §6**, áp dụng nguyên văn cho giai đoạn này:
- Làm liên tục `PWP1 → PWPn`, **không xin duyệt giữa chừng**; quyết định chi tiết mà tài liệu không nêu thì chọn phương án đơn giản, an toàn, nhất quán và ghi vào `docs/review/PRODUCT_PROGRESS.md` (nhãn `[PROPOSED]`).
- Cập nhật `PRODUCT_PROGRESS.md` sau mỗi gói: trạng thái, số test, thay đổi, quyết định phát sinh, việc hoãn.
- Không làm yếu kiểm thử (không `skip`, không nới ngưỡng, không xóa test để qua).
- **Chỉ dừng** khi: tài liệu mâu thuẫn không giải quyết được; cần đổi hợp đồng backend `[CONFIRMED]` ngoài kế hoạch đã duyệt; quyết định không hoàn tác được mà hai hướng đều hợp lý; một test bắt buộc vẫn đỏ sau 3 lần sửa nguyên nhân gốc; thiếu việc **chỉ con người làm được** mà không thể đi vòng (ghi rõ cái gì cần, rồi tiếp tục các gói không phụ thuộc).
- **Chạm backend:** chỉ sửa lỗi thật phát hiện khi tích hợp, kèm test, liệt kê trong báo cáo; `mvn clean verify` (847 test) phải luôn xanh; không đổi hợp đồng/migration đã có; thay đổi hợp đồng phải nằm trong kế hoạch đã duyệt.
- Giữ bất biến phía client: ghi local trước, outbox ổn định, không mất mutation chờ gửi (kể cả khi 410, đăng xuất, cập nhật app), áp dụng một trang pull trong **một** transaction SQLite cùng cập nhật cursor, không tự hợp nhất dữ liệu tài chính, không dữ liệu nhạy cảm trong log/crash report/thông báo, tiền không dùng `double`.
- Mỗi gói kết thúc khi: `flutter analyze` sạch, test xanh (unit/widget/golden/tích hợp liên quan), luồng đầu-cuối liên quan chạy được với backend thật qua compose, tài liệu liên quan cập nhật.

## Bàn giao cuối (một báo cáo duy nhất)

Viết **`docs/review/PRODUCT_FINAL_REPORT.md`** và cập nhật `AGENTS.md`, `SYSTEM_ARCHITECTURE.md`, `FLUTTER_RULES.md` (nếu quy ước đổi), tài liệu vận hành:
1. Tóm tắt, bảng gói công việc (test, thời gian), cấu trúc dự án.
2. Bản đồ yêu cầu → màn hình → test (FR-*, UC-*, các test ID client còn hoãn: SYN-A02..A04/A09/A11..A13/A15, KNW-05, INT-10 phần Dart, FR-HAB/FR-MYD).
3. Kết quả các luồng đầu-cuối (a)–(i) và số đo hiệu năng app (khởi động lạnh, cuộn danh sách 10 000 mục, đồng bộ lần đầu 50 000 bản ghi, thời lượng pin/nền nếu đo được).
4. Hạ tầng đã dựng/viết (Dockerfile, compose prod, CI/CD, giám sát, sao lưu + **kết quả thử khôi phục**), runbook (`docs/ops/`): triển khai, rollback, xoay secret, khôi phục, sự cố thường gặp.
5. Danh sách artifact: `apk/aab`, hướng dẫn build iOS, hướng dẫn phát hành cửa hàng, bản nháp chính sách quyền riêng tư/mô tả cửa hàng.
6. Quyết định phát sinh `[PROPOSED]`, lỗi backend đã sửa, test cũ đã sửa, rủi ro, điểm cần người xem kỹ, việc hoãn, việc đang chờ con người.
