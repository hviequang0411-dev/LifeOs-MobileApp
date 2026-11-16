# LifeOS — Bộ tài liệu các phase sản phẩm (Phase 2–7)

> **Trạng thái:** v1 — **ĐÃ DUYỆT 2026-11-07** cùng `docs/engineering/BACKEND_MASTER_PLAN.md` (D100–D259 `[CONFIRMED]`; quyết định mặc định cho mọi `[OPEN]` ở §4 của kế hoạch đó; các thay đổi hợp đồng ở §4 bên dưới đều được cho phép). Claude Code làm theo kế hoạch tổng thể.

## 1. Hai hệ đánh số — đừng nhầm

| Hệ | Ý nghĩa | Nguồn |
|---|---|---|
| **Phase sản phẩm 1–8** | Lộ trình tính năng | `PRODUCT_CONTEXT.md` §12 |
| **Giai đoạn backend 0–4…** (Stage/Giai đoạn) | Các bước triển khai kỹ thuật đang chạy với Claude Code | `docs/review/CLAUDE_CODE_PHASE*_PROMPT.md` |

Đối chiếu hiện tại: giai đoạn backend 1 (Identity) và 2 (Project/Category/Task) đã duyệt = **Phase sản phẩm 1**; giai đoạn backend 3 (Sync) là hạ tầng của Phase 1 sản phẩm, đang chờ duyệt D93–D96. Phase 3 sản phẩm (Finance) **không** phải giai đoạn backend 3.

## 2. Danh sách tài liệu

| Tài liệu | Phase sản phẩm | Dải quyết định | Trạng thái |
|---|---|---|---|
| `PHASE_2_HABIT_ROUTINE.md` | 2 — Habit & Routine (cuối MVP) | D230–D259 | `[CONFIRMED]` — viết thêm vì backend làm hết trước và Phase 4, 6, 7 dùng định nghĩa chuỗi/tỷ lệ của nó |
| `PHASE_3_FINANCE.md` | 3 — Personal Finance | D100–D119 | `[CONFIRMED]` — do tác giả bổ sung (yêu cầu gốc bắt đầu từ Phase 4 nhưng chưa có tài liệu Finance nào) |
| `PHASE_4_CALENDAR_NOTIFICATION.md` | 4 — Calendar & Notification | D120–D149 | `[CONFIRMED]` |
| `PHASE_5_SYNC_ADVANCED.md` | 5 — Offline Sync nâng cao | D150–D179 | `[CONFIRMED]` |
| `PHASE_6_KNOWLEDGE_GOALS.md` | 6 — Knowledge & Goals | D180–D189 | `[CONFIRMED]` |
| `PHASE_7_INTELLIGENCE_ANALYTICS.md` | 7 — Life Intelligence & Analytics | D190–D219 | `[CONFIRMED]` |
| (hoãn) | 8 — AI | D260+ dành riêng | Thêm sau theo yêu cầu |

## 3. Thứ tự và phụ thuộc

```
Phase 1 (Foundation & Task, gồm sync cơ bản)
   ├── Phase 2 Habit & Routine (kết thúc MVP; 4/6/7 dùng định nghĩa của nó)
   ├── Phase 3 Finance ──────────┐
   ├── Phase 4 Calendar & Noti ──┤  (4 nhận sự kiện từ 3; 5 phủ mọi entity của 3 và 4)
   ├── Phase 5 Sync nâng cao ◄───┘  (khuyến nghị làm sau 3 và 4, trước hoặc song song 6)
   ├── Phase 6 Knowledge & Goals    (dùng hợp nhất văn bản của 5; liên kết Task)
   └── Phase 7 Analytics            (cuối: đọc từ mọi module qua *Api)
```

Có thể đổi thứ tự 3↔4; không nên làm 7 trước 3/4/6 (dashboard sẽ rỗng) và không nên phát hành Finance (3) cho người dùng thật trước khi Phase 5 có màn hình xử lý xung đột (D105: Finance luôn `CONFLICT`).

## 4. Thay đổi hợp đồng đã chốt (đã được duyệt trong `BACKEND_MASTER_PLAN.md` §5)

Các mục này **sửa tài liệu đã `[CONFIRMED]`** nên cần một câu duyệt riêng và cập nhật tài liệu gốc trước khi code:

| # | Thay đổi | Phase | Tài liệu bị ảnh hưởng |
|---|---|---|---|
| 1 | Thêm `quiet_start`, `quiet_end`, `show_details_on_lock_screen` vào `user_settings` | 4 | `SYNC_PROTOCOL.md` §3 (payload `user_settings`), V-migration `user_settings` |
| 2 | Thêm khóa `goalId`, `milestoneId` vào entity `task` | 6 | `SYNC_PROTOCOL.md` §3, `API_CONVENTIONS`, OpenAPI `Task` |
| 3 | Giới hạn payload riêng cho `note` 192 KB | 6 | `SYNC_PROTOCOL.md` §8 (D81–D92 liên quan limits) |
| 4 | Retention `change_logs`/`sync_mutations`/tombstone và mã `410 SYNC_FULL_RESYNC_REQUIRED` | 5 | `SYNC_RULES.md`, `SYNC_PROTOCOL.md` §6, `SYSTEM_ARCHITECTURE.md` §3.12 (xóa `[OPEN]`) |
| 5 | Chuyển `OutboxEventHandler` email từ `identity` sang module `notification` | 4 | `SYSTEM_ARCHITECTURE.md` (đã ghi chú sẵn), ArchUnit |
| 6 | Mở rộng bảng chiến lược xung đột theo entity (Finance luôn hỏi) | 3, 5 | `SYNC_RULES.md` §9.5 |
| 7 | Extension PostgreSQL `unaccent` | 6 | `DATABASE_RULES.md` |
| 8 | Quy ước chung cho tiền (`NUMERIC(19,4)`, JSON chuỗi, SQLite ×10⁴) | 3 | `DATABASE_RULES.md`, `API_CONVENTIONS.md`, `FLUTTER_RULES.md` |
| 9 | Thêm lý do `ALREADY_EXISTS` cho `CONFLICT` của `CREATE` (check-in/lượt chạy trùng ngày giữa hai thiết bị) | 2 | `SYNC_PROTOCOL.md` §4 (ma trận kết quả) |
| 10 | Đăng nhập nhận `deviceName`/`platform` tùy chọn, tạo bản ghi `devices` | 4 (hoặc 5) | `API_CONVENTIONS.md`, `SECURITY_RULES.md` (họ refresh token), OpenAPI |

## 5. Mục xuyên suốt cần quyết định sớm

1. **Xuất/nhập dữ liệu và xóa tài khoản**: đặc tả ở Phase 7 (D204–D205), nhưng mọi bảng mới ở Phase 3–6 phải đăng ký vào danh sách xuất/xóa ngay khi tạo (INT-15 sẽ bắt thiếu). Nếu người dùng thật xuất hiện trước Phase 7, cần triển khai tối thiểu xóa tài khoản sớm (khuyến nghị: kéo D205 lên trước phát hành).
2. **Số migration `V…` trong các tài liệu phase chỉ là minh họa:** dùng số kế tiếp tại thời điểm làm. Thứ tự dự kiến theo backend-first: Giai đoạn 4 (outbox) → Phase 2 → 3 → 4 → 5 → 6 → 7.
3. **Bảng `devices`:** `PRODUCT_CONTEXT` §19.2 đã chốt `identity` sở hữu `devices`, `notification` sở hữu `device_push_tokens` (Phase 4 tạo `devices` nếu chưa có; Phase 5 dùng lại).
4. **Mã hóa dữ liệu nhạy cảm (Finance, Notes)**: đang hoãn cùng quyết định at-rest chung; trước khi có người dùng thật cần quyết định rõ ràng.
5. **Dung lượng và giới hạn người dùng**: các phase không đặt hạn mức số bản ghi/user; cần chính sách trước khi mở đăng ký công khai.
6. **Nhà cung cấp hạ tầng ngoài**: FCM, email production (SMTP/SES), kho tệp xuất, Redis.

## 6. Quy trình dùng bộ tài liệu này

1. Người dùng đọc từng phase, trả lời các mục `[OPEN]` và duyệt dải D tương ứng (một câu duyệt rõ ràng cho mỗi phase hoặc từng mục).
2. Tác giả chuyển nhãn sang `[CONFIRMED]` **chỉ sau** câu duyệt, cập nhật các tài liệu gốc ở §4 nếu liên quan, bổ sung `AGENTS.md`.
3. Viết prompt triển khai cho Claude Code theo mẫu `CLAUDE_CODE_PHASE3_PROMPT.md` (bước 0 kiểm tra tài liệu trên đĩa, danh sách đọc, kế hoạch trước khi code, báo cáo sau từng bước).
4. Mỗi phase có thể được đưa cho GPT review bằng `GPT_REVIEW_PROMPT_*` (nhớ "Bước 0" chứng minh đã đọc).
5. Bất kỳ quyết định mới phát sinh trong lúc triển khai: ghi `[CONFIRMED]`, chờ duyệt, không tự chốt.

## 7. Quy ước chung các phase này tuân theo (tóm tắt, không thay thế tài liệu gốc)

UUID do client tạo; cột chuẩn `version/deleted_at/created_at/updated_at`; chỉ xóa mềm; partial unique `WHERE deleted_at IS NULL`; mọi ghi qua `ChangeRecorder` đặt **cuối** transaction (D86); cascade server-side bằng sự kiện miền (D80) với `mutation_id` NULL; module chỉ phụ thuộc nhau qua `api/`; kiểm tra IDOR trên mọi route (404 cho dữ liệu của người khác); không dữ liệu nhạy cảm trong log/metric/push/email; test dùng PostgreSQL thật, test đồng thời lặp ≥ 20 lần, không `Thread.sleep`.
