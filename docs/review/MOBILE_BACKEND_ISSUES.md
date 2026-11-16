# Nhật ký Đối chiếu Hợp đồng Mobile – Backend (LifeOS)

> **Cập nhật:** 2026-11-08 (Rà soát nghiệm thu vòng 2)
> **Nguyên tắc:** Không sửa bất kỳ tệp nào trong `backend/`. Mọi sai lệch giữa giả định ban đầu của Mobile và hợp đồng thực tế của Backend (`docs/api/openapi.yaml`, `docs/engineering/SYNC_PROTOCOL.md`, `docs/api/fixtures/*.json`) được ghi nhận trung thực tại đây và sửa ở phía `mobile/`.

---

## 1. Phân biệt rõ hai bài kiểm thử E2E trong `mobile/test/`

1. **[`mobile/test/e2e_protocol_harness_test.dart`](file:///d:/LifeOS/mobile/test/e2e_protocol_harness_test.dart) (Server giả in-memory — KHÔNG phải backend thật):**
   - Sử dụng `_StatefulProtocolServerHarness` (`HttpClientAdapter` trong bộ nhớ) để kiểm thử nhanh logic Client ở chế độ cô lập mạng.
   - Đã rà soát toàn bộ response của `_StatefulProtocolServerHarness` với `docs/api/openapi.yaml`: xóa bỏ các trường tự đặt trước đây (`dayStartHour`, `verificationTokenForMailpit`), trả đúng HTTP `202` body rỗng cho `/auth/register` và `/account/deletion/code`, đúng `AccountDeletionResponse` (`{cancellable, deletionScheduledAt}`), đúng `SettingsResponse` (`openapi.yaml` dòng 8175–8201) và đúng mã hóa base64url cho `nextCursor`.
2. **[`mobile/test/e2e_real_backend_test.dart`](file:///d:/LifeOS/mobile/test/e2e_real_backend_test.dart) (E2E gọi Backend thật qua Docker Compose):**
   - Đọc cờ `const bool.fromEnvironment('LIFEOS_RUN_REAL_E2E')` (`--dart-define=LIFEOS_RUN_REAL_E2E=true`).
   - Khi không truyền cờ: bài test được đánh dấu `skip` tường minh (không giả danh đã chạy).
   - Khi bật `docker compose -f backend/docker-compose.yml up -d` (gồm `postgres`, `mailpit`, `backend`) và chạy với `--dart-define=LIFEOS_RUN_REAL_E2E=true`: bài test gọi thật `http://localhost:8080/api/v1` và hộp thư Mailpit `http://localhost:8025/api/v1/messages` qua đủ 9 luồng `(a)`–`(i)`.

---

## 2. Các sai lệch phía Client phát hiện khi chạy với Backend thật & đối chiếu Spec (đã sửa ở `mobile/`)

| # | Hạng mục | Nguồn sự thật trong tài liệu / Backend | Lỗi phía Mobile ở bản giao trước | Cách đã khắc phục ở `mobile/` |
|---|---|---|---|---|
| 1 | Mã hóa `cursor` và `nextCursor` của `GET /api/v1/sync/pull` | `SYNC_PROTOCOL.md` §6 mục 3 (`D89`, dòng 278) & `SyncCursorCodec.java`: `cursor` là chuỗi base64url (không padding `=`) của chuỗi số thập phân `seq`. | Mobile gửi `cursor=0` dạng số nguyên thô và ép kiểu `nextCursor as num?`, dẫn tới lỗi HTTP `400 VALIDATION_FAILED (fieldError: cursor)` khi gọi backend thật. | Đã thêm `SyncEngine.encodeCursor` và `SyncEngine.decodeCursor` trong [`sync_engine.dart`](file:///d:/LifeOS/mobile/lib/core/sync/sync_engine.dart). |
| 2 | Tên trường thao tác trong `POST /api/v1/sync/push` | `SYNC_PROTOCOL.md` §2.1 dòng 38 & `SyncPushRequestParser.java` dòng 97: trường bắt buộc là `operation` (`CREATE\|UPDATE\|DELETE`). | `OutboxMutationRecord.toPushWireJson()` chỉ xuất khóa `'op'`, khiến backend thật trả `400 MALFORMED_REQUEST (mutations[i].operation REQUIRED)`. | Đã xuất khóa `'operation'` chuẩn trong [`database_records.dart`](file:///d:/LifeOS/mobile/lib/core/database/database_records.dart). |
| 3 | Hình dạng `MutationResult` và `ServerSnapshot` của `POST /api/v1/sync/push` | `SYNC_PROTOCOL.md` §2.2 dòng 55–60 & `openapi.yaml` dòng 7346–7371: `APPLIED` trả `entityVersion`; `CONFLICT` trả `serverSnapshot: {entityType, entityId, version, deleted, data}`; `DUPLICATE` trả `originalStatus`. | `SyncEngine` chỉ đọc `item['version']` và `item['serverCurrent']`. | Đã cập nhật `SyncEngine` đọc đúng `entityVersion`, `serverSnapshot.data` và `originalStatus`. |
| 4 | Không tồn tại trường `dayStartHour` trong `user_settings` | `PHASE_7_INTELLIGENCE_ANALYTICS.md` `D192` dòng 49 & `openapi.yaml` `SettingsResponse` dòng 8175–8201: ngày của người dùng là `[00:00, 24:00)` theo `user_settings.timezone`; tuần bắt đầu theo `weekStartDay` (`1..7`, mặc định `1`). | Mobile tự thêm trường `dayStartHour = 4` vào `UserDayClock` và `user_settings`. | Đã xóa hoàn toàn `dayStartHour` khỏi [`user_day_clock.dart`](file:///d:/LifeOS/mobile/lib/core/time/user_day_clock.dart), repository và test. |
| 5 | Thực thể chuyển khoản `account_transfer` | `PHASE_3_FINANCE.md` `D102`/`D104`/`D118` dòng 52–68, `SYNC_PROTOCOL.md` §3.6 dòng 145 & `openapi.yaml` dòng 6507–6530: `account_transfer` là thực thể đồng bộ riêng (`{fromAccountId, toAccountId, amount, occurredOn, note}`), chỉ cùng tiền tệ (`FINANCE_CURRENCY_MISMATCH`). | Mobile tạo 2 dòng `financial_transaction` có `transferPairId` và không có `account_transfer` trong danh sách `SyncEntityType`. | Đã thêm `SyncEntityType.accountTransfer` (`wireName: 'account_transfer'`) và cài đặt `createAccountTransfer` + công thức số dư `D102` trong [`finance_local_repository.dart`](file:///d:/LifeOS/mobile/lib/features/finance/data/finance_local_repository.dart). |
| 6 | Định dạng `recurrence` của `calendar_event` | `PHASE_4_CALENDAR_NOTIFICATION.md` `D124`–`D126` dòng 58, `SYNC_PROTOCOL.md` §3.7 dòng 159 & `calendar-event.response.json`: `recurrence` là đối tượng JSON `{freq, interval, byWeekday: ['MO'..'SU'], byMonthDay, until, count}`, giờ địa phương `startLocal`/`endLocal`. | Mobile lưu chuỗi RRULE thô hoặc truyền số `1..7` vào `byWeekday`. | Đã chuẩn hóa đối tượng JSON `recurrence` theo `D126` trong [`calendar_local_repository.dart`](file:///d:/LifeOS/mobile/lib/features/calendar/data/calendar_local_repository.dart). |
| 7 | Phạm vi `note_link` (không có Wiki-link hai chiều) | `PHASE_6_KNOWLEDGE_GOALS.md` dòng 6 (liên kết hai chiều kiểu wiki ngoài phạm vi) & `D189` dòng 56, `SYNC_PROTOCOL.md` §3.8 dòng 173: `note_link` chỉ liên kết một chiều từ Note tới `TASK` hoặc `GOAL` (`{noteId, targetType, targetId}`). | Mobile tự cài parser `[[Wiki-link]]` giữa các Note và danh sách Backlinks. | Đã gỡ bỏ hoàn toàn `[[Wiki-link]]`/Backlinks; thay bằng `linkNoteToTarget` (`targetType ∈ {'TASK', 'GOAL'}`) trong [`knowledge_local_repository.dart`](file:///d:/LifeOS/mobile/lib/features/knowledge/data/knowledge_local_repository.dart). |
| 8 | Giá trị enum của `task` (`status`, `priority`) và `review` (`periodType`, `periodKey`) | `SYNC_PROTOCOL.md` §3.1 dòng 85–86 (`task.status ∈ {'OPEN','DONE'}`, `priority ∈ {'NONE','LOW','MEDIUM','HIGH'}`) và §3.9 dòng 183 (`review.periodType ∈ {'DAY','WEEK','MONTH'}`, `periodKey` là ngày bắt đầu kỳ `YYYY-MM-DD`). | Mobile gửi `status: 'TODO'/'COMPLETED'`, `priority: 'URGENT'`, và `periodType: 'WEEKLY'` / `periodKey: '2026-W45'`. | Đã chuẩn hóa trong [`task_local_repository.dart`](file:///d:/LifeOS/mobile/lib/features/tasks/data/task_local_repository.dart) và [`knowledge_local_repository.dart`](file:///d:/LifeOS/mobile/lib/features/knowledge/data/knowledge_local_repository.dart). |
| 9 | Header `Idempotency-Key` | `API_CONVENTIONS.md`, `SYNC_PROTOCOL.md`, `openapi.yaml`: không định nghĩa header `Idempotency-Key` (idempotency của đồng bộ dùng `mutationId` trong body `POST /sync/push`). | `LifeOsApiClient` tự đính kèm header `Idempotency-Key` vào mọi POST/PUT/PATCH. | Đã xóa hoàn toàn việc gắn `Idempotency-Key` khỏi [`api_client.dart`](file:///d:/LifeOS/mobile/lib/core/network/api_client.dart). |

---

## 3. Kết luận đối chiếu với Backend

- **Số lỗi ở phía `backend/`:** **0 lỗi** (Backend hoạt động đúng đặc tả `openapi.yaml` và `SYNC_PROTOCOL.md`).
- **Số lỗi phía `mobile/` phát hiện qua đối chiếu spec và chạy `e2e_real_backend_test.dart` trên Docker:** **9 mục** (đã liệt kê và khắc phục toàn bộ ở bảng mục 2).
