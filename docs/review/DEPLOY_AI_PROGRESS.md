# LifeOS — Deploy + AI Progress

> Nhật ký thực thi theo `docs/engineering/DEPLOY_AI_MASTER_PLAN.md` đã duyệt ngày 2026-11-10. Không ghi secret, PII, prompt hay dữ liệu người dùng vào file này.

## 1. Phê duyệt và chỉ thị thực thi

- `[CONFIRMED]` D260–D299.
- `[RESOLVED]` O-AI-01..08 và O-DEP-01..10 theo phương án khuyến nghị.
- AIP1 công bố contract trước để Flutter triển khai song song.
- DWP1 bắt đầu ngay; nhánh Deploy tiếp tục theo dependency.
- `[CONFIRMED]` (2026-11-11) Review backend đã có kết quả; các sửa lỗi backend được người dùng **HOÃN** theo `CLAUDE_CODE_BACKEND_FIX_PROMPT.md`. Điều kiện chờ AIP2 được gỡ; thực thi AIP2 → AIP3 → AIP4 → AIP7. AIP5/AIP6 vẫn hoãn theo O-AI-06/O-AI-05.
- Chỉ dừng theo `DEPLOY_AI_MASTER_PLAN.md` §11.

---

## 2. DWP1 — Baseline vận hành

**Trạng thái:** HOÀN THÀNH phần quyết định/inventory ngày 2026-11-10. Provisioning thật thuộc DWP3 và việc con người.

### 2.1. Hosting, region và domain

| Mục | Quyết định `[CONFIRMED]` |
|---|---|
| Production app | Render paid web service, một instance 1 CPU/2 GB khởi điểm |
| Production DB | Render managed PostgreSQL có mã hóa/backup, tách app role và migration role |
| Region | Singapore/APAC gần người dùng nhất mà Render đồng thời hỗ trợ app + DB; hai dịch vụ bắt buộc cùng region. Nếu tài khoản không cung cấp Singapore, chọn region khả dụng gần nhất theo O-DEP-03 và ghi region thực tế ở DWP3 |
| Production hostname | `api.<domain-cua-nguoi-dung>`; dùng hostname Render tạm cho đến khi người dùng cung cấp DNS zone |
| Staging hostname | `staging-api.<domain-cua-nguoi-dung>`; staging có DB/secret riêng và chỉ dữ liệu giả |
| Scale | Một instance; chưa được scale ngang trước khi xử lý shared rate limit/revocation, snapshot HMAC, object storage, job locking và AI counter |

Tên miền thật, tài khoản/billing và DNS là việc con người; không chặn DWP2/local artifact.

### 2.2. RPO/RTO và SLO beta

| Chỉ số | Baseline `[CONFIRMED]` | Cách đo/alert |
|---|---:|---|
| Availability core API | ≥ 99,5% theo tháng, không tính maintenance đã thông báo | probe readiness + tỷ lệ HTTP 5xx |
| HTTP 5xx | < 1% trên cửa sổ 5 phút | alert theo `docs/ops/ALERTS.md` |
| Dashboard p95 | ≤ 800 ms | `http_server_requests_seconds` |
| Daily/monthly review p95 | ≤ 500 ms | metric theo URI chuẩn hóa |
| Analytics p95 | ≤ 1 s | metric theo URI chuẩn hóa |
| Sync push p95 | ≤ 2 s cho batch tối đa 50 | metric + cảnh báo 503 |
| AI provider p95 | ≤ 25 s; hard timeout 30 s | metric AI, không áp vào availability core |
| Backup RPO | ≤ 24 giờ kể cả khi PITR không dùng được | tuổi logical backup ngoài hệ thống |
| Restore RTO | ≤ 4 giờ | restore drill staging hằng tháng |

Baseline latency kế thừa benchmark đã kiểm chứng trong `BACKEND_FINAL_REPORT.md` §11; đo lại trên staging trước go-live. AI bị tắt/hết budget/provider lỗi không được làm core API vi phạm SLO.

### 2.3. Secret inventory — chỉ tên, không giá trị

| Nhóm | Secret/config | Môi trường | Nguồn/owner | Rotation tối thiểu |
|---|---|---|---|---|
| DB runtime | `DB_URL`, `DB_USER`, `DB_PASSWORD` | staging/prod | managed DB + operator | 90 ngày hoặc sau sự cố |
| DB migration | `MIGRATION_DB_URL`, `MIGRATION_DB_USER`, `MIGRATION_DB_PASSWORD` | staging/prod pre-deploy | managed DB + operator | 90 ngày; không cấp cho runtime |
| JWT | `JWT_SECRET`, `JWT_KID`, previous key khi xoay | staging/prod | platform secret store | 90 ngày, dual-key rollout |
| Notification | `NOTIFICATION_UNSUBSCRIBE_SECRET`, `API_BASE_URL` | staging/prod | platform secret store | 90 ngày; khác JWT |
| SMTP | `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM`, `APP_BASE_URL` | staging/prod | mail provider/operator | theo provider hoặc sau sự cố |
| FCM | `FCM_ENABLED`, project/client email/private key/token URI | staging/prod | Firebase/operator | theo Google; revoke khi lộ |
| Export | `EXPORT_DIR`, quota/max bytes | staging/prod | persistent disk/platform config | config review mỗi release |
| AI | `OPENAI_API_KEY`, project/org nếu dùng, model allowlist, quota/budget/feature flags | staging/prod | OpenAI project/operator | 90 ngày hoặc lập tức khi lộ |
| Ops | Prometheus/Grafana auth, backup/object-storage credential | staging/prod | platform/operator | 90 ngày hoặc sau sự cố |

Production phải fail-fast khi thiếu/placeholder; dev dùng giá trị giả cục bộ, Mailpit và fake AI. Không secret nào được đưa vào image, repo, log, metric, fixture hoặc Flutter.

### 2.4. Bằng chứng và chưa chạy

