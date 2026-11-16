# Prompt Mobile đợt 3 — Màn hình Trợ lý AI (`ANTIGRAVITY_AI_CHAT_PROMPT.md`)

> Gửi nguyên văn cho Antigravity. Phạm vi: `mobile/`, `docs/review/MOBILE_*`. **Không sửa** `backend/`, `docs/api/openapi.yaml`, `SYNC_PROTOCOL.md`, `SyncEngine`, `entity_types.dart`, 29 bảng gốc, `schemaVersion` CSDL gốc. Làm một lượt AWP-1 → AWP-6, một báo cáo cuối `docs/review/MOBILE_AI_CHAT_REPORT.md`. Nguyên tắc cũ giữ nguyên: 3 nhãn trạng thái, quyết định mới `[PROPOSED]`, điểm mơ hồ `[OPEN]`, không khai "xong" khi thiếu bằng chứng.

---

## 0. Bối cảnh

Backend AI (module `ai`) đã chạy: **14/18 path** trong `docs/api/openapi.yaml` có runtime, 879 test xanh. **4 path action-proposal (`/ai/action-proposals*`) chưa có runtime (AIP6 hoãn) — KHÔNG làm UI cho chúng đợt này.** Hợp đồng và dữ liệu mẫu: các path `/api/v1/ai/*` trong OpenAPI + 8 fixture `docs/api/fixtures/ai-*.json`. Đọc thêm `docs/phases/PHASE_8_AI_ASSISTANT.md` (consent, giới hạn, hành vi lỗi) trước khi code.

Khác biệt quan trọng so với phần còn lại của app: **AI là tính năng online**. Mọi gọi AI đi thẳng `LifeOsApiClient` hiện có (thêm phương thức mới trong file mới, ví dụ `ai_api.dart` — không sửa `api_client.dart` lõi nếu không bắt buộc; nếu bắt buộc phải sửa, chỉ thêm, không đổi hành vi cũ, và khai rõ). **Tuyệt đối không đi qua `outbox`, không thêm bảng vào CSDL đồng bộ gốc.** Hội thoại AI lưu trên server, không sync.

## AWP-1 — Consent theo miền (cửa vào bắt buộc)

1. Màn/sheet "Trợ lý AI" lần đầu mở: giải thích ngắn gọn AI đọc dữ liệu gì, và 5 công tắc consent theo miền đúng hợp đồng `/ai/preferences`: Công việc & Lịch, Thói quen, Mục tiêu, Ghi chú, Tài chính — **tất cả mặc định TẮT**; Tài chính là công tắc độc lập có cảnh báo riêng.
2. Đọc/ghi qua `GET/PUT /api/v1/ai/preferences`. Thu hồi consent có hiệu lực từ request kế tiếp.
3. Chưa bật miền nào vẫn được chat chung (không ngữ cảnh); UI nói rõ AI chưa đọc dữ liệu nào.
4. Một dòng cố định trong màn AI: nội dung do AI tạo, có thể sai — kiểm tra trước khi làm theo.

## AWP-2 — Chat và hội thoại

1. Lối vào: nút "Trợ lý AI" trên tab Tổng quan + icon trên app bar của shell (đề xuất khác thì ghi `[PROPOSED]`).
2. Danh sách hội thoại (`GET /ai/conversations`, phân trang), tạo mới, đổi tiêu đề nếu hợp đồng cho phép, xóa hội thoại (`DELETE /ai/conversations/{id}`) với xác nhận.
3. Màn chat: gửi tin (`POST .../messages`), hiển thị lịch sử, phân trang kéo lên. **Non-streaming theo O-AI-08:** sau khi gửi hiện trạng thái chờ (skeleton/typing) tới 30 giây; quá hạn hiện lỗi thời gian kèm nút thử lại. Chặn gửi chồng khi đang chờ (backend có concurrency gate).
4. Ánh xạ đủ 15 mã lỗi AI của OpenAPI sang tiếng Việt trong `ViStrings` (quota cạn, miền chưa consent, nội dung bị từ chối, provider lỗi…), mỗi lỗi có hành động gợi ý (mở cài đặt consent, xem hạn mức, thử lại). Không lộ mã kỹ thuật.
5. Offline: tính năng AI xám đi với thông báo cần mạng; không xếp hàng tin nhắn chờ mạng (không outbox).
6. Cache hội thoại chỉ trong bộ nhớ phiên. Muốn cache bền (đọc lại offline) thì ghi `[PROPOSED]` dùng kho `local_extras`, chờ duyệt — không tự làm.

