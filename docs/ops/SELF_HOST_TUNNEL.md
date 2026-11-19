# Tự host LifeOS miễn phí: backend trên máy nhà + Cloudflare Quick Tunnel + APK tự ký

> Dùng cho **một người dùng cá nhân**, chi phí 0 đồng. Không phải cấu hình production — production xem `deploy/` và
> `docs/ops/DEPLOYMENT.md`. Lập 2026-11-19.

## Tổng quan

```text
Điện thoại (APK tự ký)  ──HTTPS──▶  https://<ngẫu nhiên>.trycloudflare.com
                                          │ Cloudflare
                                          ▼
Máy tính nhà: Docker ── cloudflared ──▶ backend:8080 ──▶ PostgreSQL
                                          └──▶ Mailpit (email xác minh, http://localhost:8025)
```

- Postgres, Mailpit và backend chỉ mở cổng trên `127.0.0.1` của máy tính; Internet chỉ vào được backend qua tunnel.
- App là offline-first: không có mạng hoặc máy tính tắt thì vẫn dùng bình thường, có mạng lại thì đồng bộ.
- **Giới hạn của Quick Tunnel:** địa chỉ `trycloudflare.com` **đổi mỗi lần** container `cloudflared` khởi động lại (khởi
  động lại máy hoặc Docker). Địa chỉ được gắn cứng vào APK lúc build, nên khi địa chỉ đổi phải **build và cài lại APK**
  (cài đè, dữ liệu trên máy vẫn giữ nếu ký cùng keystore). Cloudflare không cam kết uptime cho Quick Tunnel. Muốn địa chỉ
  cố định thì cần một domain trên Cloudflare và dùng named tunnel.

## Yêu cầu

- Docker Desktop đang chạy; Flutter SDK; JDK 17+ (có `keytool`).
- Điện thoại Android cho phép cài ứng dụng từ nguồn không xác định.

## 1. Chạy backend và tunnel

Từ thư mục `backend/`:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/tunnel-up.ps1
```

Script sẽ:

1. Lần đầu: sinh `JWT_SECRET`, `SNAPSHOT_HMAC_SECRET`, `NOTIFICATION_UNSUBSCRIBE_SECRET` ngẫu nhiên vào
   `backend/.env.tunnel` (đã nằm trong `.gitignore`). **Giữ file này**: đổi `JWT_SECRET` làm mọi phiên đăng nhập hết hiệu lực.
2. Chạy `docker-compose.yml` + `docker-compose.tunnel.yml` (postgres, mailpit, backend, cloudflared).
3. In địa chỉ công khai, ví dụ `https://abc-def-ghi.trycloudflare.com`, và lệnh build APK tương ứng.

Kiểm tra: mở `https://<địa chỉ>/actuator/health` trên điện thoại, phải thấy `"status":"UP"`.

## 2. Tạo keystore ký APK (làm một lần)

Keystore là "chữ ký" của app. **Mất keystore thì không cài đè được nữa**, phải gỡ app và mất dữ liệu chưa đồng bộ. Lưu nó
ngoài repo và sao lưu cẩn thận. Bạn tự đặt mật khẩu; không đưa mật khẩu vào chat hay commit.

```powershell
keytool -genkeypair -v -keystore "$env:USERPROFILE\lifeos-release.jks" -storetype JKS -keyalg RSA -keysize 2048 -validity 10000 -alias lifeos
```

Tạo file `mobile/android/key.properties` (đã nằm trong `.gitignore`):

```properties
storePassword=<mật khẩu keystore>
keyPassword=<mật khẩu khóa>
keyAlias=lifeos
storeFile=C:/Users/<tên>/lifeos-release.jks
```

Thiếu file này thì `flutter build apk --release` dừng với thông báo "Thiếu android/key.properties" — bản release không
bao giờ được ký bằng khóa debug (P-DWP7-03).

## 3. Build APK