- Đã đối chiếu `BACKEND_FINAL_REPORT.md` §8/§11 và `docs/ops/ALERTS.md` để chốt single-instance constraints cùng SLO.
- Đã tạo progress log và secret inventory không chứa giá trị.
- `CHƯA CHẠY`: tạo Render service/DB, mua/cấu hình domain, backup/restore, probe SLO. Đây là DWP3/DWP4 và cần tài khoản con người; DWP2 có đường local không bị chặn.

Rollback DWP1: chỉ là tài liệu/quyết định; thay đổi cần quyết định mới được duyệt, không có tài nguyên để xóa.

---

## 3. AIP1 — Contract-first cho Flutter

**Trạng thái:** HOÀN THÀNH contract-first ngày 2026-11-10; runtime endpoint chưa triển khai theo đúng ranh giới AIP1.

### 3.1. Quyết định Spring AI

- `[CONFIRMED]` Giữ Spring AI làm toolkit nội bộ dự kiến của `OpenAIProvider` ở AIP3, sau port `LLMProvider`.
- AIP1 không thêm dependency runtime. DTO, JSON Schema, OpenAPI và service không chứa type Spring AI.
- CI dùng fake `LLMProvider`; không network/provider thật.
- Nếu bỏ Spring AI vì bất tương thích/thiếu khả năng đã đo được, phải ghi quyết định và xin duyệt trước khi thêm client khác. Quy tắc này đã được đưa vào `AGENTS.md`.

### 3.2. Contract đã công bố

- OpenAPI `/api/v1/ai`: preferences, conversations/messages/context/delete, daily/weekly plan, task breakdown, Habit/Finance insight, Note summary, action proposal approve/cancel, usage và limits.
- 15 mã lỗi `AI_*` trong `AiErrorCode`.
- 8 fixture tối thiểu `ai-*.json`, dữ liệu tổng hợp, không có `userId`, email, token hay provider/model do server quản lý.
- `AiContractPublicationTest` kiểm surface/fixture; `OpenApiExportIT` có ngoại lệ hẹp, đánh dấu bằng hai block AIP1. AIP2 phải sinh cùng contract từ controller/DTO rồi xóa ngoại lệ trước khi dùng `-Dopenapi.export=true`.

### 3.3. Kiểm chứng

- 8/8 fixture parse JSON thành công; kiểm tĩnh không có các field `userId`, email, password, API/access/refresh/device/FCM token.
- OpenAPI parse YAML thành công bằng SnakeYAML: 151 paths tổng, 219 schemas tổng; overlay AIP1 có 18 AI path và 42 AI schema.
- Tất cả 41 `$ref` tới schema AI đều resolve; 21 `operationId` AI không trùng.
- Bỏ đúng hai block marker AIP1 cho kết quả **trùng byte** với OpenAPI runtime baseline trước AIP1; ngoại lệ không che thay đổi API ngoài AI.
- `CHƯA CHẠY`: `AiContractPublicationTest`, `OpenApiExportIT`, `mvn clean verify` vì máy hiện chỉ có Java 11 và không có Maven/Docker, trong khi repo yêu cầu Java 21. Lệnh cần chạy trên môi trường chuẩn:

```powershell
mvn -f backend/pom.xml test -Dtest=AiContractPublicationTest
mvn -f backend/pom.xml verify -Dit.test=OpenApiExportIT
mvn -f backend/pom.xml clean verify
```

Không tuyên bố backend endpoint đã tồn tại: AIP1 là contract-first; runtime bắt đầu AIP2 sau khi review backend được người dùng chấp nhận.

Rollback AIP1: xóa 8 fixture AI, test publication, hai block marker OpenAPI và ngoại lệ marker trong `OpenApiExportIT`; không có schema DB/runtime/data để rollback.

---

## 4. DWP2 — Image, production config, compose và migration job

**Trạng thái:** HOÀN THÀNH artifact ngày 2026-11-10; build/container smoke ghi `CHƯA CHẠY` do môi trường hiện tại thiếu Docker/Maven/Java 21.

### 4.1. Thay đổi

- `backend/Dockerfile`: multi-stage BuildKit; target `runtime` non-root UID/GID 10001, UTC, heap 70%, liveness, graceful SIGTERM; target `migrations` chỉ chứa Flyway + V1–V11.
- `application-prod.yml`: runtime Flyway tắt; bắt buộc SMTP credential, persistent `EXPORT_DIR` và regex `TRUSTED_PROXIES`; Hikari baseline 10; shutdown 30 giây.
- `deploy/compose.production.yml` và `compose.staging.yml`: managed DB, migration/runtime role tách, root filesystem read-only, tmpfs giới hạn, drop capability, loopback-only port, export volume, resource/log limit và fail-fast `${VAR:?}`.
- `.env.*.example`, `.gitignore`, PowerShell/POSIX preflight không in giá trị và `deploy/README.md` với build/migrate/start/rollback từ máy sạch.
- `ProductionDeploymentContractTest`; cập nhật `ApiDocsProdProfileIT` để prod-profile IT bật Flyway riêng cho Testcontainers và cấp config bắt buộc giả.

### 4.2. Quyết định phát sinh có thể hoàn tác

- `[CONFIRMED]` (2026-11-11) P-DWP2-01: phát hành hai image cùng release/digest: runtime và migration. Migration image dùng Flyway `12.4.0`, khớp dependency đã resolve của backend; runtime không giữ DDL credential.
- `[CONFIRMED]` (2026-11-11) P-DWP2-02: Compose production/staging bind backend vào loopback; DWP3 đặt TLS proxy phía trước. Render dùng platform port/TLS với cùng runtime image, không dùng mapping loopback của Compose.
- `[CONFIRMED]` (2026-11-11) P-DWP2-03: base images pin tag/version ở DWP2; DWP5 khóa digest sau quét CVE/SBOM để tránh ghi một digest chưa được kiểm chứng.

### 4.3. Kiểm chứng đã chạy

