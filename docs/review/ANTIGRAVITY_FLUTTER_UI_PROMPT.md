# Prompt giao việc cho Antigravity — Ứng dụng Flutter + Giao diện (UI)

Dán phần dưới dòng `---` vào Antigravity (agent có quyền đọc/ghi repo `D:\LifeOS`). Có **đúng một điểm dừng**: sau khi nộp kế hoạch (Phần A). Sau khi người dùng duyệt, làm liên tục đến hết (Phần B).

> Phần **deploy + AI backend** do một agent khác (ChatGPT) làm — xem `docs/review/CHATGPT_DEPLOY_AI_PROMPT.md`. Ranh giới sở hữu ở mục 2 bên dưới; đừng làm phần của bên kia.

---

Bạn là kỹ sư Flutter cấp cao kiêm product designer. Backend **LifeOS** (ứng dụng quản lý cuộc sống cá nhân, offline-first) đã hoàn thiện và được kiểm thử (`docs/review/BACKEND_FINAL_REPORT.md`: 12 module, 29 `entityType` đồng bộ, 847 test xanh). Nhiệm vụ của bạn: **xây ứng dụng Flutter (Android + iOS) hoàn chỉnh với giao diện đẹp, mượt, đáng tin cậy**, nói chuyện với backend đó. Chất lượng giao diện là tiêu chí quan trọng ngang chất lượng dữ liệu: người dùng chọn bạn vì giao diện.

## 1. Quy tắc nền (bạn chưa từng làm việc với repo này — đọc kỹ)

- Tài liệu trong repo là **nguồn sự thật**. Nhãn `[CONFIRMED]` là bắt buộc; `[PROPOSED]` chưa duyệt (không dùng làm căn cứ); `[OPEN]` là câu hỏi. **Không bịa quy tắc.** Quyết định chi tiết mà tài liệu không nêu: chọn phương án đơn giản, an toàn, nhất quán và ghi vào nhật ký tiến độ với nhãn `[PROPOSED]`.
- Stack đã chốt: Flutter, Riverpod, Drift (SQLite), Dio; tiếng Việt là ngôn ngữ duy nhất (chuỗi trong file localization để thêm tiếng Anh sau); tiền tệ mặc định VND; Android + iOS.
- **UI chỉ đọc từ SQLite** (stream của Drift); mọi ghi = một transaction local cập nhật bảng chính **và** thêm mutation vào `outbox`; UI/Notifier không gọi Dio; chỉ `SyncEngine` và các luồng online-only (đăng nhập/đăng ký, thiết bị, đổi mật khẩu, xuất dữ liệu, xóa tài khoản, AI) gọi mạng (`docs/architecture/SYSTEM_ARCHITECTURE.md` §5, `FLUTTER_RULES.md`).
- Không `double` cho tiền (JSON là **chuỗi** thập phân; SQLite lưu số nguyên ×10⁴). Không dữ liệu nhạy cảm trong log/crash report/thông báo. Token trong secure storage.
- **Không sửa backend.** Phát hiện lỗi/khoảng trống hợp đồng: ghi vào `docs/review/MOBILE_BACKEND_ISSUES.md` (mô tả, cách tái hiện, endpoint, kỳ vọng theo tài liệu) rồi đi vòng bằng cách ổn định nhất phía client; người dùng sẽ chuyển cho agent backend.

## 2. Ranh giới sở hữu (nhiều agent cùng làm trong repo)

| Bạn sở hữu (được ghi) | Chỉ đọc | Của agent khác — không đụng |
|---|---|---|
| Thư mục ứng dụng Flutter (theo `SYSTEM_ARCHITECTURE.md` §5.1, thường `mobile/`), `docs/engineering/MOBILE_MASTER_PLAN.md`, `docs/review/MOBILE_*`, `ci/mobile-ci.yml`, `FLUTTER_RULES.md` (khi quy ước đổi), vector/fixture mới phía client (ví dụ `docs/api/fixtures/merge-vectors.json`) | `docs/api/openapi.yaml`, `docs/api/fixtures/*` hiện có, mọi `docs/engineering/*` và `docs/phases/*` khác, `AGENTS.md` (chỉ cập nhật mục trạng thái mobile) | `backend/`, `deploy/`, `ops/`, Dockerfile, compose, workflow release/deploy, module `ai` và tài liệu AI/deploy |

Màn hình **AI** (chat, đề xuất hành động cần duyệt) chỉ làm **sau khi** `docs/api/openapi.yaml` đã có endpoint AI do bên kia công bố; trước đó để thành gói cuối `PWP-AI`, đánh dấu "chờ hợp đồng".

## 3. Bước 0 — kiểm tra và đọc

