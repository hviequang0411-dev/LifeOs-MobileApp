# LifeOS — Observability

## Trạng thái và ranh giới

DWP6 đã cung cấp Prometheus, Grafana, dashboard, alert rules và metric textfile cho backup/restore. Stack chưa được tuyên bố vận hành production: backend hiện chưa có Prometheus registry/endpoint được bảo vệ và job/AI metric producer chưa được triển khai.

Không mở `/actuator/prometheus`, Prometheus hay Grafana ra Internet công khai. Grafana/Prometheus trong compose chỉ bind loopback; backend và scraper dùng network riêng `lifeos-observability`. Trên Render, dùng private service/network hoặc dịch vụ metrics có xác thực; không thêm exception public vào `SecurityConfig`.

## Chạy stack local/staging

1. Tạo network một lần: `docker network create lifeos-observability`.
2. Đặt `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD`, `GRAFANA_ROOT_URL`; không lưu secret vào repo.
3. Start backend compose và `ops/compose.observability.yml` trên cùng network.
4. Với backup/restore runner, mount named volume `lifeos-textfile-metrics` read-write và đặt `PROMETHEUS_TEXTFILE_DIR` trỏ tới mount đó. Node exporter chỉ đọc volume.
5. Truy cập Grafana/Prometheus qua SSH tunnel hoặc loopback; không publish port công khai.

## Backend producer còn thiếu — chờ review backend

Thay đổi tối thiểu sau khi được mở khóa:

- thêm Micrometer Prometheus registry tương thích Spring Boot hiện tại;
- expose `/actuator/prometheus` chỉ trên management/private interface hoặc cơ chế authenticated scrape;
- thêm test chứng minh Internet/public route bị từ chối và private scrape hoạt động;
- tạo `lifeos.job.last_success_seconds{job}` và `lifeos.job.expected_interval_seconds{job}` với tập `job` hữu hạn;
- bật `lifeos-future-alerts.yml` chỉ sau khi producer/test tồn tại;
- AIP2/AIP3 định nghĩa AI metric theo Phase 8, không chứa prompt/context/user/entity ID.

Không sửa backend cho phần này trước khi người dùng báo kết quả review backend.

## Quy tắc cardinality và dữ liệu

Label chỉ dùng tập hữu hạn như `result`, `outcome`, `channel`, `type`, `job`, `uri` đã chuẩn hóa. Không dùng URL thô, exception message, query, user/entity/conversation/message/proposal ID, email, prompt, output hoặc amount. Dashboard và alert annotation chỉ chứa tên service/metric/runbook, không echo dữ liệu request.

## Kiểm chứng bắt buộc trước go-live

```text
promtool check config ops/prometheus/prometheus.yml
promtool check rules ops/prometheus/rules/lifeos-alerts.yml
docker compose -f ops/compose.observability.yml config
jq empty ops/grafana/lifeos-backend.json ops/grafana/lifeos-operations.json
```

Sau khi có staging: xác minh target `UP`, tạo có kiểm soát từng signal 5xx/pool/job/backup, kiểm route scrape không public, kiểm alert không chứa PII và chụp bằng chứng dashboard. `lifeos-future-alerts.yml` phải tiếp tục không load nếu bất kỳ producer nào chưa có.