- SnakeYAML parse thành công `compose.production.yml`, `compose.staging.yml` và `application-prod.yml`.
- PowerShell parser báo 0 lỗi cho `preflight.ps1`.
- Preflight nhận env tổng hợp hợp lệ và không in secret; từ chối file `.example` còn placeholder.
- Quét tĩnh không thấy secret mặc định, password `lifeos`, image `latest`, PostgreSQL public container hoặc trusted-proxy wildcard trong artifact DWP2.

### 4.4. `CHƯA CHẠY`

Máy hiện chỉ có Java 11, không có Maven/Docker. Cần chạy:

```powershell
docker build --check -f backend/Dockerfile backend
docker build -f backend/Dockerfile --target runtime -t lifeos-backend:dwp2 backend
docker build -f backend/Dockerfile --target migrations -t lifeos-migrations:dwp2 backend
docker compose --env-file <secure-staging-env> -f deploy/compose.staging.yml config --quiet
mvn -f backend/pom.xml test -Dtest=ProductionDeploymentContractTest
mvn -f backend/pom.xml verify -Dit.test=ApiDocsProdProfileIT
mvn -f backend/pom.xml clean verify
```

Sau build phải xác minh UID khác 0, health/readiness, thiếu từng secret không start, runtime không có DDL quyền, migration role không dùng cho app và shutdown trong 40 giây.

Rollback DWP2: dùng digest app trước; không chạy down migration; production compose không được `down -v`. Các file cấu hình chưa tạo tài nguyên thật nên có thể hoàn tác bằng bản trước.

---

## 5. DWP3 — TLS, proxy, domain và staging provisioning

**Trạng thái:** HOÀN THÀNH artifact/runbook; provisioning và live verification `CHƯA CHẠY` vì cần tài khoản Render, billing và domain của người dùng. Đường local/artifact đã hoàn tất nên tiếp tục DWP4, chưa go-live.

### 5.1. Thay đổi

- `deploy/render/PROVISIONING.md`: thứ tự tạo app/managed DB cùng region, role DDL/DML tách, migration-before-runtime, persistent export disk, staging riêng, TLS/domain và bằng chứng bắt buộc.
- `docs/ops/DEPLOYMENT.md`: pre-deploy, deploy, smoke, rollback không down-migration, TLS/proxy và giới hạn điều được tuyên bố.
- Dùng `ClientIpIT` và `ClientIpTrustedProxyIT` hiện có làm cổng XFF; checklist thêm live spoof test qua platform.

### 5.2. Quyết định phát sinh

- `[CONFIRMED]` (2026-11-11) P-DWP3-01: dùng hostname Render tạm cho smoke; gắn staging custom domain trước production; chỉ bật HSTS sau khi certificate/domain cuối ổn định.
- `[CONFIRMED]` (2026-11-11) P-DWP3-02: không suy đoán proxy CIDR của Render. `TRUSTED_PROXIES` chỉ nhận regex từ nguồn platform đã xác minh; nếu không có nguồn ổn định thì đây là blocker go-live theo §11.
- `[CONFIRMED]` (2026-11-11) P-DWP3-03: không mở public DB chỉ để chạy migration từ CI; migration phải chạy trong private network bằng role DDL riêng hoặc cơ chế pre-deploy tương đương.

### 5.3. `CHƯA CHẠY`

- Tạo web service/managed DB staging và production; xác minh Singapore/APAC thực tế.
- Private connectivity, DB không public, runtime role bị từ chối DDL.
- DNS, TLS certificate, HTTP→HTTPS, HSTS và security headers.
- Live readiness/auth/sync smoke và XFF spoof test qua Render.

Không có credential/account nên không thể tuyên bố DWP3 vận hành thật. Việc này không chặn viết backup/restore artifact DWP4; nhưng DWP8/go-live không được hoàn thành nếu chưa có bằng chứng trên.

---

## 6. DWP4 — Backup, restore và data lifecycle

**Trạng thái:** HOÀN THÀNH script/runbook; restore drill thật `CHƯA CHẠY` vì chưa có staging DB/object store.

### 6.1. Thay đổi

- `ops/backup/postgres-backup.sh`: `pg_dump` custom format, SHA-256, upload S3 SSE-KMS, daily/weekly/monthly key; credential DB không nằm trong command line.
- `ops/backup/postgres-restore-drill.sh`: chỉ restore vào DB mới rỗng, yêu cầu confirmation trùng tên DB, kiểm checksum, không `--clean`, xác minh Flyway/users/data_exports và ghi duration.
- `ops/export/cleanup-orphans.sh`: canonical path guard, UUID `.zip`, cũ hơn 26 giờ, đối chiếu `data_exports`; dry-run mặc định, chỉ xóa khi `APPLY=true`, không theo symlink.
- `docs/ops/BACKUP_RESTORE.md`: retention 7 daily/4 weekly/6 monthly, IAM/KMS, quy trình restore, bằng chứng và điều kiện dừng.

### 6.2. Quyết định phát sinh

- `[CONFIRMED]` (2026-11-11) P-DWP4-01: logical backup dùng S3-compatible object store + SSE-KMS và AWS CLI trong backup runner; managed PITR vẫn là lớp thứ nhất, object store khác account/region là lớp độc lập.
- `[CONFIRMED]` (2026-11-11) P-DWP4-02: orphan export chỉ đủ điều kiện sau 26 giờ (TTL 24 giờ + 2 giờ đệm), tên UUID, không có DB reference; job luôn chạy dry-run trước apply.
- `[CONFIRMED]` (2026-11-11) P-DWP4-03: restore script tuyệt đối không `--clean` hay ghi vào DB có table; operator phải tạo DB mới và nhập confirmation đúng tên DB.

### 6.3. Kiểm chứng đã chạy

- Git Bash `bash -n`: 3/3 script hợp lệ cú pháp.
- 3/3 script từ chối chạy khi thiếu env bắt buộc.
- Kiểm tĩnh xác nhận cleanup chỉ có `rm` cho temp dir đã tạo bằng `mktemp` hoặc file đã qua canonical/path/type/age/UUID/DB-reference guard; chưa thực hiện xóa file thật.

