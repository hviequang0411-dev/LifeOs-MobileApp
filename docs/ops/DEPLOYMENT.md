# Runbook triển khai LifeOS

## Phạm vi

Runbook vận hành DWP2–DWP3 cho một backend instance. Production ưu tiên Render + managed PostgreSQL; Compose dùng cho staging/VPS fallback và kiểm artifact. Secret chỉ ở platform store hoặc file ngoài repo.

## Pre-deploy

1. Xác định environment, release tag và hai image digest runtime/migration cùng build.
2. Chạy preflight tên biến; không in giá trị vào log.
3. Xác nhận backup/restore point, dung lượng DB/export disk và không có incident đang mở.
4. Kiểm migration additive/checksum; không sửa V1–V11.
5. Xác nhận runtime role không có DDL và migration role không nằm trên web service.

## Deploy

1. Chạy migration job một lần; lưu exit code, checksum/version Flyway và correlation triển khai, không lưu DB URL/password.
2. Nếu migration thất bại: không cập nhật app; giữ DB để điều tra, không tự `repair`/`clean`.
3. Cập nhật runtime digest; chờ liveness/readiness.
4. Smoke: health, login với tài khoản staging giả, một CRUD không nhạy cảm, sync nhỏ; production không dùng dữ liệu giả nếu nó tạo dữ liệu người dùng thật.
5. Quan sát 5xx, p95, Hikari pending, outbox/job trong ít nhất 15 phút trước khi đóng release.

## Rollback

1. Chuyển runtime về digest trước.
2. Không chạy down migration. Nếu schema additive vẫn tương thích, app cũ tiếp tục chạy.
3. Nếu migration phá tương thích hoặc có dấu hiệu mất dữ liệu: dừng traffic/deploy theo §11, restore vào DB mới, xác minh rồi mới chuyển endpoint.
4. Không chạy `docker compose down -v` hoặc xóa DB/disk trong rollback.

## TLS/proxy

- Platform/proxy là điểm public duy nhất; backend Compose bind loopback.
- Chỉ tin XFF từ regex `TRUSTED_PROXIES` đã xác minh. Cấm wildcard.
- HTTPS bắt buộc; HSTS sau khi domain/certificate ổn định.
- Proxy body limit phải bao phủ app limit 1 MB và Note 192 KiB nhưng không vô hạn.

## Chưa thể xác nhận khi chưa có tài khoản

Provisioning Render, private DB, DNS, certificate, live XFF test và staging health phải ghi `CHƯA CHẠY` cho đến khi có run URL/kết quả thật.
