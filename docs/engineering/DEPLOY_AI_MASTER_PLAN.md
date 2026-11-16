# LifeOS — Kế hoạch tổng thể Deploy + AI Assistant

> **Trạng thái:** v1 — **ĐÃ DUYỆT 2026-11-10** cho cả Deploy và AI; O-DEP-01..10 và O-AI-01..08 theo phương án khuyến nghị.
> **Phạm vi sở hữu:** đúng bảng §2 của `docs/review/CHATGPT_DEPLOY_AI_PROMPT.md`. Không sửa `mobile/`, `ci/mobile-ci.yml`, `docs/engineering/MOBILE_MASTER_PLAN.md` hay `docs/review/MOBILE_*`.
> **Đặc tả AI:** `docs/phases/PHASE_8_AI_ASSISTANT.md` D260–D299 `[CONFIRMED]`.
> **Baseline:** backend Phase 1–7, V1–V11, 847 test theo báo cáo; trước thực thi vẫn phải tự chạy lại và không coi báo cáo là bằng chứng mới.

Chỉ thị thực thi đã duyệt: công bố AIP1 trước để Flutter có contract; bắt đầu DWP1 ngay; tiếp tục nhánh Deploy theo phụ thuộc. Quan hệ Spring AI phải được ghi trong `AGENTS.md` ngay AIP1; nếu bỏ Spring AI phải có quyết định được duyệt. `[CONFIRMED]` (2026-11-11) Review backend đã có kết quả, các sửa lỗi backend được người dùng **HOÃN**, điều kiện chờ AIP2 được gỡ. Thực thi liên tục AIP2 → AIP3 → AIP4 → AIP7 và chỉ dừng theo §11; AIP5 (RAG) và AIP6 (action proposal) vẫn hoãn theo O-AI-06/O-AI-05. Migration AI dùng **V12** theo D290; V1–V11 bất biến.

---

## 1. Mục tiêu và nguyên tắc

### 1.1. Mục tiêu Deploy

- Có dev, staging và production tách secret/dữ liệu.
- Một người có thể deploy, rollback, quan sát, backup và restore bằng runbook.
- Production fail-fast khi thiếu secret; HTTPS bắt buộc; PostgreSQL không public.
- Migration forward-only, có backup trước thay đổi và restore drill thật.
- CI giữ toàn bộ cổng hiện có, thêm nightly, image scan, deploy có approval/rollback.
- Chuẩn bị đường phát hành Android/iOS mà không chạm mã Flutter thuộc agent khác.

### 1.2. Mục tiêu AI

- Công bố hợp đồng OpenAPI/fixture sớm để Flutter làm song song.
- Xây AI theo consent/privacy/safety-first; provider giả trong CI.
- Chat/planning read-only trước; RAG và action proposal sau feature flag.
- Chi phí có quota, metric, cảnh báo và hard stop.

### 1.3. Bất biến không được phá

- `@Transactional` chỉ ở service; không gọi mạng ngoài transaction.
- Module chỉ phụ thuộc nhau qua `api/`; ArchUnit A1–A14 giữ xanh và bổ sung A15 cho AI.
- `userId` từ `CurrentUser`; mọi route mới có IDOR/mass-assignment test.
- D86: khóa/flush nghiệp vụ trước, `ChangeRecorder.record` cuối; cascade mutation ID `NULL`.
- V1–V11 bất biến; chỉ V12+.
- Không secret/PII/nội dung/số tiền trong log/metric/lỗi.
- Test đồng thời lặp ≥ 20, không `Thread.sleep`, không nới/xóa/disable test.

---

## 2. Quyền sở hữu và sản phẩm bàn giao

### 2.1. Được ghi sau khi duyệt

- `deploy/`, `ops/`, `docs/ops/`, production/staging compose, `Dockerfile`.
- `ci/*` trừ `ci/mobile-ci.yml`; workflow deploy/release dạng nguồn để người dùng chép vào `.github/` nếu cần.
- Module `backend/.../ai`, migration V12+, test AI và thay đổi tối thiểu `*Api` ở module khác.
- `docs/api/openapi.yaml`, `docs/api/fixtures/ai-*.json` chỉ cho hợp đồng AI.
- `docs/review/DEPLOY_AI_PROGRESS.md`, `DEPLOY_AI_FINAL_REPORT.md`.

### 2.2. Chỉ đọc/không đụng

- `mobile/`, `ci/mobile-ci.yml`, tài liệu `MOBILE_*`.
- Backend module khác, trừ `*Api`/command facade tối thiểu phục vụ AI đã nêu trong kế hoạch.
- Tài liệu phase/engineering khác, trừ cập nhật do Definition of Done yêu cầu và chỉ sau khi phần tương ứng được duyệt rõ.