### 6.4. `CHƯA CHẠY`

- `pg_dump` thật, upload/checksum/KMS/lifecycle và backup age alert.
- Restore vào PostgreSQL staging mới; `flyway validate`, app read-only smoke, isolation/Finance/sync/outbox/export/AI checks; đo RPO/RTO.
- Orphan dry-run/apply trên export volume staging.

Thiếu staging/account không chặn viết DWP5/DWP6, nhưng DWP8/go-live phải dừng nếu chưa có restore thật đạt RPO/RTO.

---

## 7. DWP5 — CI/CD, image security và rollback

**Trạng thái:** HOÀN THÀNH workflow/script nguồn; `CHƯA CHẠY` trên GitHub/Render vì chưa được chép vào `.github/workflows/` và chưa có environment secret/account.

### 7.1. Thay đổi

- Mở rộng `ci/backend-ci.yml`: Java 21 `clean verify`, migration immutability, secret/dependency scan, build hai target, prod fail-fast, container smoke PostgreSQL 17, SBOM và Trivy High/Critical.
- `ci/backend-nightly.yml`: full multi-device simulation, benchmark, migration→logical dump→restore DB mới, image scan kể cả unfixed.
- `ci/deploy-staging.yml`: verify commit, build/push GHCR runtime+migration cùng SHA, scan runtime digest, lưu manifest và deploy Render staging.
- `ci/deploy-production.yml`: GitHub production environment approval, bắt buộc xác nhận restore point, tải manifest từ staging run, so đúng digest; rollback chỉ runtime digest, không down migration.
- `deploy/scripts/render-deploy.sh`: deploy migration digest, one-off `migrate`, poll job; deploy runtime digest, poll `live` và readiness; retry HTTP hữu hạn, không in API key.
- Migration image có entrypoint allowlist `idle|migrate|validate|info`; private migration service idle và chỉ one-off job mới chạy Flyway.
- `docs/ops/CI_CD.md` ghi mapping `ci/* → .github/workflows/*`, tên secret/variable, gates và rollback.

### 7.2. Quyết định phát sinh

- `[CONFIRMED]` (2026-11-11) P-DWP5-01: Render migration dùng private/background service từ migration image, chỉ có DDL credential; CI cập nhật đúng digest rồi tạo one-off job. Runtime service không có DDL credential.
- `[CONFIRMED]` (2026-11-11) P-DWP5-02: staging là nơi duy nhất build release image; production chỉ nhận hai digest trong artifact manifest của một staging run thành công.
- `[CONFIRMED]` (2026-11-11) P-DWP5-03: action/image version pin ở source hiện tại; trước go-live phải khóa action commit SHA và image digest sau lần chạy/scan đầu, để Dependabot quản lý update có review.

### 7.3. Kiểm chứng đã chạy

- SnakeYAML parse 4/4 workflow source thành công.
- Quét tĩnh 4 workflow: không secret literal, không image `latest`, không path mobile.
- `render-deploy.sh` và migration entrypoint đã từng đạt `bash -n`; script deploy từ chối khi thiếu env. Lần kiểm tra lặp gần nhất Git Bash trên host lỗi tạo MSYS named object trước khi đọc script — lỗi môi trường, không được tính là test script đỏ.
- Thiết kế Render API đối chiếu tài liệu chính thức: create/retrieve one-off job, trigger/retrieve deploy và deploy prebuilt image theo digest.

### 7.4. `CHƯA CHẠY`

- Java/Maven/Docker jobs, container smoke, SBOM/Trivy, nightly simulation/benchmark/restore.
- Chép workflow vào `.github/workflows/`, cấu hình environments/reviewer/GHCR/Render IDs, và mọi GitHub run URL.
- Render migration one-off job, staging deploy/readiness, production deploy/rollback.

Không tuyên bố DWP5 chạy thật cho đến khi có run URL và Render deploy/job ID. Workflow production không được dùng trước khi DWP3/DWP4 có staging/backup bằng chứng.

---

## 8. DWP6 — Observability, alert và incident runbook

**Trạng thái:** HOÀN THÀNH artifact ops/consumer; producer metric backend `CHỜ REVIEW BACKEND`; live stack/drill `CHƯA CHẠY`.

### 8.1. Thay đổi

- Prometheus/Grafana compose chỉ bind loopback, dùng network riêng; dashboard backend hiện có và dashboard operations/AI skeleton được provision tự động.
- Rule core bao phủ target/no-data, 5xx, DB pool, heap, outbox, reminder, sync, consistency, export, backup stale và restore overdue.
- Backup/restore ghi atomic textfile metric; node exporter chỉ bật textfile collector và đọc named volume dùng chung.
- Rule job/AI nằm trong file future và không được load trước khi producer/test tồn tại.
- Bổ sung `OBSERVABILITY.md`, `ALERTS.md`, `INCIDENT_RESPONSE.md`, `SECRET_ROTATION.md`.
- Không sửa `pom.xml`, `SecurityConfig`, application common hoặc scheduler: backend hiện thiếu Prometheus registry/secure scrape và job gauge; đúng điều kiện người dùng, phần này chờ kết quả review backend.

### 8.2. Quyết định phát sinh

- `[CONFIRMED]` (2026-11-11) P-DWP6-01: scrape chỉ qua private network/authenticated path; tuyệt đối không permit public `/actuator/prometheus` chỉ để Prometheus hoạt động.
- `[CONFIRMED]` (2026-11-11) P-DWP6-02: backup/restore metric đi qua node-exporter textfile collector tối giản; runner mount volume riêng, atomic rename tránh scrape file dở.
- `[CONFIRMED]` (2026-11-11) P-DWP6-03: future rules không được load theo từng phần khi metric chưa tồn tại; producer + test/no-data behavior phải đi cùng lần bật rule.

### 8.3. Kiểm chứng đã chạy

