# LifeOS — Offline Sync Rules

> **Trạng thái:** v1 — tách từ `CODE_GUIDELINES.md`. **Giữ nguyên số mục** của Engineering Guidelines v1 để các tham chiếu `§x` vẫn đúng.
> **Phạm vi:** Sync, idempotency, optimistic locking, conflict (§9). Đặc tả cài đặt chi tiết (ma trận kết quả, payload, khóa): `SYNC_PROTOCOL.md`. Phía client xem thêm `FLUTTER_RULES.md`.
> Quy ước nhãn `[CONFIRMED]` / `[CONFIRMED]` / `[OPEN]` và quy trình review: xem `CODE_GUIDELINES.md` §0 và §16. Agent chỉ áp dụng `[CONFIRMED]`.

---

## 9. Offline Sync, Idempotency, Optimistic Locking

### 9.1. Nguyên tắc `[CONFIRMED]`

- Server quản lý `version` và `cursor`. Client không dùng timestamp tự khai báo để xác định bản mới hơn.
- Mutation có `mutationId` duy nhất → idempotent.
- `version` dùng để phát hiện conflict; xóa bằng tombstone.
- Không Last Write Wins cho mọi entity.

### 9.2. Push — thứ tự xử lý mỗi mutation `[CONFIRMED]` (theo baseline §9.2)

```text
1. Authenticate user
2. Authorize entity ownership
3. Check idempotency (mutationId đã xử lý? → trả lại kết quả cũ)
4. Validate baseVersion
5. Validate business rules
6. Execute mutation
7. Increase entity version
8. Lưu mutation result + change log (cùng transaction)
9. Return result
```

Quy tắc triển khai `[CONFIRMED]`:
- **Sync không được ghi thẳng vào repository.** Mỗi `entityType` có một `MutationHandler` gọi **Application Service** của module sở hữu để business rules chỉ nằm ở một chỗ.
- Idempotency: bảng `sync_mutations` có `UNIQUE (user_id, mutation_id)`. Mutation trùng trả lại đúng kết quả đã lưu, không thực thi lại.
- Xử lý **theo từng mutation**, mỗi mutation một transaction; kết quả trả về theo từng mutation (`APPLIED`, `DUPLICATE`, `CONFLICT`, `REJECTED` + `errorCode`). Một mutation lỗi không làm mất các mutation hợp lệ khác trong batch.
- Mutation phụ thuộc nhau (tạo project rồi tạo task trong project) được xử lý theo thứ tự client gửi; client SHOULD dừng đẩy các mutation phụ thuộc nếu mutation cha bị `REJECTED`/`CONFLICT`.
- Chỉ ghi `change_logs` khi mutation thật sự được áp dụng.

### 9.3. Pull `[CONFIRMED]` (theo baseline §9.3)

- Client gửi cursor; server trả `changes, operation, entityId, entityVersion, nextCursor, hasMore`.
- Cursor do server cấp, không dựa trên timestamp client.
- `[CONFIRMED]` **Cursor = bộ đếm `seq` theo từng user** (giải quyết D8). Lý do: sequence toàn cục cấp số lúc insert chứ không lúc commit, nên hai transaction commit lệch thứ tự có thể khiến client bỏ sót thay đổi. Bộ đếm theo user tránh điều đó.
  - `user_sync_state(user_id PK, last_seq BIGINT NOT NULL)`; tạo dòng **lười** ở lần ghi `change_logs` đầu tiên (D21; không cần bước khởi tạo lúc đăng ký).
  - Mỗi thay đổi của entity đồng bộ cấp seq bằng `UPDATE user_sync_state SET last_seq = last_seq + 1 WHERE user_id = ? RETURNING last_seq` **trong cùng transaction** với thay đổi nghiệp vụ. Row lock giữ đến commit nên ghi của một user tuần tự, seq liên tục và theo đúng thứ tự commit; rollback hoàn tác luôn phần tăng.
  - `change_logs(user_id, seq, entity_type, entity_id, operation, entity_version, mutation_id NULL, created_at)`, khóa chính `(user_id, seq)`.
  - Pull: `WHERE user_id = ? AND seq > :cursor ORDER BY seq LIMIT :n`; `nextCursor` = seq lớn nhất trả về; cursor ban đầu là `0`. Với client, cursor là chuỗi mờ (opaque).
  - `[CONFIRMED]` **Mọi** thay đổi entity đồng bộ, dù đến từ REST hay sync push, MUST đi qua một thành phần duy nhất (vd `ChangeRecorder`) để không có thay đổi nào thiếu dòng `change_logs`.
  - `[CONFIRMED]` Giữ transaction ghi ngắn vì row lock theo user kéo dài đến commit.