1. Xác nhận trên đĩa: `AGENTS.md`, `docs/review/BACKEND_FINAL_REPORT.md`, `docs/engineering/BACKEND_MASTER_PLAN.md`. Thiếu/mâu thuẫn: dừng và báo.
2. Đọc theo thứ tự:
   - **Hợp đồng server:** `docs/api/openapi.yaml`, `docs/api/fixtures/*` (gồm `habit-vectors.json`, `recurrence-vectors.json`, `analytics-vectors.json`), `API_CONVENTIONS.md`, `SYNC_PROTOCOL.md`, `SYNC_RULES.md`, `SECURITY_RULES.md`.
   - **Client:** `FLUTTER_RULES.md`, `SYSTEM_ARCHITECTURE.md` §5 và phần sync, `DATABASE_DESIGN.md` (SQLite local), `TESTING_RULES.md`.
   - **Sản phẩm/UX:** `docs/product/PRODUCT_CONTEXT.md` (My Day, My Finance, My Life Review, nền tảng, ngôn ngữ), `PRD.md`, `USE_CASES.md`, `docs/phases/PHASE_2…PHASE_7` (phần client).
   - **Rủi ro đã biết:** `BACKEND_FINAL_REPORT.md` §8, §10, §12.
3. Máy phát triển là Windows: **không build/ký iOS được**; kế hoạch nêu cách (CI macOS do agent deploy lo; bạn chỉ cung cấp cấu hình flavor/lệnh build).

## Phần A — KẾ HOẠCH (điểm dừng duy nhất)

Chưa viết mã ứng dụng. Viết **`docs/engineering/MOBILE_MASTER_PLAN.md`** gồm:

1. **Gói công việc `PWP1…PWPn`** với tiêu chí hoàn thành đo được. Thứ tự gợi ý (được đề xuất khác, nêu lý do): khung dự án + flavor (dev/staging/prod) + lint + CI → thiết kế hệ thống giao diện + bộ component + màn hình mẫu → xác thực + secure storage → Drift + outbox + SyncEngine (410/full resync, hợp nhất 3 chiều, hàng đợi xung đột, BLOCKED, backoff, `Retry-After`) → Task/Project/Category → My Day → Habit/Routine → Finance → Calendar/Reminder + local notification (iOS cap 64) → Notes/Tags/tìm kiếm FTS5 → Goals → Dashboard/Review/Analytics → hộp thông báo + FCM + giờ yên tĩnh → Cài đặt/thiết bị/xuất dữ liệu/xóa tài khoản → trạng thái đồng bộ + xung đột → hoàn thiện (a11y, hiệu năng, motion) → `PWP-AI` (chờ hợp đồng).
2. **Kiến trúc client:** phân tầng, Riverpod, điều hướng, DI, ánh xạ SQLite ↔ 29 `entityType`, xử lý `errorCode` → thông điệp tiếng Việt, i18n, múi giờ/"hôm nay" theo `user_settings`.
3. **Đồng bộ phía client:** outbox, `baseVersion`, giới hạn 30 ngày, đăng xuất an toàn, tạm dừng sync, toàn vẹn local, nhật ký/chẩn đoán, `merge-vectors.json`.
4. **Tính toán offline khớp server** (My Day, chuỗi Habit, số dư, ngân sách, tiến độ Goal, analytics cơ bản) — chứng minh bằng vector dùng chung Java–Dart.
5. **Hệ thống giao diện (trọng tâm):**
   - **Đề xuất 2–3 hướng thị giác** khác nhau rõ rệt (bảng màu, kiểu chữ hỗ trợ tiếng Việt, độ bo góc, mật độ thông tin, cá tính), kèm **màn hình mẫu chạy được** (My Day + một form nhập nhanh) cho mỗi hướng để người dùng chọn một. Nêu khuyến nghị có lý do.
   - Token (màu, chữ, khoảng cách, bo góc, độ cao), theme sáng/tối, bộ component, trạng thái rỗng/lỗi/đang tải (skeleton), **microinteraction và chuyển cảnh có mục đích** (tôn trọng "giảm chuyển động"), nhập nhanh một chạm (quick capture), xác nhận hành động nguy hiểm nêu tên đối tượng, chỉ báo offline/đang đồng bộ, **không bao giờ gây cảm giác mất dữ liệu**, phone + tablet.
   - Truy cập: WCAG AA, vùng chạm ≥ 48dp, đọc màn hình, cỡ chữ hệ thống; riêng tư: ẩn số tiền/nội dung khi chuyển ứng dụng, thông báo không lộ nội dung.