- SnakeYAML parse 8/8 file cấu hình compose/Prometheus/rule/provisioning thành công.
- PowerShell JSON parse cả dashboard backend hiện có và dashboard operations mới thành công.
- Kiểm tĩnh xác nhận `lifeos-future-alerts.yml` bị comment khỏi `rule_files` và label/annotation mới không chứa user/entity/prompt/output.

### 8.4. `CHƯA CHẠY` / blocker

- `promtool` không cài trên host; `docker compose config` không trả kết quả trong cửa sổ chạy vì Docker daemon/CLI host không sẵn sàng. Target scrape, Grafana provisioning và alert fire/recover drill trên host thật đều `CHƯA CHẠY`.
- Registry/endpoint scrape backend được bảo vệ, `lifeos.job.last_success`/expected interval producer; chờ người dùng báo kết quả review backend.
- AI metric producer và alert: chờ AIP2/AIP3; action metric chờ AIP6.
- Disk alert phụ thuộc Render/PaaS metric source; chưa tạo expression giả khi datasource chưa chốt.

DWP6 không được coi là production-ready cho đến khi private scrape, producer, no-data alert và staging drill có bằng chứng.

---

## 9. DWP7 — Mobile release source và store/privacy draft

**Trạng thái:** HOÀN THÀNH artifact bàn giao; signed build/store upload `CHƯA CHẠY`, mobile release project chưa sẵn sàng và ngoài quyền sở hữu.

### 9.1. Thay đổi

- `ci/mobile-release-source.yml`: manual Android/iOS/both, production URL/version preflight, test, signed AAB/IPA, artifact retention hữu hạn; publish tùy chọn chỉ tới Play Internal draft/TestFlight qua protected environment.
- Workflow fail closed nếu Android/iOS ID chưa là `vn.lifeos.app`, Android còn debug signing/TODO hoặc iOS thiếu privacy manifest.
- `MOBILE_RELEASE.md` ghi secret/environment/handoff và blocker đã kiểm chứng.
- `PRIVACY_POLICY_DRAFT.md`, `STORE_LISTING_DRAFT.md` có AI consent/provider/retention/ZDR, export/delete, data-safety worksheet và placeholder con người.
- Không sửa bất kỳ file nào dưới `mobile/` và không sửa `ci/mobile-ci.yml`.

### 9.2. Quyết định phát sinh

- `[CONFIRMED]` (2026-11-11) P-DWP7-01: package/bundle ID production thống nhất `vn.lifeos.app` theo `AppFlavorConfig`; agent mobile phải đổi native project và review migration/store identity trước lần publish đầu.
- `[CONFIRMED]` (2026-11-11) P-DWP7-02: pipeline chỉ upload Play Internal ở trạng thái draft và TestFlight; promotion production luôn là bước con người sau privacy/data-safety/review.
- `[CONFIRMED]` (2026-11-11) P-DWP7-03: application ID/signing/privacy manifest là hard gate, không cho pipeline tạm dùng debug key hoặc bundle ID mẫu.

### 9.3. Kiểm chứng đọc-only

- `pubspec.yaml`: version hiện `1.0.0+1`.
- Android: `applicationId=com.lifeos.lifeos_mobile`, release ký debug, còn TODO.
- iOS: bundle ID `com.lifeos.lifeosMobile`, chưa thấy team/privacy manifest.
- Flavor config dự kiến production `vn.lifeos.app`, API `https://api.lifeos.vn/api/v1`; production diagnostics hiện `true` cần agent mobile/security xác nhận.
- Manifest được review chưa thấy quyền location/camera/microphone; phải re-scan signed binary và dependency manifest trước khai store.

### 9.4. `CHƯA CHẠY`

- SnakeYAML parse workflow thành công; kiểm tĩnh xác nhận workflow không tham chiếu/sửa `ci/mobile-ci.yml` và không chứa API localhost.
- Flutter test/signed AAB/IPA, signature, Play Internal/TestFlight upload; thiếu mobile release config, account, key/certificate và macOS evidence.
- Privacy/legal review, ZDR/account processor list, screenshot/content rating/data-safety submission.

DWP7 dừng ở handoff đúng quyền sở hữu; không tự sửa mobile để vượt preflight.

---

## 10. DWP8 — Production readiness review

**Trạng thái:** REVIEW HOÀN TẤT với kết luận **NO-GO**; go-live rehearsal `BỊ CHẶN` theo §11.5 vì thiếu account/secret/quyền con người và không có live staging thay thế.

### 10.1. Thay đổi

- `PRODUCTION_READINESS.md`: gate/evidence matrix, rehearsal thứ tự, chi phí baseline và điều kiện GO.
- `ROLLBACK.md`: runtime digest rollback, migration failure và restore-to-new-DB; cấm down migration/`down -v`/restore đè.
- `AI_PROVIDER_INCIDENT.md`: kill switch, budget hard stop, key leak/ZDR/privacy escalation.

### 10.2. Kết luận

- Source/artifact DWP1–DWP7 đã chuẩn bị trong phạm vi quyền sở hữu và mọi phần không chạy đều được gắn `CHƯA CHẠY`.
- Không có bằng chứng Render staging/TLS/private DB, GitHub run, image scan, migration job, backup/restore, alert drill, rollback, signed mobile build hoặc store upload.
- Điều kiện chờ AIP2 đã được người dùng gỡ ngày 2026-11-11. Sửa lỗi backend review được **HOÃN**; không có luồng sửa backend song song. Nhánh AI tiếp tục AIP2 → AIP3 → AIP4 → AIP7; AIP5/AIP6 vẫn hoãn.
- Vì DWP8 cần credential/account/live resources và không còn fake/local path có thể thay thế bằng bằng chứng production, dừng tại §11.5 là bắt buộc; không tự provisioning hay yêu cầu secret trong chat.

### 10.3. Chi phí và rủi ro còn lại

- Baseline đã duyệt: Render app+DB 31–40 USD/tháng, AI hard cap 25 USD/tháng; tổng 56–65 USD/tháng trước thuế và chưa gồm domain/SMTP/storage/KMS/egress/observability/store.
- Rủi ro cao nhất: restore chưa chứng minh, private scrape/job metric chưa có producer, workflow chưa chạy/pin digest, trusted proxy CIDR chưa xác minh, mobile dùng ID/debug signing mẫu, privacy/ZDR chưa có xác nhận con người.