Từ thư mục `mobile/`, dùng đúng địa chỉ script ở bước 1 in ra:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-apk.ps1 -ApiBaseUrl https://abc-def-ghi.trycloudflare.com/api/v1
```

Kết quả: `mobile/build/app/outputs/flutter-apk/app-release.apk`. App build với `LIFEOS_ENV=prod`, tên "LifeOS",
định danh `vn.lifeos.app`.

## 4. Cài lên điện thoại

1. Chép `app-release.apk` sang điện thoại (cáp USB, Google Drive, Zalo...), hoặc khi cắm cáp: `adb install -r build/app/outputs/flutter-apk/app-release.apk`.
2. Mở file, cho phép "Cài ứng dụng không rõ nguồn gốc". Nếu Play Protect cảnh báo, chọn "Vẫn cài đặt".
3. Cập nhật lần sau: cài đè file APK mới (cùng keystore) — không cần gỡ app.

## 5. Đăng ký tài khoản

1. Trong app: Đăng ký bằng email và mật khẩu.
2. Trên máy tính, mở http://localhost:8025 (Mailpit — email không gửi ra ngoài thật). Mở thư xác minh, chép phần token
   sau `#token=` trong liên kết.
3. Dán token vào ô xác minh email trong app rồi đăng nhập. Quên mật khẩu cũng lấy token theo cách này.

## 6. Hằng ngày

| Việc | Lệnh (từ `backend/`) |
|---|---|
| Xem địa chỉ hiện tại | `docker compose -f docker-compose.yml -f docker-compose.tunnel.yml --env-file .env.tunnel logs cloudflared` (tìm dòng `trycloudflare.com`) |
| Dừng (giữ dữ liệu) | `docker compose -f docker-compose.yml -f docker-compose.tunnel.yml --env-file .env.tunnel stop` |
| Chạy lại | chạy lại `scripts/tunnel-up.ps1` (địa chỉ có thể đổi ⇒ build lại APK) |
| Sao lưu database | `docker compose -f docker-compose.yml exec -T postgres pg_dump -U lifeos -d lifeos > lifeos-backup.sql` |

**Không** chạy `docker compose down -v`: `-v` xóa volume `lifeos-pg`, tức xóa toàn bộ dữ liệu.

## 7. Bảo mật

- Backend mở ra Internet qua tunnel: ai có địa chỉ đều gọi được API (vẫn cần đăng nhập). Không chia sẻ địa chỉ; dùng mật
  khẩu mạnh. Đăng ký tài khoản đang mở, nhưng người lạ không lấy được token xác minh vì Mailpit chỉ mở trên máy bạn.
- `TRUSTED_PROXIES` chỉ tin dải mạng Docker nội bộ (container `cloudflared`), để rate limit đăng nhập thấy IP thật.
- Tài liệu API (`/v3/api-docs`) bị tắt; profile vẫn là `dev` (log DEBUG chỉ nằm trên máy bạn).
- AI tắt (không có `OPENAI_API_KEY`); FCM tắt.
- Dữ liệu (gồm tài chính) nằm trên ổ máy tính: sao lưu định kỳ theo mục 6.

## 8. Xử lý sự cố

| Triệu chứng | Nguyên nhân / cách xử lý |
|---|---|
| Script báo "Chưa thấy địa chỉ tunnel"; log `cloudflared` lặp `failed to request quick Tunnel: ... api.trycloudflare.com ... Client.Timeout` | Máy không gọi được dịch vụ cấp Quick Tunnel (dịch vụ tạm lỗi, mạng/ISP chặn, hoặc VPN như Cloudflare WARP can thiệp). Container tự thử lại; thử tắt/bật WARP hoặc đổi mạng rồi chạy lại script. Đã gặp lúc thiết lập 2026-11-19. |
| Có địa chỉ nhưng mở ra trang "Cloudflare Tunnel error 1033" (HTTP 530) dù log có `Registered tunnel connection ... protocol=quic` | QUIC (UDP) không nhận request khi máy bật VPN/WARP. `docker-compose.tunnel.yml` đã ép `--protocol http2`; nếu tự sửa lệnh thì giữ cờ này. Đã gặp và xử lý 2026-11-19. |
| Log `cloudflared` có dòng `ERR Cannot determine default origin certificate path ... cert.pem` | Vô hại với Quick Tunnel (chỉ cần cho named tunnel); bỏ qua |
| App báo không kết nối được | Địa chỉ tunnel đã đổi so với lúc build APK ⇒ build lại; hoặc máy tính/Docker đang tắt (app vẫn dùng offline) |
| `flutter build apk --release` báo thiếu `key.properties` | Làm bước 2 |
| Cài đè APK báo xung đột chữ ký | APK ký bằng keystore khác — phải dùng đúng keystore cũ, hoặc gỡ app (mất dữ liệu chưa đồng bộ) |