## AWP-3 — Minh bạch ngữ cảnh (context disclosure)

1. Mỗi tin nhắn của AI có nút "AI đã đọc gì?" → `GET /ai/messages/{id}/context`: hiển thị miền, danh sách bản ghi và trường đã gửi (payload sau redaction), đúng D-context trong Phase 8.
2. Hiển thị trung thực những gì endpoint trả; không tự suy diễn thêm.

## AWP-4 — Planning và insight (read-only)

1. "Lập kế hoạch hôm nay/tuần" (`POST /ai/plans/daily|weekly`), "Chia nhỏ việc" từ một task (`POST /ai/tasks/{id}/breakdown`), insight Thói quen và Tài chính (`POST /ai/insights/...`), tóm tắt ghi chú (`POST /ai/notes/{id}/summary`).
2. Kết quả chỉ hiển thị — kèm nhãn rõ là gợi ý. Nếu muốn nút "tạo task từ gợi ý" thì đó là **người dùng tự tạo qua luồng task thường (local + outbox như mọi task tay)**, không phải AI tạo; được phép làm và ghi chú rõ trong báo cáo. Không gọi 4 path action-proposal.
3. Các lối vào đặt đúng ngữ cảnh: breakdown trong chi tiết task, insight trong tab Thói quen/Tài chính, tóm tắt trong NoteEditorSheet — chỉ hiện khi miền tương ứng đã consent.

## AWP-5 — Hạn mức và dữ liệu AI

1. Màn "Mức dùng AI": `GET /ai/usage` + `GET /ai/limits` — đã dùng/còn lại, cảnh báo gần hạn mức; tiền hiển thị đúng đơn vị hợp đồng (micro-USD → hiển thị gọn), không `double` cho tính toán.
2. Trong Cài đặt: mục "Dữ liệu AI" với nút "Xóa toàn bộ dữ liệu AI trên server" → `DELETE /ai/data`, xác nhận kiểu nguy hiểm (nhập chữ), nói rõ xóa trên server và không ảnh hưởng dữ liệu LifeOS khác.

## AWP-6 — Kiểm thử và bằng chứng

1. Widget/unit test: consent gating (miền tắt thì lối vào ẩn/khóa), ánh xạ 15 mã lỗi, trạng thái chờ 30 s và lỗi quá hạn, chặn gửi chồng, parse đúng 8 fixture `ai-*.json` làm hợp đồng vàng.
2. Golden: màn consent, màn chat (có tin nhắn + trạng thái chờ), màn mức dùng — Light/Dark, lưu `docs/review/mobile_screenshots/ai_chat/`.
3. E2E: thêm luồng AI vào bài E2E backend thật **chỉ khi** backend Docker bật được `FakeLLMProvider` qua cấu hình có sẵn (đọc `DEPLOY_AI_PROGRESS.md`/`application-*.yml` để biết; không sửa backend). Không bật được thì ghi `CHƯA CHẠY` kèm lý do và lệnh cần chạy — không giả lập rồi gọi là thật.
4. Full suite `flutter test --no-pub -j 1` + `dart analyze` dán nguyên văn; golden 2 lượt không lệch; E2E cũ (không AI) phải vẫn xanh.
5. Báo cáo: bảng AWP-1..6 ba nhãn; danh sách file; xác nhận không file vùng cấm nào bị sửa; số đo benchmark 50k chạy lẻ nếu tầng DB bị chạm (theo điều kiện D-M08).
