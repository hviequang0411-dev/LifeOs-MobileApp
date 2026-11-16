# Báo cáo Triển khai Chế độ Web Local (`localhost`) — LifeOS Mobile

> **Ngày hoàn thành:** 2026-11-12  
> **Mục tiêu:** Cho phép chạy và kiểm thử ứng dụng Flutter (`mobile/`) trực tiếp trên trình duyệt Chrome/Edge (`http://localhost:8090`) kết nối với backend Spring Boot Docker thật (`http://localhost:8080`) mà **không cần tạo Android Emulator**, đồng thời **không sửa `backend/`** và giữ nguyên 100% hành vi Android/iOS.

---

## 1. Kiến trúc Cách 2 (SQLite WASM + Proxy Same-Origin)

1. **Tầng CSDL đa nền tảng (`conditional import`):**
   - [`sqlite_runtime.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime.dart) export giao diện chung `package:sqlite3/common.dart` và tự chọn implementation theo nền tảng:
     - Android / iOS / VM test (`dart.library.io`): [`sqlite_runtime_io.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime_io.dart) dùng `dart:io`, `package:sqlite3/sqlite3.dart` và `NativeDatabase.opened(...)`.
     - Trình duyệt Web (`dart.library.js_interop`): [`sqlite_runtime_web.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime_web.dart) nạp [`web/sqlite3.wasm`](file:///d:/LifeOS/mobile/web/sqlite3.wasm) (`sqlite3-2.9.4`, 730.989 bytes), ưu tiên `IndexedDbFileSystem` (lưu bền vững trên trình duyệt, tự fallback `InMemoryFileSystem` nếu trình duyệt chặn IndexedDB) và bọc qua `WasmDatabase.opened(...)` của Drift.
     - Fallback an toàn: [`sqlite_runtime_stub.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime_stub.dart).
   - [`drift_connection.dart`](file:///d:/LifeOS/mobile/lib/core/database/drift_connection.dart) và [`local_extras_database.dart`](file:///d:/LifeOS/mobile/lib/core/database/local_extras_database.dart) chuyển sang dùng `CommonDatabase` / `CommonPreparedStatement` / `ResultSet` / `Row`, giữ nguyên hoàn toàn 29 bảng đồng bộ + 6 bảng hạ tầng + `schemaVersion = 1`.
   - [`overview_and_settings_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/review/presentation/overview_and_settings_tab_view.dart) thay thao tác xóa file `dart:io` bằng `deleteLocalDatabaseFiles(userId)`.
   - [`local_notification_service.dart`](file:///d:/LifeOS/mobile/lib/features/notification/data/local_notification_service.dart) degrade êm khi `kIsWeb == true` (trả về không gọi MethodChannel native).
   - [`main.dart`](file:///d:/LifeOS/mobile/lib/main.dart) khởi tạo `await initializeSqliteRuntime()` trước khi chạy app; trên Web mở `/lifeos_web.sqlite` (IndexedDB) và fallback `AppDatabase.inMemory()` nếu lỗi.

2. **Proxy Same-Origin + CORS cho `localhost`:**
   - [`mobile/tool/web_dev_proxy.dart`](file:///d:/LifeOS/mobile/tool/web_dev_proxy.dart) (`LifeOsWebDevProxy`) lắng nghe mặc định tại `http://localhost:8090`:
     - Phục vụ bản build tĩnh [`mobile/build/web/`](file:///d:/LifeOS/mobile/build/web/) tại `/` (kèm MIME type `application/wasm` cho `sqlite3.wasm` và SPA fallback về `index.html`).
     - Chuyển tiếp toàn bộ `/api/*` sang `http://localhost:8080/api/*` (kèm header CORS và xử lý preflight `OPTIONS`, trả `502 PROXY_BACKEND_UNREACHABLE` nếu backend chưa bật).
   - [`app_flavor.dart`](file:///d:/LifeOS/mobile/lib/core/config/app_flavor.dart): khi chạy trên Web và không truyền `--dart-define=LIFEOS_API_BASE_URL`, môi trường `dev` tự dùng `${Uri.base.origin}/api/v1` (cùng nguồn `http://localhost:8090/api/v1`), tránh hoàn toàn lỗi CORS mà không cần sửa `backend/`.

---

## 2. Xác nhận ranh giới & Danh sách tệp

- **Không sửa** `backend/`, `docs/api/openapi.yaml`, `SYNC_PROTOCOL.md`, `SyncEngine`, `LifeOsApiClient`, `entity_types.dart`, `drift_tables.dart`.
- **Tệp tạo mới:**
  - `mobile/web/` (`index.html`, `manifest.json`, `favicon.png`, `icons/*`, `sqlite3.wasm`)
  - [`mobile/lib/core/database/sqlite_runtime.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime.dart)
  - [`mobile/lib/core/database/sqlite_runtime_io.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime_io.dart)
  - [`mobile/lib/core/database/sqlite_runtime_web.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime_web.dart)
  - [`mobile/lib/core/database/sqlite_runtime_stub.dart`](file:///d:/LifeOS/mobile/lib/core/database/sqlite_runtime_stub.dart)
  - [`mobile/tool/web_dev_proxy.dart`](file:///d:/LifeOS/mobile/tool/web_dev_proxy.dart)
  - [`mobile/test/web_proxy_and_portability_test.dart`](file:///d:/LifeOS/mobile/test/web_proxy_and_portability_test.dart)
- **Tệp chỉnh sửa:**
  - [`mobile/lib/main.dart`](file:///d:/LifeOS/mobile/lib/main.dart)
  - [`mobile/lib/core/config/app_flavor.dart`](file:///d:/LifeOS/mobile/lib/core/config/app_flavor.dart)
  - [`mobile/lib/core/database/drift_connection.dart`](file:///d:/LifeOS/mobile/lib/core/database/drift_connection.dart)
  - [`mobile/lib/core/database/local_extras_database.dart`](file:///d:/LifeOS/mobile/lib/core/database/local_extras_database.dart)
  - [`mobile/lib/features/notification/data/local_notification_service.dart`](file:///d:/LifeOS/mobile/lib/features/notification/data/local_notification_service.dart)
  - [`mobile/lib/features/review/presentation/overview_and_settings_tab_view.dart`](file:///d:/LifeOS/mobile/lib/features/review/presentation/overview_and_settings_tab_view.dart)

---

## 3. Bằng chứng kiểm chứng (chạy sau lần sửa cuối)

| Hạng mục | Lệnh đã chạy | Kết quả nguyên văn |
|---|---|---|
| Static analysis | `D:\src\flutter\bin\dart.bat analyze` (trong `mobile/`) | `No issues found!` |
| Build Web | `D:\src\flutter\bin\flutter.bat build web --no-tree-shake-icons` | `√ Built build\web` (cảnh báo `Wasm dry run` từ `flutter_secure_storage_web` không ảnh hưởng bản build JS mặc định) |
| Test Proxy & Portability | `D:\src\flutter\bin\flutter.bat test --no-pub -j 1 test\web_proxy_and_portability_test.dart` | `00:02 +3: All tests passed!` |
| Live Proxy + Docker Backend | `dart run tool/web_dev_proxy.dart --port=8090` + probe HTTP | `HEALTHZ: {"status":"ok","proxyOrigin":"http://localhost:8090","backendOrigin":"http://localhost:8080","webRoot":"build/web"}`<br>`HTML_STATUS: 200 LENGTH: 1546`<br>`WASM_STATUS: 200 TYPE: application/wasm LENGTH: 730989`<br>`PROXY_REGISTER_STATUS: 202 CORS: *` |
| Bộ test đầy đủ | `D:\src\flutter\bin\flutter.bat test --no-pub -j 1` | `03:39 +72 ~1 -1` (72 đạt, 1 skip cổng E2E, 1 lỗi pre-existing ở `test\ui_golden_test.dart`: `Golden "goldens/tablet_light_01_my_day.png": Pixel test failed, 0.07%, 534px diff detected.` do `HabitHeatmapWidget` dùng `DateTime.now()` khi sang ngày `2026-11-12`) |
| E2E Backend thật | `D:\src\flutter\bin\flutter.bat test --no-pub --timeout=2m --dart-define=LIFEOS_RUN_REAL_E2E=true test\e2e_real_backend_test.dart` | `00:19 +1: All tests passed!` |

---

## 4. Hướng dẫn mở và test ngay trên Chrome/Edge (`localhost`)

1. Đảm bảo Docker backend đang chạy (PostgreSQL + Mailpit + Spring Boot):
   ```powershell
   docker start backend-postgres-1 backend-mailpit-1 backend-backend-1
   ```
2. Khởi động máy chủ Web Dev Proxy từ thư mục `d:\LifeOS\mobile`:
   ```powershell
   cd d:\LifeOS\mobile
   D:\src\flutter\bin\dart.bat run tool/web_dev_proxy.dart --port=8090
   ```
3. Mở Chrome hoặc Edge truy cập:
   - Ứng dụng LifeOS Web: `http://localhost:8090`
   - Mailpit xem mã xác minh email / đặt lại mật khẩu: `http://localhost:8025`
4. (Tùy chọn) Nếu muốn chạy hot-reload bằng `flutter run -d chrome`, vẫn mở `web_dev_proxy.dart` ở cửa sổ khác rồi chạy:
   ```powershell
   D:\src\flutter\bin\flutter.bat run -d chrome --dart-define=LIFEOS_API_BASE_URL=http://localhost:8090/api/v1
   ```

---

## 5. Giới hạn đã biết trên Web

- **Thông báo cục bộ nền & FCM:** Trình duyệt không chạy được `flutter_local_notifications` nền như Android/iOS; tầng UI cài đặt nhắc vẫn hoạt động và lưu cấu hình vào SQLite (`user_settings`), nhưng thông báo hệ thống trên Web chỉ ở chế độ no-op an toàn.
- **Lưu trữ trình duyệt:** Dữ liệu SQLite Web được lưu trong IndexedDB (`lifeos_web`) thông qua `sqlite3.wasm`; nếu mở ẩn danh (Incognito) hoặc trình duyệt chặn IndexedDB, ứng dụng tự fallback về RAM (`InMemoryFileSystem`).