- `[CONFIRMED]` Kích thước lô Phase 1: push tối đa 50 mutation, pull mặc định 200 và tối đa 500 (`SYSTEM_ARCHITECTURE.md` §3.5, §3.7). `[OPEN]` hoãn đến Phase 5: retention của cursor/tombstone và full resync.

### 9.4. Optimistic locking `[CONFIRMED]` cơ chế / `[CONFIRMED]` chi tiết

- Mỗi entity đồng bộ có cột `version` (JPA `@Version` hoặc kiểm tra tường minh).
- Update thông thường qua REST cũng gửi `version` mong đợi; sai → `409 *_VERSION_CONFLICT`.
- Trong sync, `baseVersion != version hiện tại` → `CONFLICT`, trả kèm bản server để client xử lý.

### 9.5. Chiến lược conflict theo entity `[CONFIRMED]` (baseline §9.4)

| Entity | Chiến lược |
|---|---|
| Task | Optimistic locking, user-assisted conflict resolution |
| Habit Check-in | Idempotent upsert + unique constraint (`habit_id`, ngày) |
| Notes | Tạo *conflict copy*, không mất nội dung |
| Calendar Event | Optimistic locking + kiểm tra overlap |
| Goal | Optimistic locking |
| Finance Transaction | Không tự động ghi đè; xử lý conflict an toàn, cần user quyết định |
| Mọi entity Finance (Phase 3: `financial_account`, `financial_category`, `financial_transaction`, `account_transfer`, `budget`, `recurring_transaction`, `saving_goal`, `saving_contribution`) `[CONFIRMED]` D105 | Luôn `CONFLICT` khi `baseVersion` lệch, kèm `serverSnapshot` (số tiền là chuỗi); **không** Last-Write-Wins, **không** hợp nhất theo trường; người dùng luôn chọn (UI ở Phase 5). Giao dịch do job định kỳ sinh đến client qua pull dạng `CREATE` (client không tự sinh) |
| Task, Project, Category, Calendar, Reminder (PHASE_5 D158) `[CONFIRMED]` | Hợp nhất 3 chiều ở client theo trường (base/local/server); cùng trường đổi khác giá trị → hỏi; nhóm trường thời gian (Calendar) và `mode/offset/local_*` (Reminder) hỏi cả bộ. Server không đổi (vẫn CONFLICT + snapshot) |
| `user_settings` (PHASE_5 D158, §4 kế hoạch) `[CONFIRMED]` | Việc của client (theo khóa; cùng khóa → bản `updatedAt` mới hơn thắng, có thông báo); server không đổi |
| Habit check-in, lượt chạy/kết quả bước routine (Phase 2) `[CONFIRMED]` D235 | `CREATE` trùng khóa tự nhiên → `CONFLICT` `conflictReason: ALREADY_EXISTS` kèm bản server; client nhận bản server |
| Phase 6: `note`, `tag`, `goal`, `milestone` (PHASE_6 §7, D183; triển khai WP6) `[CONFIRMED]` | Lệch `baseVersion` → `CONFLICT` + bản server. Client hợp nhất 3 chiều theo trường; `note.content` cùng bị sửa → **luôn hỏi**, hiển thị hai bản cạnh nhau (không hợp nhất theo dòng). `tag` `CREATE` trùng tên (không phân biệt hoa thường) → `CONFLICT ALREADY_EXISTS` kèm thẻ đang có (client gộp thẻ) |
| `note_tag`, `note_link` (PHASE_6 D184, D189) `[CONFIRMED]` | Chỉ CREATE/DELETE (UPDATE → `SYNC_OPERATION_NOT_ALLOWED`); hợp nhất ở mức liên kết — thêm của hai thiết bị đều giữ; `CREATE` trùng cặp đang sống → `APPLIED` với version bản đang có (client gộp theo cặp) |
| `review` (PHASE_7 D202; triển khai WP7) `[CONFIRMED]` | `CREATE` trùng kỳ → `CONFLICT ALREADY_EXISTS`; sửa `reflection` lệch version → `CONFLICT` văn bản như `note` |

