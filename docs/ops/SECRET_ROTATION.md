# LifeOS — Xoay secret

## Quy tắc

- Secret nằm trong secret manager/platform, khác nhau giữa dev/staging/production; không gửi qua chat, commit, image, log hay report.
- Ghi owner, ngày tạo/xoay, môi trường và nơi dùng; không ghi giá trị. Ưu tiên overlap ngắn/dual-key khi giao thức cho phép.
- Mỗi lần xoay phải có rollback hữu hạn, health/auth/sync/job smoke và xác nhận secret cũ đã revoke sau khi ổn định.

## Ma trận

| Secret | Cách xoay | Kiểm chứng | Thu hồi cũ |
|---|---|---|---|
| JWT | Tạo key/kid mới, giữ previous trong cửa sổ token, deploy issuer/validator | Token mới hợp lệ; token cũ còn hợp lệ đúng cửa sổ; kid lạ bị từ chối | Xóa previous sau TTL tối đa |
| Notification unsubscribe HMAC | Maintenance/dual verify nếu code hỗ trợ; hiện phải kiểm hành vi trước xoay | Link mới hoạt động, link sai bị từ chối | Theo TTL link đã công bố |
| DB runtime | Tạo role/password mới DML-only, cập nhật secret, deploy | App start, CRUD/sync smoke; DDL bị từ chối | Revoke password/role cũ |
| DB migration | Tạo credential DDL tạm, chạy one-off migrate/validate | Job success, runtime không có credential | Revoke ngay sau window nếu mô hình ephemeral |
| SMTP | Tạo credential mới, cập nhật secret | Gửi mail staging/synthetic; SPF/DKIM/DMARC không đổi ngoài ý muốn | Revoke cũ |
| FCM/APNs | Tạo key/service account quyền tối thiểu | Synthetic device staging nhận notification | Revoke cũ và kiểm lỗi provider |
| OpenAI | Tạo key trong đúng project/environment và spend cap | Smoke dữ liệu giả, metric cost/quota, không log key | Revoke cũ, kiểm usage bất thường |
| Backup S3/KMS | Tạo access mới/role mới, giữ key encryption được phép decrypt lịch sử | Backup mới + restore checksum vào DB mới | Revoke access cũ; không xóa KMS key cần đọc backup |
| Grafana | Tạo admin/service credential mới | Login/API health qua private path | Revoke cũ, kết thúc session nếu hỗ trợ |

## Khi nghi lộ

Không chờ lịch xoay: cô lập tính năng, revoke credential, tạo credential mới, kiểm log/artifact/usage, mở incident privacy nếu dữ liệu có thể đã bị truy cập. Với OpenAI, tắt `lifeos.ai.enabled`; với DB, chặn network/role bị lộ trước khi thay password.