### 2.3. Artifact dự kiến

```text
deploy/
├── compose/
│   ├── compose.staging.yml
│   └── compose.production.yml
├── proxy/                  # Caddy/Nginx cho phương án VPS
├── scripts/                # deploy, migrate, backup, restore, smoke, rollback
└── env/                    # chỉ .example, không secret
ops/
├── prometheus/
├── grafana/
└── alerts/
docs/ops/
├── DEPLOYMENT.md
├── ROLLBACK.md
├── BACKUP_RESTORE.md
├── INCIDENTS.md
├── SECRET_ROTATION.md
├── AI_PROVIDER_INCIDENT.md
├── PRIVACY_POLICY_DRAFT.md
└── STORE_LISTING_DRAFT.md
ci/
├── backend-ci.yml
├── backend-nightly.yml
├── deploy-staging.yml
├── deploy-production.yml
└── mobile-release-source.yml  # nguồn để agent/chủ repo tích hợp, không sửa mobile-ci.yml
```

---

## 3. Môi trường và phương án hosting

### 3.1. Ma trận môi trường

| Môi trường | Dữ liệu | Provider ngoài | Mục đích |
|---|---|---|---|
| Dev | PostgreSQL + Mailpit local, dữ liệu giả | AI fake mặc định; OpenAI smoke opt-in | Phát triển |
| Staging | DB/secret riêng, dữ liệu tổng hợp giả; không copy production | AI fake + OpenAI project/key staging có hard cap nhỏ | Migration, restore, E2E, release candidate |
| Production | DB/backup/secret riêng | SMTP/FCM/OpenAI production | Người dùng thật |

Không dùng free tier có sleep/expiry làm production. Staging có thể dùng free/scale-to-zero nếu chấp nhận cold start và không dùng để kiểm latency.

### 3.2. So sánh phương án

Giá là ước tính công khai tại ngày 2026-11-10, chưa gồm thuế, domain, SMTP, FCM, egress, backup ngoài, store fee và AI; phải xác minh lại lúc mua.

| Phương án | Cấu hình khởi điểm | Ước tính/tháng | Ưu điểm | Nhược điểm |
|---|---|---:|---|---|
| A — **Render PaaS + managed Postgres** | Web service 1 CPU/2 GB 25 USD + Postgres nhỏ từ khoảng 6 USD + storage | **≈31–40 USD** | TLS, private network, deploy/rollback, secret, paid DB có backup/PITR; ít vận hành nhất | Giá cao hơn VPS; disk/export và metrics cần thiết kế theo PaaS; tier DB nhỏ phải benchmark |
| B — **AWS Lightsail VPS + managed DB** | App 2 GB 10 USD + managed DB encrypted 2 GB 30 USD | **≈40 USD** | Chi phí cố định, vùng APAC, DB tách và mã hóa; dễ chuyển dần sang AWS khác | Phải quản proxy/OS/app; DB 1 GB 15 USD được trang giá ghi “No Data encryption” nên không khuyến nghị production |
| C — **Một VPS 4 GB tự chạy app + PostgreSQL** | Lightsail 4 GB khoảng 20 USD; volume/snapshot/offsite backup thêm | **≈20–30 USD** | Rẻ, đúng Docker Compose, kiểm soát đầy đủ | Một điểm lỗi; tự vá OS/DB, backup/restore/TLS/monitoring; RPO/RTO phụ thuộc kỷ luật cá nhân |

