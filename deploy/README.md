# LifeOS deployment artifacts

Các file ở đây là nguồn triển khai cho DWP2. Không commit `.env` thật; chỉ các file `.example` được phép vào repo.

## 1. Build hai image cùng một release

Chạy từ root repo bằng Docker BuildKit:

```bash
docker build -f backend/Dockerfile --target runtime -t registry.example/lifeos-backend:RELEASE backend
docker build -f backend/Dockerfile --target migrations -t registry.example/lifeos-migrations:RELEASE backend
```

CI phải push immutable tag và ghi digest. Production dùng tag bất biến hoặc `image@sha256:...`, không dùng `latest`. Base image hiện pin theo version/tag; DWP5 sẽ quét CVE, sinh SBOM và khóa digest đã kiểm chứng trước go-live.

## 2. Chuẩn bị cấu hình

1. Sao chép `.env.production.example` hoặc `.env.staging.example` ra secret workspace ngoài repo.
2. Thay mọi placeholder, giữ app DB role và migration DB role tách biệt.
3. Kiểm tên biến mà không in giá trị:

```powershell
pwsh deploy/scripts/preflight.ps1 -EnvironmentFile C:\secure\lifeos-production.env
```

```bash
sh deploy/scripts/preflight.sh /secure/lifeos-production.env
```

Production Spring profile cố ý tắt Flyway và yêu cầu SMTP credential, persistent `EXPORT_DIR`, cùng regex `TRUSTED_PROXIES`. Compose cũng dừng trước khi tạo container nếu thiếu biến bắt buộc.

## 3. Trình tự release

Không chạy `up` trước migration. Trình tự chuẩn:

1. Xác nhận backup/restore point theo DWP4.
2. Pull hai image cùng release/digest.
3. Chạy migration job một lần bằng DDL role.
4. Khởi động/cập nhật runtime bằng DML role.
5. Kiểm readiness, auth smoke và log không có secret/PII.

Production Compose:

```bash
docker compose --env-file /secure/lifeos-production.env -f deploy/compose.production.yml --profile migration run --rm migrate
docker compose --env-file /secure/lifeos-production.env -f deploy/compose.production.yml up -d backend
docker compose --env-file /secure/lifeos-production.env -f deploy/compose.production.yml ps
```

Staging thay bằng `compose.staging.yml` và env staging. PostgreSQL là managed/private, không có container DB trong hai compose này.

Render dùng cùng hai image: migration image là pre-deploy job; runtime image là web service. App và DB phải cùng region. Không cấp migration credential cho runtime service.

## 4. Health, filesystem và shutdown

- Liveness: `/actuator/health/liveness`; readiness: `/actuator/health/readiness`.
- Backend chỉ bind loopback trong Compose để reverse proxy/TLS ở DWP3 là cổng public duy nhất.
- Root filesystem read-only; `/tmp` là tmpfs giới hạn; export là persistent volume riêng.
- Container chạy UID/GID 10001, drop Linux capabilities, `no-new-privileges`, UTC và graceful shutdown 30 giây (Compose chờ 40 giây).
- Baseline 1 CPU/2 GB, heap tối đa 70%; điều chỉnh chỉ sau benchmark/metric.

## 5. Rollback DWP2

- App rollback: đặt `LIFEOS_IMAGE` về digest trước và chạy lại `up -d backend`.
- Không tự chạy down migration. Migration DWP2 là forward-only/additive; nếu schema không tương thích, dừng theo §11 và restore vào DB mới đã xác minh.
- Không dùng `docker compose down -v` ở staging/production vì xóa volume export. Dọn tài nguyên phải chỉ rõ target và có backup.

## 6. Việc chưa chạy trên máy soạn artifact

Máy hiện không có Docker, Maven hoặc Java 21 nên chưa build/smoke image. Chạy trên môi trường chuẩn:

```bash
docker build --check -f backend/Dockerfile backend
docker build -f backend/Dockerfile --target runtime -t lifeos-backend:dwp2 backend
docker build -f backend/Dockerfile --target migrations -t lifeos-migrations:dwp2 backend
docker compose --env-file /secure/lifeos-staging.env -f deploy/compose.staging.yml config --quiet
mvn -f backend/pom.xml clean verify
```