### 10.4. `CHƯA CHẠY`

Toàn bộ rehearsal và bằng chứng live trong `PRODUCTION_READINESS.md`. Theo yêu cầu người dùng, đã tạo `DEPLOY_AI_FINAL_REPORT.md` đúng mẫu bàn giao nhưng gắn rõ **MỘT PHẦN — NO-GO**, liệt kê AIP2–AIP7 và mọi live evidence là `CHƯA LÀM/CHƯA CHẠY`; tên file “FINAL” không được hiểu là toàn bộ Deploy/AI đã hoàn thành.

---

## 11. AIP2 — cổng môi trường và baseline bắt buộc (2026-11-11)

**Trạng thái:** CỔNG XANH; được phép bắt đầu code AIP2.

- Toolchain dự án: Microsoft OpenJDK `21.0.13.0.2`, Apache Maven `3.10.0`, Docker Desktop/server `29.8.2`.
- Hai test đã sửa ở AIP1 đều xanh: `OpenApiExportIT` 1/1 và `ApiDocsProdProfileIT` 1/1.
- Tổng baseline: 158 unit + 694 integration = **852 test**, lớn hơn baseline 847 do có 5 test contract/deploy mới; 0 failure, 0 error, 0 skipped.
- Lần đầu bị ngắt do Docker daemon mất phản hồi khi ổ C hết chỗ, không phải failure assertion. Đã xóa đúng crash dump PostgreSQL và ba bộ cài tạm cũ trong `%LOCALAPPDATA%\\Temp`, khởi động lại Docker, rồi chạy sạch lại từ đầu.

Tổng kết nguyên văn của lượt sạch thành công:

```text
[INFO] Tests run: 158, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 694, Failures: 0, Errors: 0, Skipped: 0
[INFO] --- failsafe:3.5.6:verify (default) @ lifeos-backend ---
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  12:24 min
[INFO] Finished at: 2026-11-11T22:56:05+07:00
[INFO] ------------------------------------------------------------------------
```

---

## 12. AIP2 — nền tảng AI, V12, consent, quota và fake provider (2026-11-12)

**Trạng thái:** **MỘT PHẦN (bằng chứng tập trung)**; foundation/V12/consent/quota/fake provider đã có, nhưng AI-04/06..09 phụ thuộc context/chat AIP3 nên chưa đủ toàn bộ acceptance range AIP2. Full-suite 3 lượt liên tiếp là gate cuối AIP7, chưa được tuyên bố tại đây.

### 12.1. Thay đổi

- Thêm duy nhất `V12__ai_assistant.sql`; V1–V11 không đổi. V12 tạo `ai_user_preferences`, `ai_conversations`, `ai_messages`, `ai_usage_logs`, `ai_action_proposals`; mọi bảng đều có `user_id`.
- Đăng ký năm bảng AI vào `UserDataCatalog` theo thứ tự con-trước-cha để export/purge và xóa tài khoản bao phủ FR-AI-11.
- Thêm preferences/usage/limits/delete-data runtime dưới `/api/v1/ai`, consent mặc định tắt, whitelist field và từ chối mass assignment.
- Thêm quota ngày/tháng/5 request-phút, kill switch/budget hard stop, reset UTC và cost integer micro-USD; không dùng `double`.
- Thêm port LifeOS-owned `LLMProvider` và `FakeLLMProvider` profile test; test tự động không gọi mạng hoặc OpenAI thật.
- Thêm AI data store/service/controller và test AI-01/02/03/05/16/17/18/29, migration và catalog.

### 12.2. Bằng chứng đã chạy

Unit tập trung:

```text
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.lifeos.ai.AiQuotaGuardTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.lifeos.ai.FakeLLMProviderTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  01:16 min
[INFO] Finished at: 2026-11-12T15:36:49+07:00
```

Integration tập trung sau khi Docker daemon được khởi động lại:

```text
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in com.lifeos.ai.AiFoundationIT
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in com.lifeos.db.FlywayMigrationIT
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in com.lifeos.db.UserDataCatalogIT
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  01:02 min
[INFO] Finished at: 2026-11-12T15:53:56+07:00
```

Flyway log xác nhận `Successfully validated 12 migrations` và `Successfully applied 12 migrations ... now at version v12`. Lượt sandbox trước đó lỗi chỉ vì không có quyền đọc Docker named pipe; một lượt sau đó chạy tới assertion và phát hiện test AI-03 còn mong 422 trong khi hợp đồng consent là 403. Assertion được sửa về 403 rồi lượt nêu trên xanh. Không sửa route/payload REST/sync cũ.

### 12.3. Hạ tầng host trong quá trình test

- Xóa đúng cache installer tạm cũ `%LOCALAPPDATA%\\Temp\\u4vwq03v` (2,268 GB, có thể tải lại) sau khi xác minh resolved path nằm dưới Temp; không xóa dữ liệu dự án/người dùng.
- Docker Desktop từng treo sau khi ổ C đầy; dừng đúng tiến trình Docker Desktop/backend và mở lại. `docker version` sau restart trả server `29.8.2`.

---

## 13. AIP3 — quyết định tương thích Spring AI và Responses API (2026-11-12/13)

**Trạng thái:** `[RESOLVED]` ngày 2026-11-13; tiếp tục AIP3.

### 13.1. `[RESOLVED] AIP3-SPRING-AI-01`

LifeOS dùng Spring Boot `4.1.0`. Spring AI `2.0.1` là dòng GA tương thích Boot 4.1, nhưng tài liệu/release chính thức xác nhận OpenAI Responses API chỉ được thêm từ Spring AI `2.1.0-M1`; tài liệu 2.0 còn ghi Responses API “not currently supported by this client”. Spring AI 2.1 chuyển baseline sang Spring Boot 4.2 và tại thời điểm kiểm tra mới là milestone/RC, không phải cặp tương thích đã duyệt cho LifeOS.