Nguồn chính thức: [Render pricing](https://render.com/pricing), [Render compute plans](https://render.com/docs/compute-plans), [Render service/backup notes](https://render.com/docs/service-types), [AWS Lightsail pricing](https://aws.amazon.com/lightsail/pricing/). Render nói rõ free instance không dùng cho production tại [free tier documentation](https://render.com/docs/free).

### 3.3. Đề xuất

`[CONFIRMED]` Chọn **A — Render PaaS + managed Postgres** cho production beta: một instance backend, một managed DB cùng region, một persistent disk chỉ cho `EXPORT_DIR` nếu còn dùng tệp cục bộ. Lý do: người vận hành duy nhất, chi phí chênh nhỏ so với app+managed DB Lightsail, giảm phần việc TLS/OS/DB backup/deploy.

`[CONFIRMED]` Phương án B là đường lui nếu cần APAC/kiểm soát mạng rõ hơn. Phương án C chỉ dùng staging/hobby hoặc khi người dùng chấp nhận tự vận hành và đã restore drill thành công.

### 3.4. Một instance và điều kiện scale

Ở production beta chỉ chạy một backend instance vì:

- rate limiter/revoked-device cache/circuit breaker hiện ở bộ nhớ;
- snapshot HMAC key hiện sinh trong memory theo báo cáo;
- `EXPORT_DIR` là local filesystem;
- scheduled jobs cùng process và một số job chưa có distributed lock.

Trước khi chạy >1 instance phải hoàn thành một gói scale riêng: shared rate limit/revocation, snapshot key cố định từ secret, object storage cho export, kiểm mọi job với `SKIP LOCKED`/advisory lock, shared AI budget/concurrency counter và load test đa instance. Trigger xem xét: CPU >70% hoặc memory >80% kéo dài, p95 vượt SLO sau tối ưu, connection pool pending, hoặc nhu cầu zero-downtime/HA không đáp ứng bằng một instance.

---

## 4. Đóng gói, cấu hình và mạng

### 4.1. Production Dockerfile

`[CONFIRMED]` DWP2 tạo multi-stage image:

1. Builder JDK 21 + Maven; build bằng lock/commit hiện có; không secret trong build args/layer.
2. Runtime JRE 21 tối thiểu, user/group không root, thư mục app chỉ đọc.
3. `TZ=UTC` và `-Duser.timezone=UTC`; `JAVA_TOOL_OPTIONS` có `-XX:MaxRAMPercentage`, heap dump tắt hoặc ghi vào volume bảo vệ.
4. PID 1 nhận SIGTERM; Spring graceful shutdown; stop grace đủ cho request nhưng không kéo dài vô hạn.
5. Healthcheck dùng `/actuator/health/liveness`; platform readiness dùng `/actuator/health/readiness` nếu endpoint hiện tại tách được mà không lộ detail.
6. Không shell/tool thừa trong runtime nếu healthcheck do platform thực hiện.
7. Pin image bằng digest/phiên bản; quét CVE; sinh SBOM/provenance nếu CI hỗ trợ.

Giới hạn khởi điểm cần benchmark: app 1 CPU/2 GB, heap tối đa khoảng 65–70% container, Hikari nhỏ (ví dụ 10) và scheduler 4 thread như hiện tại. Đây là cấu hình `[CONFIRMED]` để khởi điểm, không coi là SLO trước load test.

### 4.2. Compose/profile production

- Không dùng `backend/docker-compose.yml` dev cho production.
- `compose.production.yml` không chứa password mặc định, không public PostgreSQL, không Mailpit.
- VPS: Caddy/Nginx kết thúc TLS, chỉ mở 80/443; backend/private DB ở network nội bộ; SSH allowlist/key-only.
- PaaS: dùng TLS/load balancer của nền tảng; không thêm reverse proxy container không cần thiết.
- `EXPORT_DIR` trỏ volume/persistent disk riêng; root filesystem read-only nếu platform cho phép; `/tmp` có quota.
- Resource limits, restart policy, log rotation và health/readiness bắt buộc.

### 4.3. Secret và biến môi trường

Nhóm bắt buộc production:

| Nhóm | Biến/secret |
|---|---|
| DB app | `DB_URL`, `DB_USER`, `DB_PASSWORD` (DML, không DDL) |
| DB migration | `MIGRATION_DB_URL`, `MIGRATION_DB_USER`, `MIGRATION_DB_PASSWORD` (chỉ pre-deploy) |
| JWT | `JWT_SECRET`, `JWT_KID`, khóa previous khi xoay |
| Notification | `NOTIFICATION_UNSUBSCRIBE_SECRET` riêng JWT; `API_BASE_URL` |
| Mail | `SMTP_*`, `MAIL_FROM`, `APP_BASE_URL` |
| FCM | `FCM_ENABLED`, project/client email/private key/token URI |
| Export | `EXPORT_DIR`, max bytes |
| AI | `OPENAI_API_KEY`, project/org nếu cần, model allowlist, budget/quota/feature flags |
| Ops | credential scrape/Grafana/backup/object storage nếu có |

Quy tắc:

- Prod không khởi động nếu thiếu/placeholder/độ dài sai; secret dev/staging/prod khác nhau.
- Secret chỉ từ secret manager/platform; `.env` chỉ local và gitignored; repo chỉ có `.example`.
- Xoay JWT dùng hai khóa; các secret khác có runbook dual-deploy hoặc maintenance window.
- OpenAI/SMTP/FCM key không xuất hiện trong image, crash dump, diagnostic endpoint hoặc mobile.

### 4.4. TLS, proxy và header

- HTTPS bắt buộc, redirect HTTP, HSTS sau khi domain ổn định, TLS hiện đại do platform/proxy quản.
- `TRUSTED_PROXIES` phải khớp chính xác dải proxy/platform; không dùng `.*`/trust-all.
- Test staging gửi `X-Forwarded-For` trực tiếp tới backend và qua proxy để chứng minh chỉ proxy tin cậy được ảnh hưởng IP rate-limit.
- CORS tiếp tục tắt cho native app; chỉ mở allowlist nếu có web client được duyệt sau.
- Security headers: `nosniff`, no-store cho dữ liệu cá nhân/token/AI, ẩn server version; CSP chỉ cần nếu có web page.
- Giữ body 1 MB, Note 192 KiB, sync payload 64 KiB/batch 50; proxy phải có limit không thấp hơn app một cách gây lỗi mơ hồ và không cao vô hạn.

---

## 5. Dữ liệu, migration, backup và restore

### 5.1. Migration production

`[CONFIRMED]` Trình tự deploy:

1. CI kiểm migration checksum và additive/backward compatibility.
2. Backup/snapshot pre-deploy và xác nhận dung lượng/restore point.
3. Pre-deploy migration job dùng DB role DDL riêng; chỉ một job; timeout/lock monitoring.
4. App mới khởi động với app role DML và `spring.flyway.enabled=false` ở runtime production.
5. Smoke health/auth/schema; sau đó mới chuyển traffic.

V12 AI là additive. Rollback app chỉ được phép khi schema vẫn backward-compatible. Không “down migration” tự động. Nếu migration phá dữ liệu hoặc app cũ không tương thích: dừng, restore vào DB mới, xác minh, rồi chuyển kết nối; không ghi đè production DB chưa xác minh.

### 5.2. Backup policy đề xuất

| Dữ liệu | Chính sách beta `[CONFIRMED]` | Kiểm chứng |
|---|---|---|
| PostgreSQL | Managed PITR nếu tier hỗ trợ + nightly encrypted logical dump ra nơi khác region/account; giữ 7 daily, 4 weekly, 6 monthly | Restore staging mỗi tháng và trước release lớn |
| Export files | Dữ liệu tạm 24h; dọn quá hạn/mồ côi; không coi là backup | Job + metric orphan/age |
| Secret/config | Secret manager + inventory/version, không nằm trong DB dump | Rotation drill |
| Grafana/alerts/runbook | Mã hóa trong repo | CI validate JSON/config |

Mục tiêu đề xuất beta: **RPO ≤ 24 giờ** ngay cả khi PITR hỏng; **RTO ≤ 4 giờ**. Nếu managed PITR vận hành đúng, mục tiêu nội bộ tốt hơn là RPO ≤ 1 giờ/RTO ≤ 2 giờ. Người dùng chốt tại O-DEP-04.

### 5.3. Restore drill

Runbook phải thực sự chạy:

1. Tạo PostgreSQL mới ở staging/cô lập.
2. Restore dump/PITR; chạy `flyway validate`, `ddl-auto=validate` và smoke read-only.
3. Kiểm row count/constraint, user isolation, Finance balance mẫu, sync cursor, notification/outbox, export catalog và bảng AI.
4. Ghi thời gian bắt đầu/kết thúc, RPO thực tế, checksum dump, lỗi và bước sửa vào `DEPLOY_AI_FINAL_REPORT.md`.
5. Xóa an toàn bản restore sau khi bằng chứng hoàn tất.

Không được tuyên bố backup hoạt động chỉ vì job `pg_dump` trả exit 0.

### 5.4. pgvector/RAG

Theo D292, production ban đầu không cần pgvector. Nếu AIP5 được duyệt:

- dùng image/managed PostgreSQL hỗ trợ đúng extension version;
- thử enable/upgrade/backup/restore ở staging trước V13;
- đo index build, disk, query plan và deletion;
- không triển khai nếu host không bảo đảm restore extension.

---

## 6. CI/CD và rollback

### 6.1. Pull request gate

Mở rộng `ci/backend-ci.yml` nhưng giữ cổng hiện tại:

- Maven `clean verify`, ArchUnit, Testcontainers, OpenAPI drift, migration immutability.
- Secret scan, dependency review, Maven dependency/image scan mức High/Critical.
- Build production image, SBOM, scan image, chạy container smoke với PostgreSQL tạm.
- AI contract/fixture/schema tests; test chứng minh CI không cần `OPENAI_API_KEY` và không ra Internet.
- Không deploy từ PR không tin cậy hoặc fork có secret.

### 6.2. Nightly

`ci/backend-nightly.yml`:

- `mvn ... verify -Dlifeos.sync.simulation.full=true`.
- `BackendBenchmark` và test đồng thời ≥20; không chặn PR nhưng đỏ nightly phải có cảnh báo.
- Backup/restore smoke theo lịch phù hợp; dependency/image scan đầy đủ.
- AI fake-provider load, cost vector, safety/eval nhỏ; không gọi provider thật.
- Lưu report/benchmark có retention hữu hạn, không chứa PII.

### 6.3. Staging deploy

- Trigger sau merge main hoặc manual; build image một lần, ký/tag bằng commit SHA.
- Pre-deploy backup + migration, deploy image SHA, smoke test, OpenAPI compatibility.
- OpenAI staging smoke chỉ manual, dữ liệu giả, budget/key staging, ghi kết quả `CHƯA CHẠY` nếu thiếu key.

### 6.4. Production deploy

- Manual approval bắt buộc, dùng **chính image digest đã qua staging**.
- Kiểm backup/restore point, migration compatibility, secret/feature flag, alert silence window.
- Deploy một instance có maintenance/rolling strategy của platform; smoke health/login/sync/job lag.
- Rollback app bằng image digest trước. Rollback DB chỉ qua restore-to-new-instance khi thật sự cần.
- Feature AI, RAG và actions mặc định tắt; bật theo rollout nhỏ sau khi quan sát.

### 6.5. `.github/` và quyền sở hữu

Nếu agent không được ghi `.github/`, mọi workflow hoàn chỉnh nằm trong `ci/` và báo cáo ghi lệnh/path để người dùng chép:

```text
ci/backend-ci.yml          → .github/workflows/backend-ci.yml
ci/backend-nightly.yml     → .github/workflows/backend-nightly.yml
ci/deploy-staging.yml      → .github/workflows/deploy-staging.yml
ci/deploy-production.yml   → .github/workflows/deploy-production.yml
```

Không tuyên bố workflow đã chạy trên GitHub cho đến khi có run URL/kết quả thật.

---

## 7. Quan sát, cảnh báo và runbook

### 7.1. Metric/dashboard

Giữ dashboard `ops/grafana/lifeos-backend.json`, bổ sung:

- `lifeos.job.last_success{job}` và `last_failure` cho recurring, retention, purge, export, backup nếu scrape được.
- Queue/lag/error outbox, reminder, delivery, recurring, export, account purge.
- DB Hikari pending, connections, slow query; JVM heap/GC/thread; HTTP p50/p95/p99/5xx.
- AI: requests/result, provider p95, timeout/429/5xx, circuit state, inflight, schema rejection, consent rejection, quota/budget, token/cost, action outcome.
- Backup age/last successful restore drill và disk/export orphan age.

Không label `userId`, entity/conversation/message/proposal ID, email, prompt, model output hay amount.

### 7.2. Alert khởi điểm `[CONFIRMED]`

- 5xx >1%/5 phút; health/readiness fail; DB pending >0/5 phút.
- `job.last_success` quá 3 chu kỳ; outbox FAILED tăng; reminder lag >3 chu kỳ.
- Backup age >26 giờ; restore drill quá hạn >35 ngày; disk >80/90%.
- AI provider error >10%/10 phút, p95 >25 giây, circuit mở >5 phút.
- AI cost 50/80/100% ngân sách; quota rejection tăng bất thường; action FAILED >0 cảnh báo trung bình.
- Không gửi alert chứa prompt/context/user data.

Các ngưỡng trên là baseline đã duyệt; điều chỉnh bằng dữ liệu thật phải ghi quyết định mới `[PROPOSED]` trong progress log.

### 7.3. Runbook bắt buộc

- Deploy từ máy/runner sạch; rollback image; migration failure.
- Backup, restore, kiểm tính đúng, chuyển DB endpoint.
- PostgreSQL đầy disk/connection; outbox/reminder/job ngừng; export mồ côi.
- Xoay JWT, notification HMAC, DB, SMTP, FCM, OpenAI key.
- Provider AI timeout/hết budget/lộ key: tắt `lifeos.ai.enabled`, revoke key, kiểm log, thông báo.
- Incident privacy/data leak: cô lập, bảo toàn bằng chứng không chứa secret, đánh giá phạm vi, xóa/rotate.

---

## 8. Kế hoạch phát hành ứng dụng

Phần này chỉ chuẩn bị pipeline/tài liệu; không sửa Flutter do ranh giới sở hữu.

### 8.1. Android

- Agent mobile cung cấp build/flavor/application ID/version và bảo đảm production dùng API URL thật.
- Build `aab` release bằng keystore trong secret store; không commit keystore/password.
- Google Play: Internal testing → Closed testing → Production; ký Play App Signing; lưu mapping/symbol nếu có.
- FCM production `google-services.json` do người dùng/agent mobile tích hợp; backend service account riêng, quyền tối thiểu.

### 8.2. iOS

- Cần macOS runner hoặc Mac của người dùng; chứng chỉ/provisioning/App Store Connect API key trong secret store.
- Archive/sign/upload TestFlight → nhóm internal/external → App Store.
- APNs/FCM cấu hình production, entitlement và privacy manifest do agent mobile xác minh.

### 8.3. Version và tương thích

- SemVer/app build number đơn điệu; backend ghi release tương thích.
- `minSupportedClientVersion` chỉ nâng sau khi bản mới đã phát hành đủ và có kế hoạch force-update; 426 không xóa local data.
- Staged rollout/canary store; theo dõi crash, auth/sync 4xx/5xx trước 100%.

### 8.4. Privacy/store listing draft

Draft phải nêu:

- loại dữ liệu LifeOS lưu và mục đích;
- AI mặc định tắt theo miền, Finance/Notes riêng, dữ liệu gửi OpenAI, không dùng huấn luyện theo mặc định, retention provider áp dụng/ZDR nếu có;
- context disclosure, quyền thu hồi consent, xóa AI, xuất/xóa tài khoản;
- AI có thể sai và không tư vấn/tự thực hiện giao dịch tài chính;
- retention hội thoại/usage/proposal, contact privacy/support;
- quyền OS: notification, network; không tuyên bố quyền chưa dùng.

Tài sản con người phải cung cấp: tên pháp nhân/cá nhân, contact/privacy URL, icon/screenshots, mô tả, content rating, support URL, account xóa, tài khoản developer và phí store.

---

## 9. Gói công việc và thứ tự

### 9.1. Deploy work packages

| Gói | Nội dung | Phụ thuộc | Bằng chứng hoàn thành |
|---|---|---|---|
| DWP1 | Chốt hosting/region/domain/RPO-RTO/secret inventory/SLO; tạo progress log | Duyệt Deploy | Quyết định ghi rõ, không secret |
| DWP2 | Production Dockerfile, prod/staging compose/profile, fail-fast config, TZ UTC, migration role/job | DWP1 | Image non-root; container smoke; thiếu secret không start |
| DWP3 | TLS/proxy/trusted proxy/domain/staging provisioning | DWP2 + tài khoản con người | HTTPS, XFF test, DB không public, staging health |
| DWP4 | Backup/restore/orphan cleanup/data lifecycle | DWP3 | Restore drill thật và thời gian/RPO/RTO trong report |
| DWP5 | CI PR/nightly, image scan/SBOM, staging/prod deploy + rollback source | DWP2–4 | Pipeline chạy hoặc ghi `CHƯA CHẠY` + bước chép `.github/` |
| DWP6 | Prometheus/Grafana, alerts, job gauge, AI metrics skeleton, runbooks | DWP3 | Dashboard/alert validation, incident drills bàn giấy |
| DWP7 | Store release pipeline source + privacy/store drafts | Agent mobile cung cấp project release | Android pipeline; iOS ghi rõ macOS/human requirement |
| DWP8 | Production readiness review/go-live rehearsal | DWP1–7 | Checklist, restore, rollback, smoke, cost estimate |

### 9.2. AI packages

| Gói | Nội dung | Phụ thuộc | Bằng chứng hoàn thành |
|---|---|---|---|
| AIP1 | **Công bố sớm** OpenAPI `/ai`, error codes, fixtures và contract tests | D260–D299 được duyệt | Flutter contract có trước backend implementation |
| AIP2 | V12, export/purge catalog, preferences, usage/quota/budget, fake provider | AIP1 | Migration clean/upgrade; INT-15; AI-01..09/16..18 |
| AIP3 | LLMProvider/OpenAIProvider, chat read-only, exact context, history/delete | AIP2 + key chỉ cho smoke | AI-07..20; CI không network; no transaction external call |
| AIP4 | Daily/weekly plan, breakdown, Habit/Goal/Finance insight, Note summary | AIP3 | Consent/schema/injection tests từng mode |
| AIP5 | RAG addendum, pgvector V13+, indexing/deletion/restore | O-AI-06 duyệt + DWP4 | Benchmark/restore/delete xanh; có thể bỏ qua |
| AIP6 | Proposal/action Task allowlist | AIP3–4, feature flag off | Concurrency/idempotency/version/forbidden tests xanh 3 lượt |
| AIP7 | Load/eval/hardening/privacy/runbook/final report | AIP1–6 + DWP6 | Full verify 3 lượt, report không tuyên bố test chưa chạy |

### 9.3. Thứ tự tích hợp

```text
Duyệt tài liệu
  ├─ DWP1 → DWP2 → DWP3 → DWP4 → DWP5 → DWP6 → DWP8
  └─ AIP1 → AIP2 → AIP3 → AIP4 → (AIP5 tùy chọn) → AIP6 → AIP7

DWP2 cung cấp secret/config/image cho AIP3
DWP4 phải bao phủ bảng AI từ AIP2 và pgvector nếu AIP5
DWP6 phải sẵn trước khi bật AI production
DWP7 chạy song song khi agent mobile bàn giao release project
```

Không chờ production account để làm provider fake, contract, schema và test. Không chờ store account để chuẩn bị checklist/pipeline source.

`[CONFIRMED]` AIP1 giữ Spring AI làm toolkit nội bộ dự kiến của `OpenAIProvider` ở AIP3, luôn nằm sau port `LLMProvider`; AIP1 chưa thêm dependency runtime. `[CONFIRMED]` (2026-11-13) AIP3 đã chứng minh Spring AI 2.0.x không có Responses API còn dòng 2.1 yêu cầu baseline Spring Boot 4.2; theo phương án người dùng duyệt, LifeOS giữ Spring Boot 4.1.0 và adapter đầu tiên dùng SDK chính thức `com.openai:openai-java:4.101.0` hoàn toàn sau `LLMProvider`, bắt buộc `store=false`, không để type SDK đi qua boundary. Quyết định và bằng chứng nằm tại `docs/review/DEPLOY_AI_PROGRESS.md` §13 và `AGENTS.md` §4.

---

## 10. Thay đổi hợp đồng được phép sau duyệt

1. Thêm `/api/v1/ai/...`, schema/error code/fixture AI theo Phase 8.
2. Thêm V12+ và module `ai`.
3. Thêm read `*QueryApi` hoặc command facade tối thiểu ở module khác chỉ để AI đọc/thực thi allowlist; không đổi REST/sync hiện có.
4. Thêm cấu hình/metric/health cần cho deploy và AI, không lộ chi tiết.
5. Thêm `UserDataCatalog` entry cho bảng AI; không thay hành vi xuất/xóa ngoài việc bao phủ dữ liệu mới.
6. Thêm A15 ArchUnit và test AI/ops.

Bất kỳ thay đổi nào khác đối với `[CONFIRMED]`, V1–V11, payload sync, Finance, mobile hoặc API cũ phải dừng và xin duyệt riêng.

---

## 11. Điều kiện dừng trong giai đoạn thực thi

Chỉ dừng khi:

1. Tài liệu đã duyệt mâu thuẫn và không có thứ tự ưu tiên giải quyết.
2. Cần đổi hợp đồng `[CONFIRMED]` ngoài §10.
3. Cần quyết định không hoàn tác (xóa/restore/đổi PK/migration phá dữ liệu) mà hai hướng đều hợp lý.
4. Test bắt buộc vẫn đỏ sau 3 lần sửa nguyên nhân gốc.
5. Thiếu tài khoản/quyền/secret con người và không có fake/staging/local path thay thế.
6. Backup/restore hoặc migration có dấu hiệu mất dữ liệu; lập tức ngừng deploy.

Không dừng vì chi tiết nhỏ có phương án an toàn, hoàn tác được và nhất quán; ghi chúng `[PROPOSED]` trong progress log.

---

## 12. Chuẩn hoàn thành

### 12.1. Mỗi DWP/AIP

- Test/smoke liên quan xanh hoặc ghi rõ `CHƯA CHẠY`, lý do và lệnh.
- Không secret/PII; ownership đúng; progress log cập nhật.
- Tài liệu/runbook/contract cùng gói không lệch code.
- Rollback/cleanup được mô tả và thử tương xứng rủi ro.

### 12.2. Deploy hoàn thành

- Deploy từ máy/runner sạch, TLS/health/readiness/fail-fast/least privilege đúng.
- Backup tự động và **restore thật** đạt RPO/RTO đã duyệt.
- CI PR/nightly/image scan/deploy/rollback chạy hoặc có bàn giao `.github/` chính xác.
- Dashboard/alert/runbook/secret rotation/incident response sẵn sàng.
- Chi phí tháng thực tế/ước tính được ghi.

### 12.3. AI hoàn thành

- D260–D299 và các lựa chọn O-AI đã duyệt.
- Consent/context disclosure/injection/schema/IDOR/action/quota/delete/export test xanh.
- CI dùng fake provider; production feature flags mặc định an toàn.
- OpenAPI/fixture công bố cho Flutter; bảng AI trong backup/export/purge.
- `mvn clean verify` xanh 3 lượt; test cũ sửa phải nêu lý do.

### 12.4. Báo cáo cuối

`docs/review/DEPLOY_AI_FINAL_REPORT.md` gồm: bảng gói/test/thời gian/`CHƯA CHẠY`; migration; cách deploy sạch và rollback; restore drill; secret/human task; chi phí hạ tầng + AI; FR-AI→AI-xx; contract Flutter; consent matrix; action allow/deny; injection evidence; cost/latency; export/delete; quyết định phát sinh/rủi ro/việc hoãn.

---

## 13. Việc chỉ con người làm được

| Việc | Cần lúc | Đường không bị chặn |
|---|---|---|
| Chọn/mua domain, DNS | DWP3 | Dùng hostname staging/platform tạm |
| Tạo hosting/DB, billing và region | DWP3 | Compose local + artifact IaC/config |
| Tạo OpenAI project/key, spend limit, ZDR request | AIP3 smoke/production | Fake provider trong toàn bộ CI/dev |
| SMTP production + SPF/DKIM/DMARC | DWP3/go-live | Mailpit/stub ở dev/staging |
| Firebase/FCM và APNs | DWP7/go-live | FCM disabled, local/in-app notification |
| Google Play account/keystore/Play App Signing | DWP7 | Build debug/internal artifact source |
| Apple Developer/App Store Connect/Mac | DWP7 | Chuẩn bị pipeline/checklist; ghi `CHƯA CHẠY` |
| Privacy/legal identity/support contact | DWP7/go-live | Draft có placeholder rõ ràng |
| Chép workflow vào `.github/` và đặt secrets | DWP5 | Bàn giao file trong `ci/` + hướng dẫn |

Không đưa credential thật vào chat, issue, commit hoặc báo cáo.

---

## 14. Quyết định `[RESOLVED]` ngày 2026-11-10

Người dùng đã duyệt O-DEP-01..10 theo toàn bộ phương án khuyến nghị dưới đây.

| ID | Câu hỏi | Khuyến nghị |
|---|---|---|
| O-DEP-01 | Hosting production | **Render web service + managed Postgres**, một instance |
| O-DEP-02 | DB production managed hay tự chạy | **Managed**; không dùng tier ghi “No Data encryption” |
| O-DEP-03 | Region/domain | **Region gần người dùng và có cả app+DB cùng region**; domain riêng, staging subdomain riêng |
| O-DEP-04 | RPO/RTO beta | **RPO 24h/RTO 4h**, theo dõi mục tiêu tốt hơn 1h/2h khi PITR sẵn |
| O-DEP-05 | Kho tệp export | **Persistent disk cục bộ khi một instance**; chuyển object storage trước multi-instance |
| O-DEP-06 | Open Questions backend: `TZ=UTC`, nightly, job gauge | **Duyệt cả ba** |
| O-DEP-07 | Staging | **Luôn có DB riêng và dữ liệu giả**; compute có thể nhỏ/sleep, không dùng đo SLO |
| O-DEP-08 | Ngân sách AI production beta | **25 USD/tháng**, cảnh báo 80%, hard stop 100% |
| O-DEP-09 | ZDR cho Notes/Finance | **Yêu cầu ZDR trước khi bật production**; xem O-AI-03 |
| O-DEP-10 | RAG/action trong release AI đầu | **Hoãn cả hai**; chat/planning read-only trước |

Open Questions `BACKEND_FINAL_REPORT.md` §12 được xử lý như sau trong kế hoạch:

- Kho export: O-DEP-05.
- `TZ=UTC`: duyệt ở O-DEP-06 và DWP2.
- CI nightly: duyệt ở O-DEP-06 và DWP5.
- Gauge job: duyệt ở O-DEP-06 và DWP6.
- Multi-instance/object storage: hoãn tới trigger §3.4.
- FR-GOAL-06 và sửa module list `AGENTS.md`: ngoài phạm vi Deploy/AI, không tự sửa.
- 75 quyết định backend `[PROPOSED]`: không tự duyệt trong kế hoạch này.

Thay đổi các lựa chọn này cần quyết định mới được duyệt và phải được ghi trong progress log.