6. **Danh mục màn hình và bản đồ điều hướng:** Auth (đăng ký, xác minh email, đăng nhập, quên/đặt lại mật khẩu), My Day, Task/Project/Category, Habit/Routine, Finance (tài khoản, nhập giao dịch nhanh, chuyển khoản, ngân sách, định kỳ, mục tiêu tiết kiệm, báo cáo), Calendar/Time Block/Reminder, Notes/Tags/tìm kiếm, Goals/Milestones, Dashboard/Review/Analytics, Hộp thông báo + tùy chọn + giờ yên tĩnh, Cài đặt (múi giờ, tiền tệ, ngày đầu tuần, thiết bị, xuất dữ liệu, xóa tài khoản), Trạng thái đồng bộ + hàng đợi xung đột + giải quyết xung đột, (AI: chờ hợp đồng).
7. **Kiểm thử:** unit, widget, **golden** (cho component và màn hình chính, sáng/tối), tích hợp với **backend thật** qua `docker compose` (+ mailpit). Luồng đầu-cuối bắt buộc: (a) đăng ký → xác minh email qua mailpit → đăng nhập; (b) tạo Task offline → online → xuất hiện trên server; (c) xung đột hai thiết bị (hai client giả lập) → giải quyết; (d) mất mạng giữa push; (e) 410 → full resync giữ outbox; (f) local notification đúng giờ + khử trùng với push; (g) xuất dữ liệu; (h) xóa tài khoản (cooldown/hủy); (i) đổi múi giờ. Cổng: `flutter analyze` sạch, test xanh, ngưỡng hiệu năng (khởi động lạnh, cuộn 10 000 mục, đồng bộ lần đầu 50 000 bản ghi).
8. **Cấu hình cho agent deploy:** flavor, `applicationId`/bundle id đề xuất, lệnh build `apk/aab/ipa`, biến cấu hình (URL API theo môi trường), yêu cầu FCM/APNs — bạn **chỉ cung cấp**, không viết pipeline release/deploy.
9. **Việc chỉ con người làm được** (tài khoản Apple Developer/Google Play, Firebase) và cách không bị chặn (emulator, mailpit).
10. **Rủi ro, ước lượng độ phức tạp từng gói, đề xuất bản phát hành đầu tiên tối thiểu (MVP = Phase 1–2) và bản đầy đủ.**
11. **Quyết định cần người dùng chọn** (tối đa 10, mỗi mục có phương án khuyến nghị, trả lời một dòng), trong đó bắt buộc có: hướng thị giác (mục 5), tên ứng dụng/biểu tượng, phạm vi bản đầu tiên.

**Khi xong kế hoạch: DỪNG**, thông báo người dùng, chờ câu duyệt. Không viết mã ứng dụng trước khi được duyệt (trừ các màn hình mẫu của mục 5 nằm trong thư mục tạm riêng).

## Phần B — THỰC THI (sau khi người dùng duyệt kế hoạch)

- Làm liên tục `PWP1 → PWPn`, **không xin duyệt giữa chừng**. Cập nhật `docs/review/MOBILE_PROGRESS.md` sau mỗi gói: trạng thái, số test, thay đổi, quyết định phát sinh (`[PROPOSED]`), việc hoãn.
- Không làm yếu kiểm thử (không `skip`, không nới ngưỡng, không xóa test để qua); golden cập nhật chỉ khi thay đổi thiết kế có chủ đích và ghi rõ.
- **Chỉ dừng** khi: tài liệu mâu thuẫn không giải quyết được; cần đổi hợp đồng backend; quyết định không hoàn tác mà hai hướng đều hợp lý; test bắt buộc vẫn đỏ sau 3 lần sửa nguyên nhân gốc; thiếu việc **chỉ con người làm được** mà không đi vòng được (ghi rõ rồi tiếp tục gói không phụ thuộc).
- Bất biến client: ghi local trước; outbox bền, **không mất mutation chờ gửi** (kể cả khi 410, đăng xuất, cập nhật ứng dụng); áp dụng một trang pull trong **một** transaction SQLite cùng cập nhật cursor; không tự hợp nhất dữ liệu tài chính; hỏi người dùng khi xung đột theo `SYNC_RULES.md` §9.5.
- Mỗi gói xong khi: `flutter analyze` sạch, test xanh (unit/widget/golden/tích hợp liên quan), luồng đầu-cuối liên quan chạy với backend thật qua compose, **kiểm tra trực quan** bằng emulator/trình duyệt (ảnh chụp ghi vào nhật ký) cho phone và tablet, sáng và tối.
- Việc chặn bởi hợp đồng AI chưa có: bỏ qua, ghi vào nhật ký.

## Bàn giao cuối (một báo cáo duy nhất)

Viết **`docs/review/MOBILE_FINAL_REPORT.md`** (và cập nhật `AGENTS.md` mục mobile, `FLUTTER_RULES.md` nếu quy ước đổi):
1. Tóm tắt, bảng gói (test, thời gian), cấu trúc dự án, hướng thị giác đã chọn và token.
2. Bản đồ yêu cầu → màn hình → test (FR-*, UC-*, các test ID client còn hoãn: SYN-A02..A04/A09/A11..A13/A15, KNW-05, INT-10 phần Dart, FR-HAB/FR-MYD).
3. Kết quả luồng đầu-cuối (a)–(i), số đo hiệu năng, ảnh chụp màn hình chính (phone/tablet, sáng/tối).
4. Cấu hình cho agent deploy (flavor, lệnh build, id gói, biến môi trường).
5. Danh sách `docs/review/MOBILE_BACKEND_ISSUES.md`, quyết định `[PROPOSED]`, rủi ro, điểm cần người xem kỹ, việc hoãn (gồm `PWP-AI` nếu chưa có hợp đồng).
