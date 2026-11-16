# Render provisioning checklist (DWP3)

> Chưa tạo tài nguyên. File này không chứa account ID, credential, domain thật hoặc secret. Thực hiện bằng dashboard/API của tài khoản người dùng và ghi bằng chứng vào progress/final report.

## 1. Thứ tự tạo tài nguyên

1. Chọn region Singapore/APAC gần nhất có cả web service và managed PostgreSQL; app và DB phải cùng region.
2. Tạo PostgreSQL paid có encryption/backup. Không cho phép public ingress nếu private connection đáp ứng migration/restore path.
3. Tạo hai login/role riêng:
   - migration role: DDL, chỉ cấp cho pre-deploy job;
   - runtime role: connect + DML/sequence trên schema ứng dụng, không `CREATE/ALTER/DROP`.
4. Build/push runtime và migration image cùng immutable release/digest theo `deploy/README.md`.
5. Tạo một private/background migration service từ migration image: command mặc định `idle`, chỉ có migration DB role, không có public URL. CI deploy đúng migration digest rồi tạo [Render one-off job](https://api-docs.render.com/reference/post-job) với `startCommand=migrate` và chờ trạng thái `succeeded`.
6. Chỉ sau migration thành công mới gọi [Trigger deploy](https://api-docs.render.com/reference/create-deploy) cho web service bằng runtime `imageUrl` digest và chờ trạng thái `live`.
7. Web service dùng profile `prod`, health path `/actuator/health/readiness`, một instance 1 CPU/2 GB khởi điểm, persistent disk mount đúng `/var/lib/lifeos/exports`.
8. Nạp secret theo DWP1 inventory qua Render secret store. Không dùng env group chung giữa staging và production.
9. Tạo staging service + DB riêng trước production; staging chỉ dùng dữ liệu giả.

Không đặt migration credential trên runtime web service. Migration service phải private, process idle không chạy Flyway; chỉ one-off job mới chạy `migrate`. Không chạy Flyway đồng thời từ nhiều job. Nếu account/plan không hỗ trợ one-off job trong private network, dừng trước production deploy và chọn cơ chế pre-deploy tương đương; không mở DB public chỉ để né giới hạn.

## 2. TLS và domain

1. Smoke bằng hostname Render tạm.
2. Gắn `staging-api.<owned-domain>` trước; tạo DNS record đúng hướng dẫn platform.
3. Chờ certificate `issued/verified`, kiểm chuỗi TLS và redirect HTTP→HTTPS.
4. Sau ít nhất một release staging ổn định, gắn `api.<owned-domain>` production.
5. Chỉ bật HSTS sau khi HTTPS/domain cuối đã xác minh; không bật `includeSubDomains` nếu các subdomain khác chưa sẵn sàng.

Nguồn cần đối chiếu lúc provisioning: tài liệu Render về [web services](https://render.com/docs/web-services), [PostgreSQL](https://render.com/docs/postgresql-creating-connecting) và [custom domains](https://render.com/docs/custom-domains).

## 3. Trusted proxy và client IP

- `TRUSTED_PROXIES` phải là regex cho địa chỉ/dải proxy thực tế mà platform công bố hoặc quan sát và xác minh; cấm `.*`, `0.0.0.0` hoặc tin toàn internet.
- Nếu platform không bảo đảm nguồn proxy ổn định/được tài liệu hóa, không tự suy đoán CIDR. Ghi blocker theo §11 trước go-live.
- Chạy `ClientIpIT` và `ClientIpTrustedProxyIT` trong CI.
- Live staging: gửi chuỗi request có `X-Forwarded-For` giả từ cùng client. Header giả không được tạo bucket IP tùy ý; rate limit phải dựa trên client IP do platform thêm. Kiểm log/metric chỉ bằng mã kết quả, không ghi raw header.

## 4. Bằng chứng bắt buộc trước khi đánh dấu DWP3 xong thật

- URL staging HTTPS + thời điểm smoke; readiness `UP`, API docs không public ở prod profile.
- App/DB cùng region; DB private endpoint; thử kết nối từ internet thất bại.
- Ảnh/chứng cứ cấu hình web service không chứa giá trị secret trong report.
- Migration chạy bằng role DDL; runtime role bị từ chối khi thử DDL vô hại trong transaction rollback.
- Kết quả XFF spoof/trusted proxy; body limit ở proxy không thấp hơn app và không vô hạn.
- Domain/certificate/DNS status; redirect và security headers.

## 5. Cleanup/rollback

- Staging lỗi: rollback runtime digest; không tự down-migrate.
- Chỉ xóa service/DB sau khi xác nhận đúng environment ID và backup/restore evidence. Giữ DB cô lập nếu cần điều tra.
- Không xóa persistent disk cùng lúc rollback app.
