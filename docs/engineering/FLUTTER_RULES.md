# LifeOS — Flutter & SQLite Rules

> **Trạng thái:** v1 — tách từ `CODE_GUIDELINES.md`. **Giữ nguyên số mục** của Engineering Guidelines v1 để các tham chiếu `§x` vẫn đúng.
> **Phạm vi:** Kiến trúc client, dữ liệu local, outbox phía client (§12).
> Quy ước nhãn `[CONFIRMED]` / `[CONFIRMED]` / `[OPEN]` và quy trình review: xem `CODE_GUIDELINES.md` §0 và §16. Agent chỉ áp dụng `[CONFIRMED]`.

---

## 12. Flutter và SQLite

### 12.1. Kiến trúc `[CONFIRMED]`

```text
UI (Widget)
  → State layer (Notifier/Controller)
    → Repository (interface)
        ├── LocalDataSource  (SQLite, nguồn đọc duy nhất của UI)
        └── RemoteDataSource (HTTP; chỉ SyncEngine & luồng không-offline gọi)
SyncEngine (nền): đọc Local Outbox → push; pull → ghi vào SQLite
```

- **Quản lý state:** `[CONFIRMED]` (D9) Riverpod (`flutter_riverpod`, chưa dùng code generation).
- **SQLite access:** `[CONFIRMED]` (D10) Drift: truy vấn có kiểu, migration có phiên bản, stream reactive, transaction.

### 12.2. Quy tắc dữ liệu local

1. `[CONFIRMED]` Core CRUD hoạt động không cần Internet; dữ liệu lưu local; thay đổi offline vào hàng đợi.
2. `[CONFIRMED]` **UI chỉ đọc từ SQLite** (qua stream/query), không đọc trực tiếp từ response API. Pull ghi vào SQLite → UI tự cập nhật.
3. `[CONFIRMED]` Mỗi thao tác ghi: *một local transaction* ghi bản ghi vào bảng chính **và** thêm mutation vào `outbox` (kèm `mutationId` UUID, `entityType`, `entityId`, `operation`, `baseVersion`, `payload`). Không bao giờ ghi một mà không ghi kia.
4. `[CONFIRMED]` Bảng local của entity đồng bộ có: `id (UUID)`, `version` (từ server), `deleted_at`, `sync_state` (`synced | pending | conflict`). `sync_state` chỉ tồn tại ở SQLite client, **không** có ở PostgreSQL. Xóa local = đánh dấu tombstone, không `DELETE` cho đến khi server xác nhận.
5. `[CONFIRMED]` Client không dùng timestamp để quyết định bản mới hơn; `baseVersion` lấy từ bản server đã biết gần nhất.
6. `[CONFIRMED]` Sync engine: retry với backoff, chạy lại được an toàn (idempotent nhờ `mutationId`), cập nhật `sync_state`, hiển thị Sync Status cho người dùng.
7. `[CONFIRMED]` Xử lý `CONFLICT`: đánh dấu `conflict`, giữ bản local, hiển thị UI cho người dùng chọn (Task/Goal/Calendar); Notes tạo conflict copy; Finance không tự ghi đè.
8. Thời gian: `[CONFIRMED]` lưu UTC (ISO-8601); ngày nghiệp vụ lưu dạng `yyyy-MM-dd` theo timezone của user. Chi tiết chính sách timezone ở §8.1.
9. `[CONFIRMED]` Local notification dùng cho nhắc việc offline; Flutter cập nhật lịch local khi dữ liệu thay đổi; backend vẫn là nơi quản lý lịch reminder trung tâm.
10. `[CONFIRMED]` Token lưu bằng secure storage; không log dữ liệu cá nhân; không lưu secret của AI/provider trên client.
11. `[CONFIRMED]` **Tiền** (PHASE_3 D100, BACKEND_MASTER_PLAN §5 #8): SQLite lưu `INTEGER` = giá trị × 10⁴ (ví dụ `125000` VND → `1250000000`; `19.99` USD → `199900`), an toàn trong int64 với giới hạn `< 10^13`. JSON với server luôn là **chuỗi** thập phân (`"125000"`, `"19.99"`); chuyển đổi chuỗi ↔ số nguyên ×10⁴ bằng phép toán thập phân, **cấm `double`** cho tiền ở mọi tầng. Số chữ số thập phân nhập vào không vượt mức của tiền tệ (VND 0, USD 2…). Số dư và mức dùng ngân sách tính local bằng cùng công thức với server (D102, D109); giao dịch định kỳ chỉ do server sinh (đến qua pull).
12. `[OPEN]` Mã hóa SQLite at rest, hành vi xóa tài khoản khi offline (baseline §16).
