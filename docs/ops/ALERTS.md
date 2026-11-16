# LifeOS — Cảnh báo vận hành

> Baseline DWP6 đã duyệt ngày 2026-11-10. Không đưa `userId`, ID thực thể, email, prompt, model output, số tiền nghiệp vụ hoặc dữ liệu cá nhân vào label/annotation.

Nguồn rule đang bật là `ops/prometheus/rules/lifeos-alerts.yml`. Rule cần metric backend mới nằm trong `lifeos-future-alerts.yml` và **không được load** cho đến khi producer tương ứng đã có test.

## Cảnh báo đang cấu hình

| Cảnh báo | Điều kiện | Mức | Runbook đầu tiên |
|---|---|---|---|
| Backend metrics down | target `up == 0` trong 2 phút | Critical | Kiểm app/private scrape/auth và readiness; không mở endpoint public để chữa tạm |
| 5xx cao | >1% trong 5 phút | Critical | Kiểm deploy gần nhất, DB, log đã redaction; rollback runtime nếu hồi quy |
| Hikari pending | >0 trong 5 phút | Critical | Kiểm DB latency/connection, traffic và pool; không tăng pool trước khi đo DB |
| JVM heap | >90% trong 10 phút | Warning | Kiểm GC/traffic; thu heap dump chỉ theo quy trình bảo vệ PII |
| Outbox FAILED | tăng trong 15 phút | Critical | Cô lập nguyên nhân, retry theo idempotency; không sửa hàng trực tiếp |
| Reminder lag | p95 >90 giây | Warning | Kiểm scheduler, DB lock, outbox |
| Sync push chậm | p95 >2 giây trong 10 phút | Warning | Kiểm pool/lock/payload và release gần nhất |
| Vi phạm nhất quán | gauge >0 | Critical | Dừng release, bảo toàn bằng chứng, không tự sửa dữ liệu |
| Export lỗi | >3/giờ | Warning | Kiểm disk/quota/permission, không log nội dung export |
| Backup cũ | >26 giờ | Critical | Chạy backup có giám sát, kiểm object/checksum/KMS; mở incident nếu vẫn lỗi |
| Restore drill quá hạn | >35 ngày | Warning | Lập lịch restore vào DB mới, không restore đè production |
| Metric backup/restore mất | absent 30 phút/1 giờ | Critical/Warning | Kiểm runner, named volume và textfile collector; no-data không phải healthy |

Metric backup/restore được script ops ghi theo atomic rename vào textfile volume và `node-exporter` thu thập. Backup runner phải mount volume `lifeos-textfile-metrics` tại `PROMETHEUS_TEXTFILE_DIR`; nếu không, alert sẽ có trạng thái no-data và phải được coi là cấu hình chưa hoàn tất.

## Rule chưa bật

- `lifeos.job.last_success{job}` và expected interval: chờ thay đổi backend sau kết quả review.
- AI provider/error/latency/circuit/quota/budget/action: chờ AIP2/AIP3; AIP6 action vẫn mặc định tắt.
- Disk 80/90%: Render/PaaS cần metric/datasource hạ tầng cụ thể; không giả lập bằng metric container không tồn tại.

No-data không đồng nghĩa healthy. Trước go-live phải có alert riêng cho scrape target down/no-data từ hệ thống giám sát được chọn.

## Kiểm tra và thay đổi ngưỡng

Chạy `promtool check config ops/prometheus/prometheus.yml` và `promtool check rules ops/prometheus/rules/*.yml` bằng đúng image Prometheus đã pin. Mọi thay đổi ngưỡng từ baseline phải ghi `[PROPOSED]` trong progress log, có dữ liệu staging/production đã khử PII làm căn cứ và được duyệt trước khi áp dụng production.