Ghi chú: mỗi entity mới đồng bộ ở phase sau (Habit, Note, Calendar…) phải định nghĩa **cách người dùng xử lý conflict trên UI** trước khi được thêm vào `SyncEntityHandler`; Phase 1 chỉ có hành vi ở §9.7.

### 9.6. Idempotency ngoài sync `[CONFIRMED]`

- Các thao tác tạo có tác động tài chính hoặc có thể bị retry (tạo transaction, recurring generation) MUST an toàn khi retry: dùng khóa tự nhiên/unique constraint hoặc `Idempotency-Key`.
- Recurring Transaction `[CONFIRMED]` phải tránh tạo trùng khi retry (vd unique `(recurring_id, occurrence_date)`).
- Xóa luôn là soft delete với entity đồng bộ; xóa vật lý chỉ bởi job dọn tombstone sau thời hạn retention `[CONFIRMED]` (BACKEND_MASTER_PLAN §4–§5 #4, PHASE_5 D153–D156, triển khai WP5):
  - `change_logs`: giữ 180 ngày **và** tối thiểu 1000 seq gần nhất mỗi user; xóa theo lô 5000; `user_sync_state.min_retained_seq` = seq lớn nhất đã xóa, cập nhật trong cùng transaction (khóa `user_sync_state`). Pull với cursor < `min_retained_seq` → `410 SYNC_FULL_RESYNC_REQUIRED` `{minRetainedSeq, currentSeq}`; client chạy full resync qua `POST /sync/snapshot` (D151).
  - `sync_mutations`: giữ 35 ngày; client đặt hạn 30 ngày cho mutation trong outbox. `CREATE` lặp sau khi đã dọn → `REJECTED ENTITY_ALREADY_EXISTS`, client đối chiếu dữ liệu (D155).
  - Tombstone: xóa cứng khi `deleted_at` quá 180 ngày, mọi `change_logs` của entity đã bị dọn, và không còn dòng con tham chiếu; con trước cha. Giao dịch Finance sinh từ định kỳ giữ tombstone khi dòng định kỳ còn tồn tại (khóa UNIQUE D110; không tạo bảng phụ). Không hồi sinh id cũ (D157).

### 9.7. Phạm vi sync ở Phase 1 `[CONFIRMED]`

- Phase 1 triển khai push/pull **tối thiểu nhưng đúng giao thức**: `mutationId`, `baseVersion`, cursor `seq`, tombstone, idempotency, local outbox.
- Chưa làm trong Phase 1: UI xử lý conflict, multi-device phức tạp, recovery/full resync nâng cao (Phase 5).
- `[CONFIRMED]` Khi gặp `CONFLICT` ở Phase 1: đánh dấu `sync_state = conflict`, giữ nguyên bản local, **không** ghi đè bản server hay bản local, hiển thị thông báo đơn giản cho người dùng. Không được mất dữ liệu. UI chọn bản khi conflict làm ở Phase 5.