Điều này xung đột trực tiếp với:

- D261: `OpenAIProvider` phải gọi OpenAI Responses API với `store=false`;
- `AGENTS.md`: chỉ thêm Spring AI ở AIP3 khi tương thích Boot hiện tại; nếu bỏ phải ghi quyết định và xin duyệt trước khi thêm client/dependency khác;
- ranh giới không được âm thầm gọi OpenAI SDK/HTTP trực tiếp.

Bằng chứng chính thức:

- [Spring AI Getting Started — 2.0.x hỗ trợ Spring Boot 4.0.x/4.1.x](https://docs.spring.io/spring-ai/reference/getting-started.html)
- [Spring AI 2.0 OpenAI Chat — Responses API chưa được client hỗ trợ](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/chat/openai-chat.html)
- [Spring AI releases — 2.1.0-M1 thêm OpenAI Responses API và nâng Boot 4.2](https://github.com/spring-projects/spring-ai/releases)
- [Spring AI 2.1 Responses API — `store=false` luôn được gửi](https://docs.spring.io/spring-ai/reference/2.1-SNAPSHOT/api/chat/openai-responses-chat.html)

### 13.2. Quyết định người dùng

`[CONFIRMED]` (2026-11-13) Người dùng yêu cầu tiếp tục theo phương án khuyến nghị số 3: giữ Spring Boot 4.1.0, bỏ Spring AI cho adapter đầu tiên và dùng SDK Java chính thức của OpenAI hoàn toàn sau `LLMProvider`. `OpenAIProvider` bắt buộc gọi Responses API với `store=false`; SDK/type OpenAI không đi qua boundary; test/CI chỉ dùng fake provider và không cần `OPENAI_API_KEY`. AIP5/AIP6 vẫn hoãn.

SDK chính thức được ghim theo tài liệu OpenAI tại thời điểm triển khai; nâng version sau này phải qua dependency review và full verify.

Ngày 2026-11-13, trang tài liệu OpenAI hiển thị `4.103.0` nhưng Maven Central chưa có artifact đó. Metadata chính thức của Central trả `latest=release=4.101.0`, `lastUpdated=20261105204835`; vì vậy implementation ghim `com.openai:openai-java:4.101.0`, không dùng snapshot/repository ngoài.

---

## 14. AIP2 → AIP3 → AIP4 → AIP7 — kết quả thực thi local/fake (2026-11-14)

### 14.1. Trạng thái gói

| Gói | Nhãn | Bằng chứng và phần còn thiếu |
|---|---|---|
| AIP2 | **XONG (bằng chứng)** | V12 giữ nguyên V1–V11; preferences/consent; usage/quota/budget; fake provider; năm bảng AI có `user_id`, đăng ký `UserDataCatalog`, export/purge; AI-01..05, AI-09, AI-16..18 và AI-29 xanh. |
| AIP3 | **MỘT PHẦN** | Chat/history/context/delete và exact-minimal context đã chạy; SDK chính thức `openai-java:4.101.0` nằm sau `LLMProvider`, Responses API luôn `store=false`, không tools; test không network. Chưa có live provider smoke, ZDR xác nhận, circuit-breaker/latency thật. |
| AIP4 | **XONG (bằng chứng)** | Daily/weekly plan, task breakdown, Habit/Finance insight và Note summary chạy read-only bằng fake provider; strict schema/source-ID allowlist/sanitizer/fail-closed; AI-09..15 xanh. |
| AIP7 | **MỘT PHẦN** | Concurrency gate AI-20, retention AI-32, 40 prompt tiếng Việt offline AI-36..38, ArchUnit A15, secret scan và ba lượt full verify liên tiếp xanh. Chưa có live provider/load/metrics/restore/ZDR; OpenAPI runtime còn 14/18 path vì AIP6 hoãn. |

AIP5 (RAG) và AIP6 (action proposal) vẫn **HOÃN** đúng O-AI-06/O-AI-05. Không có code mobile hoặc `ci/mobile-ci.yml` bị sửa.

### 14.2. Implementation và ranh giới đã kiểm chứng

- `OpenAIProvider` dùng Responses API, `store=false`, không tools; client khởi tạo lazy và thiếu key thì fail closed trước network. Type SDK không đi qua `LLMProvider`.
- Provider được gọi ngoài transaction. Các service core Task/Calendar/Habit/Goal/Note/Finance chỉ được đọc qua `*QueryApi`; không thay route/payload REST/sync cũ và không có ghi Finance.
- Context builder kiểm consent trước provider, chỉ gửi field/record tối thiểu, phân biệt finance consent độc lập, kiểm source ID thuộc allowlist của user.
- Output được kiểm strict shape/field/depth/size, sanitize HTML/script/system-prompt/key marker và fail closed nếu response thiếu/không hợp lệ.
- Mọi test AI dùng `FakeLLMProvider` hoặc client giả; không gọi OpenAI thật. Quét literal dạng secret: **0 file**; các occurrence `OPENAI_API_KEY` còn lại chỉ là tên biến, placeholder và vector injection/redaction.

### 14.3. Bản đồ FR-AI → test thực tế

| FR | Test/bằng chứng đã chạy | Trạng thái thật |
|---|---|---|
| FR-AI-01 Consent theo miền | AI-01..04, AI-09 | **XANH** — mặc định off, whitelist/provider terms, thu hồi tức thời, finance độc lập, chặn trước provider. |
| FR-AI-02 Chat read-only có ngữ cảnh | AI-05..08, AI-10..15, AI-20 | **MỘT PHẦN** — runtime/fake, IDOR/mass-assignment/injection/concurrency xanh; chưa live provider/resilience. |
| FR-AI-03 Context disclosure | AI-07..10 | **XANH local/fake** — exact-minimal context, source IDs và redaction; chưa live privacy validation. |
| FR-AI-04 Hội thoại/xóa | AI-04, AI-05, AI-28/29, AI-32 | **XANH local** — cursor/ownership/cascade delete, purge và retention idempotent. |
| FR-AI-05 Kế hoạch/breakdown | AI-12, AI-10..15 | **XANH local/fake** — daily/weekly/breakdown strict schema và read-only. |
| FR-AI-06 Insight | AI-09..15 | **XANH local/fake** — Habit/Finance, consent/allowlist/sanitizer. |
| FR-AI-07 Note summary | AI-10..15 | **XANH local/fake** — hostile Note là untrusted context, output nguy hiểm bị sanitize. |
| FR-AI-08 Usage/quota | AI-16..18 | **XANH** — quota trước provider, kill switch/budget hard stop, integer micro-USD. |
| FR-AI-09 Action proposal | AI-11..15 chỉ bao phủ việc cấm/fail-closed | **CHƯA LÀM runtime** — AIP6 hoãn. |
| FR-AI-10 Approve/cancel | AI-21..27 | **CHƯA LÀM** — AIP6 hoãn. |
| FR-AI-11 Export/delete | AI-28/29/32, `UserDataCatalog`, INT-15 | **MỘT PHẦN** — local export/purge/retention xanh; restore staging chưa chạy. |
| FR-AI-12 Suy giảm êm | AI-18, AI-20, AI-36..38 | **MỘT PHẦN** — local kill/budget/concurrency/eval xanh; chưa live failover/metrics. |

Kết quả AI riêng trong lượt verify cuối: **28/28 test**, 0 failure/error/skipped, gồm 10 suite (`AiConversationIT`, `AiFoundationIT`, `AiGenerationIT`, `AiRetentionIT` và sáu unit/contract/provider suite).

### 14.4. Injection, consent, quota/budget và fake cost/latency

- AI-10: prompt injection trong Note chỉ nằm ở untrusted context, không vào instruction, không bật tools và không lộ marker nhạy cảm.
- AI-11..13: ID ngoài allowlist và extra/malformed field bị từ chối; không ghi assistant message dở.
- AI-14..15: HTML/script/system-prompt/key marker bị sanitize; response thiếu/không hợp lệ fail closed.
- AI-01..04/09: consent mặc định off, provider terms bắt buộc, thu hồi có hiệu lực trước request kế tiếp, Finance độc lập và chặn trước provider.
- AI-16..18: quota minute/day, kill switch và budget chặn trước provider; tiền dùng integer micro-USD, không `double`.
- Fake provider fixture: **11 input token, 7 output token, latency 3 ms**; với giá test 2/10 micro-USD mỗi token, cost là **92 micro-USD**. Đây là fixture giả, không phải benchmark OpenAI/network. Vector 31/7 = 132 micro-USD vẫn xanh.

### 14.5. Ba lượt `mvn -f backend/pom.xml clean verify` xanh liên tiếp

Môi trường: Java `21.0.13.0.2`, Maven `3.10.0`, Docker Desktop/server `29.8.2`, PostgreSQL Testcontainers. Mỗi lượt có **167 unit + 712 integration = 879 test**, lớn hơn baseline 847.

```text
[INFO] Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 712, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  08:32 min
[INFO] Finished at: 2026-11-14T22:31:00+07:00
```

```text
[INFO] Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 712, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  07:02 min
[INFO] Finished at: 2026-11-14T22:38:19+07:00
```

```text
[INFO] Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 712, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  07:05 min
[INFO] Finished at: 2026-11-14T22:45:42+07:00
```

Trước chuỗi trên, một full verify đỏ duy nhất do `MailpitDeliveryIT` timeout SMTP/container 30 giây (711 integration test còn lại xanh). Chạy riêng test này xanh 1/1 trong 31,33 giây; không sửa test ngoài phạm vi. Chuỗi được reset về 0 và ba lượt nêu trên là ba lượt xanh **liên tiếp**.

### 14.6. OpenAPI và test hồi quy đã sửa

- `IdorMatrixIT` nay bao phủ sáu route AI có path variable; victim ID và random ID đều 404 giống nhau, không đổi row và không tạo `change_logs`.
- `OpenApiExportIT` xác nhận chính xác **14 runtime AI path** và **17 runtime operation ID**, đồng thời so byte-equivalent toàn bộ contract ngoài AI. Operation ID của runtime khớp phần AIP1 tương ứng.
- Contract Flutter đã công bố vẫn giữ **18 path, 42 schema, 15 mã lỗi**. Bốn path action proposal thuộc AIP6 chưa có runtime controller vì AIP6 đang hoãn; vì vậy hai marker overlay và ngoại lệ hẹp **chưa được xóa**.

`[CONFIRMED]` (2026-11-14) **AIP7-OPENAPI-01 — chọn phương án (a):** giữ overlay cho đúng bốn path action proposal dưới đây cho tới khi AIP6 được mở lại:

1. `/api/v1/ai/action-proposals`
2. `/api/v1/ai/action-proposals/{id}`
3. `/api/v1/ai/action-proposals/{id}/approve`
4. `/api/v1/ai/action-proposals/{id}/cancel`

Ngoại lệ contract-first trong `OpenApiExportIT` phải được thu hẹp về **đúng bốn path này**; 14 runtime path AIP2–AIP4 và schema/operation ID tương ứng phải tiếp tục được kiểm trực tiếp từ OpenAPI runtime, không nằm trong ngoại lệ. Khi AIP6 được thực hiện và runtime sinh đủ bốn path action proposal, AIP6 phải xóa phần ngoại lệ còn lại và xóa nốt hai marker overlay AIP1. Không thay đổi contract Flutter 18 path/42 schema/15 mã lỗi trong thời gian AIP6 hoãn.

### 14.7. NO-GO live giữ nguyên

Render service/DB/TLS, secret thật, OpenAI project/key và live smoke, xác nhận ZDR, backup/restore staging, private metrics/alert drill, signed AAB/IPA và store submission vẫn **NO-GO chờ con người**. Không có secret thật được đưa vào repo, log hoặc báo cáo.
