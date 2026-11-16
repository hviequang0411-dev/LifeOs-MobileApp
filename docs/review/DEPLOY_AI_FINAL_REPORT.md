# LifeOS — Báo cáo Deploy + AI Assistant

> **Trạng thái:** **AIP2 XONG (bằng chứng); AIP3 MỘT PHẦN; AIP4 XONG (bằng chứng); AIP7 MỘT PHẦN — NO-GO production** (cập nhật 2026-11-14).  
> **Phạm vi đã thực hiện:** AIP1–AIP4 local/fake, hardening AIP7 local và artifact/source/runbook DWP1–DWP8; AIP5/AIP6 vẫn hoãn.  
> **Đang chờ quyết định:** `[OPEN] AIP7-OPENAPI-01` về bốn path action proposal thuộc AIP6; provisioning/live rehearsal, provider/ZDR thật và phát hành mobile vẫn chưa thực hiện.  
> **Điều kiện đã gỡ:** review backend đã có kết quả; sửa lỗi backend được người dùng hoãn. Phần live vẫn cần account, quyền và secret con người mà không có fake/local path thay thế bằng bằng chứng production.  
> **Nhật ký chi tiết:** `docs/review/DEPLOY_AI_PROGRESS.md`. Báo cáo này dùng tên file bàn giao theo mẫu, không có nghĩa toàn bộ Deploy/AI đã hoàn thành.

---

## 1. Tóm tắt

- AIP1 đã công bố contract-first cho Flutter: 18 path AI, 21 operation ID, 42 schema AI, 15 mã lỗi và 8 fixture JSON. Runtime hiện sinh 14/18 path và 17 operation ID; bốn path AIP6 vẫn chỉ ở overlay vì action proposal đang hoãn.
- Quan hệ Spring AI đã được giải quyết: giữ Spring Boot 4.1.0, adapter đầu tiên dùng SDK chính thức `openai-java:4.101.0` hoàn toàn sau `LLMProvider`, Responses API `store=false`, không tools và không để type SDK đi qua boundary.
- DWP1–DWP7 đã có artifact hạ tầng, CI/CD source, backup/restore script, observability, runbook, mobile release source và privacy/store draft.
- DWP8 đã review readiness và kết luận **NO-GO**. Chưa có Render staging/DB/TLS, restore drill, GitHub run, image scan, rollback live, private scrape, signed AAB/IPA hoặc store upload.
- Không sửa `mobile/`, `ci/mobile-ci.yml` hoặc route/payload REST/sync cũ. AIP2 → AIP4 và phần local AIP7 đã chạy; AIP5/AIP6 vẫn hoãn.
- Migration AI được giữ đúng số **V12**; V1–V11 bất biến; test/CI không gọi provider thật.

## 2. Bảng gói, test, thời gian và phần chưa chạy

Thời gian wall-clock từng gói không được ghi tự động trong phiên nên cột thời gian để `KHÔNG ĐO`; báo cáo không suy đoán số phút. Thời gian lệnh Maven/Docker/live cũng không có vì các lệnh đó chưa chạy.

| Gói | Trạng thái | Bằng chứng đã chạy | Thời gian | `CHƯA CHẠY` / phần thiếu |
|---|---|---|---|---|
| AIP1 | XONG contract-first | 8/8 fixture parse; OpenAPI YAML parse; 41 AI `$ref` resolve; 21 operation ID không trùng; `AiContractPublicationTest` 2/2, `OpenApiExportIT` 1/1, `ApiDocsProdProfileIT` 1/1; baseline 852/852 | `mvn clean verify`: 12:24 | Runtime endpoint chưa có trước AIP2 |
| DWP1 | XONG tài liệu/quyết định | Đối chiếu baseline, SLO, secret inventory không chứa giá trị | KHÔNG ĐO | Account, region/domain, SLO probe thật |
| DWP2 | XONG artifact | YAML compose/prod config parse; PowerShell preflight parse; env giả hợp lệ và placeholder bị từ chối | KHÔNG ĐO | Java 21/Maven/Docker build, image scan, container smoke, fail-fast trong container |
| DWP3 | XONG runbook; live chưa làm | Review cấu hình/provisioning và test XFF hiện có trong source | KHÔNG ĐO | Render service/DB, private network, TLS/DNS/HSTS, XFF spoof live |
| DWP4 | XONG source script/runbook | Bản script trước hardening cuối từng đạt `bash -n` 3/3 và từ chối thiếu env; kiểm tĩnh guard cleanup | KHÔNG ĐO | Bản hiện tại cần chạy lại `bash -n`; backup/object/KMS/lifecycle, restore DB mới, RPO/RTO, orphan apply |
| DWP5 | XONG workflow source | 4/4 workflow YAML parse; quét tĩnh không secret literal/image `latest`/path mobile; thiết kế Render API đối chiếu docs | KHÔNG ĐO | GitHub runs, SBOM/Trivy, GHCR digest, migration one-off, staging/prod deploy/rollback |
| DWP6 | XONG consumer artifact; producer bị chặn | 8/8 YAML ops parse; 2/2 dashboard JSON parse; future rules không load | KHÔNG ĐO | `promtool`, Docker Compose, private scrape, job/AI producer, fire/recover/no-data/disk alert drill |
| DWP7 | XONG handoff artifact | Workflow YAML parse; static guard không chạm `mobile-ci`/localhost; đọc-only native release config | KHÔNG ĐO | Mobile release config, signed AAB/IPA, Play Internal/TestFlight, legal/privacy/store submission |
| DWP8 | Review xong: NO-GO | Readiness/evidence matrix; 21/21 file bàn giao khớp SHA-256; 9/9 YAML và 2/2 JSON parse tại repo | KHÔNG ĐO | Toàn bộ rehearsal live và sign-off GO |
| AIP2 | **XONG (bằng chứng)** | V12/catalog/export-purge/preferences/consent/quota/budget/fake; AI-01..05/09/16..18/29 xanh | Nằm trong full verify | Không có phần local/fake bắt buộc còn thiếu |
| AIP3 | **MỘT PHẦN** | Official SDK adapter; chat/history/context/delete; exact-minimal context; CI không network | Nằm trong full verify | Live provider smoke/ZDR/circuit breaker/latency thật |
| AIP4 | **XONG (bằng chứng)** | Plan ngày/tuần, breakdown, Habit/Finance insight, Note summary; AI-09..15 xanh | Nằm trong full verify | Không có phần local/fake bắt buộc còn thiếu |
| AIP5 | HOÃN theo O-AI-06 | Không | — | RAG/pgvector chỉ sau addendum, benchmark và restore ổn định |
| AIP6 | HOÃN theo O-AI-05 | Không | — | Task action proposal, feature flag mặc định tắt |
| AIP7 | **MỘT PHẦN** | AI-20/32/36..38, A15, secret scan; 3 full verify liên tiếp, mỗi lượt 879 test xanh | 08:32; 07:02; 07:05 | Live provider/load/metrics/restore/ZDR và `[OPEN] AIP7-OPENAPI-01` |

### 2.1. Môi trường kiểm chứng

Đã cài và kiểm chứng Microsoft OpenJDK `21.0.13.0.2`, Apache Maven `3.10.0` và Docker Desktop/server `29.8.2`. Ngày 2026-11-14, `mvn -f backend/pom.xml clean verify` xanh ba lượt liên tiếp: 167 unit + 712 integration = **879 test/lượt**, 0 failure/error/skipped. `promtool`, `shellcheck` và các bằng chứng live riêng của Deploy vẫn ghi `CHƯA CHẠY` nếu chưa chạy thực tế.

## 3. Migration V12+

| Migration | Trạng thái | Ghi chú |
|---|---|---|
| V12 AI | **ĐÃ TRIỂN KHAI VÀ TEST** | D290 được giữ nguyên; V1–V11 bất biến; Flyway clean/upgrade 9/9 xanh |
| V13 pgvector/RAG | **HOÃN** | O-AI-06: chỉ xem xét sau số đo chat và restore staging ổn định |

V1–V11 không bị sửa. Migration image hiện chỉ đóng gói migration đã có và chạy Flyway qua one-off job/role DDL riêng. Không có down migration tự động.

---

## 4. Hạ tầng, triển khai và vận hành

### 4.1. Artifact đã viết

- `backend/Dockerfile`: target runtime non-root và target migrations tách biệt.
- `backend/src/main/resources/application-prod.yml`: runtime Flyway tắt, UTC, Hikari/graceful shutdown và production config fail-fast.
- `deploy/compose.production.yml`, `deploy/compose.staging.yml`, env example, preflight và Render provisioning/deploy script.
- CI source: backend PR/nightly, staging/production deploy và mobile release; chưa chép/chạy dưới `.github/workflows/`.
- Backup/restore/orphan cleanup, Prometheus/Grafana, core/future rules, dashboard operations/AI skeleton.
- Runbook: deployment, CI/CD, backup/restore, observability/alerts, incident, secret rotation, rollback, AI provider incident, mobile release và production readiness.

### 4.2. Cách triển khai từ máy/runner sạch

1. Cài Java 21, Maven/Docker và checkout đúng commit; chạy `mvn -f backend/pom.xml clean verify`.
2. Build target `runtime` và `migrations`; sinh SBOM, scan High/Critical và ghi immutable digest.
3. Tạo Render app/managed PostgreSQL staging cùng region; DB không public; runtime role chỉ DML, migration role DDL; đặt secret qua platform.
4. Chạy backup staging, kiểm object/checksum/KMS; chạy migration image bằng private one-off job, rồi deploy runtime digest.
5. Smoke readiness/auth/sync/outbox/jobs/export; kiểm trusted proxy/XFF và private metrics scrape.
6. Restore dump vào DB mới, validate schema/data/isolation và đo RPO/RTO; rehearsal rollback về runtime digest trước.
7. Production chỉ dùng đúng digest đã qua staging, protected environment + human approval; không down migration và không `down -v`.

Chi tiết lệnh/biến nằm trong `deploy/README.md`, `deploy/render/PROVISIONING.md`, `docs/ops/DEPLOYMENT.md`, `docs/ops/CI_CD.md`, `docs/ops/BACKUP_RESTORE.md` và `docs/ops/ROLLBACK.md`.

### 4.3. Kết quả khôi phục sao lưu

**CHƯA CHẠY.** Chưa có staging DB/object store/account nên chưa có dump checksum, restore DB ID, RPO hoặc RTO thực tế. Script được thiết kế từ chối DB không rỗng, yêu cầu confirmation đúng tên DB, kiểm SHA-256, không dùng `--clean` và kiểm Flyway/users/data_exports. Đây là blocker GO; không được coi `pg_dump` thành công là bằng chứng restore.

### 4.4. Secret/config con người cần cung cấp

Chỉ liệt kê tên, không ghi giá trị:

- Runtime DB: `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- Migration DB: `MIGRATION_DB_URL`, `MIGRATION_DB_USER`, `MIGRATION_DB_PASSWORD`.
- JWT/notification: `JWT_SECRET`, `JWT_KID`, previous key khi xoay, `NOTIFICATION_UNSUBSCRIBE_SECRET`.
- URL/network: `APP_BASE_URL`, `API_BASE_URL`, `TRUSTED_PROXIES` đã xác minh từ platform.
- SMTP/FCM: `SMTP_*`, `MAIL_FROM`, `FCM_*`.
- Backup/ops: S3-compatible credential, KMS key, Grafana/metrics auth và Render/GHCR IDs/token.
- AI tương lai: `OPENAI_API_KEY`, project/org nếu dùng, model allowlist, feature flags, quota/budget và xác nhận ZDR.
- Mobile: Android keystore/Play service account; Apple certificate/profile/team/App Store Connect key.

Secret phải nằm trong platform/GitHub protected environment, khác nhau theo môi trường và không đưa vào chat/repo/image/log/mobile.

### 4.5. Chi phí vận hành ước tính

| Thành phần | Baseline tháng | Giả định |
|---|---:|---|
| Render app + managed PostgreSQL | 31–40 USD | Một instance 1 CPU/2 GB khởi điểm, DB managed nhỏ cùng region |
| AI | Tối đa 25 USD | Hard cap production beta, cảnh báo 80%, dừng 100% |
| Tổng baseline | 56–65 USD | Trước thuế |
| Chưa tính | TBD | Domain, SMTP, object storage/KMS/egress, persistent disk, managed observability và phí/tài khoản store |

Phải lấy quote/tier/region thực tế trước GO; không dùng ước tính này làm hóa đơn cam kết.

---

## 5. AI Assistant

### 5.1. Bản đồ FR-AI → test dự kiến và trạng thái

| Yêu cầu | Test AI dự kiến | Trạng thái hiện tại |
|---|---|---|
| FR-AI-01 Consent theo miền | AI-01..04, AI-09 | **XANH** — mặc định off, terms/whitelist, thu hồi tức thời, Finance độc lập, chặn trước provider |
| FR-AI-02 Chat read-only có ngữ cảnh | AI-05..08, AI-10..15, AI-19..20 | **MỘT PHẦN** — runtime/fake, IDOR/mass-assignment/injection/concurrency xanh; chưa live resilience |
| FR-AI-03 Context disclosure | AI-07..10, AI-31, AI-35 | **XANH local/fake** — exact-minimal context/source ID/redaction; chưa live privacy validation |
| FR-AI-04 Hội thoại/xóa | AI-05, AI-28..29, AI-32 | **XANH local** — cursor/ownership/cascade/purge/retention idempotent |
| FR-AI-05 Kế hoạch/breakdown | AI-07, AI-10..15, AI-35, AI-37..38 | **XANH local/fake** — daily/weekly/breakdown strict schema, read-only |
| FR-AI-06 Insight | AI-03, AI-07..15, AI-37..38 | **XANH local/fake** — Habit/Finance consent/allowlist/sanitizer |
| FR-AI-07 Note summary | AI-03..04, AI-07..15 | **XANH local/fake** — hostile Note là untrusted context; output nguy hiểm bị sanitize |
| FR-AI-08 Usage/quota | AI-16..18, AI-33 | **XANH local** — quota trước provider, kill/budget hard stop, integer micro-USD |
| FR-AI-09 Action proposal | AI-11..15, AI-21..27 | Contract có; feature/runtime HOÃN |
| FR-AI-10 Approve/cancel | AI-21..27 | Contract có; runtime HOÃN |
| FR-AI-11 Export/delete | AI-28..32, AI-40 | **MỘT PHẦN** — V12/catalog/export-purge/retention xanh; restore staging chưa chạy |
| FR-AI-12 Suy giảm êm | AI-18..20, AI-33, AI-36 | **MỘT PHẦN** — kill/budget/concurrency/eval local xanh; chưa live failover/metrics |

Lượt verify cuối có **28/28 test AI** xanh trong 10 suite. Chi tiết từng test và ba tổng kết Maven nguyên văn nằm tại `DEPLOY_AI_PROGRESS.md` §14.

### 5.2. Contract đã công bố cho Flutter

18 path:

```text
/api/v1/ai/preferences
/api/v1/ai/conversations
/api/v1/ai/conversations/{id}
/api/v1/ai/conversations/{id}/messages
/api/v1/ai/messages/{id}/context
/api/v1/ai/data
/api/v1/ai/plans/daily
/api/v1/ai/plans/weekly
/api/v1/ai/tasks/{id}/breakdown
/api/v1/ai/insights/habits
/api/v1/ai/insights/finance
/api/v1/ai/notes/{id}/summary
/api/v1/ai/action-proposals
/api/v1/ai/action-proposals/{id}
/api/v1/ai/action-proposals/{id}/approve
/api/v1/ai/action-proposals/{id}/cancel
/api/v1/ai/usage
/api/v1/ai/limits
```

8 fixture:

```text
ai-preferences.response.json
ai-chat.request.json
ai-chat.response.json
ai-context.response.json
ai-plan.response.json
ai-action-proposal.response.json
ai-usage.response.json
ai-error.response.json
```

Các fixture dùng dữ liệu giả và đã qua kiểm tĩnh cấm `userId`, email, password, API/access/refresh/device/FCM token. Runtime hiện sinh đúng 14 path/17 operation ID thuộc AIP2–AIP4; bốn path action proposal thuộc AIP6 vẫn là overlay. Marker chỉ được xóa sau quyết định `[OPEN] AIP7-OPENAPI-01`.

### 5.3. Ma trận consent × miền dữ liệu

| Miền | Mặc định | Use case cho phép | Ràng buộc |
|---|---|---|---|
| TASK_CALENDAR | Tắt | Chat, plan ngày/tuần, breakdown/gợi ý | Chỉ record/field tối thiểu, read-only ở release đầu |
| HABIT | Tắt | Chat/insight xu hướng | Không lấy miền khác; không tự thay đổi check-in |
| GOAL | Tắt | Chat/plan/insight | Chỉ dữ liệu thuộc user và cửa sổ đã giới hạn |
| NOTES | Tắt | Chat/tóm tắt Note được chọn | Production yêu cầu ZDR; text là dữ liệu không tin cậy |
| FINANCE | Tắt, độc lập | Insight đọc theo currency | Production yêu cầu ZDR; không gửi account number/ghi chú không cần; tuyệt đối không ghi Finance |

Thu hồi consent có hiệu lực trước request kế tiếp; request đã bắt đầu có thể hoàn tất theo consent snapshot lúc bắt đầu. Context disclosure phải cho xem đúng payload sau redaction, không lộ system/developer prompt hoặc secret.

### 5.4. Hành động được phép/cấm

Được phép ở gói tương lai AIP6, sau approval trực tiếp và feature flag riêng:

- `CREATE_TASK`, tối đa 10 Task độc lập cho một breakdown.
- `UPDATE_TASK_PRIORITY`.
- `UPDATE_TASK_SCHEDULE`.

Bị cấm: mọi ghi Finance; delete/soft-delete; complete/reopen; account/auth/settings/device/consent; notification/email; thay đổi Goal/Habit/Calendar/Note; bulk ngoài allowlist; hành động nền không approval. Release AI đầu chỉ chat/planning read-only.

### 5.5. Bằng chứng chống prompt injection

Đã có bằng chứng runtime/fake-provider:

- D271 tách instruction/intent/user data và coi entity text là dữ liệu không tin cậy.
- D272 strict JSON Schema + backend validation; D273 ID allowlist; D274 sanitizer output.
- AI-10..15 quy định prompt injection, ID ngoài allowlist, malformed/extra/depth/oversize, HTML/script/system-prompt leak và refusal/incomplete.

AI-10 chứng minh prompt injection chỉ nằm trong untrusted context, không vào instruction/không bật tools; AI-11..13 từ chối ID ngoài allowlist và extra/malformed field; AI-14..15 sanitize HTML/script/system-prompt/key marker và fail closed với response thiếu. Đây là bằng chứng local/fake, không thay thế red-team/live provider validation.

### 5.6. Chi phí/độ trễ với provider giả

Fake provider trả 11 input + 7 output token và latency `3 ms`; với giá fixture 2/10 micro-USD mỗi token, cost là **92 micro-USD**. Vector 31/7 = 132 micro-USD và 1.000.000/1.000.000 = 12.000.000 micro-USD cũng xanh, hoàn toàn integer. Đây là fixture giả, **không phải benchmark provider/network thật**; p95/live load vẫn chưa đo.

### 5.7. Xuất/xóa dữ liệu AI

`DELETE /api/v1/ai/data`, năm bảng V12, thứ tự con-trước-cha trong `UserDataCatalog`, export/purge, xóa hội thoại, AI-29, retention AI-32 và INT-15 đã chạy xanh. Restore staging/AI-40 còn **CHƯA CHẠY**.

---

## 6. Thay đổi hợp đồng và test cũ đã sửa

### 6.1. Thay đổi hợp đồng

- Thêm 18 path và 42 schema AI vào `docs/api/openapi.yaml` trong hai block marker AIP1.
- Thêm 8 fixture AI và 15 error code AI đã duyệt.
- Không đổi route/payload REST/sync cũ; AIP2 đã thêm V12 và năm runtime endpoint foundation đúng contract.
- Thêm cấu hình deploy/prod/CI/ops; không thay hợp đồng mobile.

### 6.2. Test cũ đã sửa

| Test | Thay đổi | Lý do | Đã chạy? |
|---|---|---|---|
| `OpenApiExportIT` | Khóa chính xác 14 runtime AI path/17 operation ID và so byte-equivalent toàn bộ contract ngoài AI; giữ ngoại lệ hẹp cho bốn path AIP6 | Không che drift ngoài AI trong khi action proposal còn hoãn | XANH trong cả ba full verify 2026-11-14; marker chờ `[OPEN] AIP7-OPENAPI-01` |
| `ApiDocsProdProfileIT` | Cấp prod-profile test properties/Flyway riêng | `application-prod.yml` nay fail-fast và runtime production tắt Flyway | XANH 1/1 trong baseline 2026-11-11 |
| `IdorMatrixIT` | Thêm sáu route AI có path variable, data setup và request body hợp lệ | Đảm bảo victim ID/random ID cùng 404, không đổi row/log | XANH trong cả ba full verify 2026-11-14 |

Test mới: `AiContractPublicationTest`, `ProductionDeploymentContractTest`. Không xóa/disable/nới assertion cũ; tuy nhiên cần Maven verify để chứng minh hành vi thực tế.

---

## 7. Quyết định phát sinh `[CONFIRMED]` (2026-11-11)

Tại thời điểm 2026-11-11 chưa có `[OPEN]` mới và người dùng đã duyệt nguyên trạng toàn bộ P-DWP2-01..P-DWP7-03. `[OPEN] AIP7-OPENAPI-01` phát sinh ngày 2026-11-14 được ghi ở Phụ lục B:

### DWP2

1. `[CONFIRMED]` (2026-11-11) P-DWP2-01 — phát hành hai image cùng release: runtime và migrations; runtime không có DDL credential.
2. `[CONFIRMED]` (2026-11-11) P-DWP2-02 — Compose bind backend loopback; Render dùng platform port/TLS với cùng runtime image.
3. `[CONFIRMED]` (2026-11-11) P-DWP2-03 — pin tag/version ở source, khóa digest sau lần scan/SBOM đầu có bằng chứng.

### DWP3

1. `[CONFIRMED]` (2026-11-11) P-DWP3-01 — smoke bằng hostname Render tạm; staging custom domain trước production; chỉ bật HSTS khi domain/cert ổn định.
2. `[CONFIRMED]` (2026-11-11) P-DWP3-02 — không suy đoán Render proxy CIDR; `TRUSTED_PROXIES` chỉ nhận giá trị đã xác minh.
3. `[CONFIRMED]` (2026-11-11) P-DWP3-03 — không public DB để migrate; dùng private network/pre-deploy tương đương với role DDL riêng.

### DWP4

1. `[CONFIRMED]` (2026-11-11) P-DWP4-01 — logical backup S3-compatible + SSE-KMS ngoài lớp managed PITR.
2. `[CONFIRMED]` (2026-11-11) P-DWP4-02 — orphan export đủ điều kiện sau 26 giờ, tên UUID, không DB reference; dry-run trước apply.
3. `[CONFIRMED]` (2026-11-11) P-DWP4-03 — restore chỉ vào DB mới rỗng, confirmation đúng tên; cấm `--clean`.

### DWP5

1. `[CONFIRMED]` (2026-11-11) P-DWP5-01 — private/background migration service idle; CI tạo one-off `migrate` bằng digest.
2. `[CONFIRMED]` (2026-11-11) P-DWP5-02 — staging là nơi duy nhất build release; production chỉ nhận đúng digest đã qua staging.
3. `[CONFIRMED]` (2026-11-11) P-DWP5-03 — khóa action commit SHA/image digest sau lần chạy/scan đầu; update qua review.

### DWP6

1. `[CONFIRMED]` (2026-11-11) P-DWP6-01 — scrape chỉ private/authenticated; không permit public `/actuator/prometheus` để chữa tạm.
2. `[CONFIRMED]` (2026-11-11) P-DWP6-02 — backup/restore metric qua node-exporter textfile collector tối giản và atomic rename.
3. `[CONFIRMED]` (2026-11-11) P-DWP6-03 — future rule chỉ bật cùng producer + test/no-data behavior.

### DWP7

1. `[CONFIRMED]` (2026-11-11) P-DWP7-01 — production package/bundle ID thống nhất `vn.lifeos.app`; agent mobile xử lý native migration/store identity.
2. `[CONFIRMED]` (2026-11-11) P-DWP7-02 — pipeline chỉ upload Play Internal draft/TestFlight; promotion production là bước con người.
3. `[CONFIRMED]` (2026-11-11) P-DWP7-03 — application ID/signing/privacy manifest là hard gate; không dùng debug key hoặc ID mẫu.

---

## 8. Rủi ro và điểm cần người xem kỹ

1. **Restore chưa chứng minh:** rủi ro GO lớn nhất; chưa có RPO/RTO/checksum/DB restore thực tế.
2. **AIP1 overlay/AIP6 hoãn:** runtime sinh 14/18 path; cần quyết định `[OPEN] AIP7-OPENAPI-01` trước khi xóa marker hoặc đổi contract Flutter.
3. **Private metrics producer thiếu:** backend chưa có Prometheus registry/secure scrape và job gauge; future rules cố ý tắt.
4. **Trusted proxy:** chưa có CIDR/regex chính thức của platform; cấu hình sai ảnh hưởng IP rate limit/audit.
5. **Workflow chưa chạy/pin digest:** action source còn cần lần chạy đầu, scan và khóa SHA/digest.
6. **Mobile chưa release-ready:** Android còn ID mẫu/debug signing; iOS còn ID mẫu, thiếu team/privacy manifest; production diagnostics cần review.
7. **Privacy/ZDR:** chưa có legal identity, processor list/DPA, privacy/support URL hay xác nhận ZDR; Notes/Finance phải tắt.
8. **Observability no-data:** alert source có no-data rule nhưng chưa fire/recover drill; disk metric phụ thuộc datasource Render thực tế.
9. **Một instance:** rate limiter/revocation/snapshot/export/job/counter chưa sẵn sàng scale ngang.
10. **Công cụ host:** static parse không thay thế Maven/Docker/promtool/shellcheck/live evidence.

## 9. Việc hoãn và đang chờ con người

| Mục | Trạng thái/lý do |
|---|---|
| AIP2 → AIP3 → AIP4 → AIP7 | Local/fake đã thực thi; AIP2/AIP4 xong, AIP3/AIP7 một phần; live còn NO-GO |
| RAG/pgvector | Hoãn theo O-AI-06 |
| Action proposal runtime | Hoãn theo O-AI-05; release đầu read-only |
| Render/domain/DNS/billing | Cần tài khoản và quyết định con người |
| SMTP/FCM/APNs/OpenAI/ZDR | Cần account/key/terms; không nhận secret qua chat |
| GitHub environments/secrets/workflow install | Chủ repo thực hiện hoặc cấp quyền |
| Backup/restore/rollback/alert drill | Cần staging resources thật |
| Android/iOS store release | Cần agent mobile sửa release project và developer accounts |
| Privacy/legal/store forms | Cần legal identity/contact và review con người |

## 10. Kết luận và điều kiện tiếp tục

Artifact/source hiện tại **chưa đủ để GO production** và chưa thể gọi Phase 8 AI hoàn thành. Nhánh local/fake-provider đã hoàn tất AIP2/AIP4 và phần khả thi của AIP3/AIP7 với ba full verify liên tiếp. Các việc live sau vẫn NO-GO chờ con người:

1. Render/GitHub/object-store/domain credentials qua secret manager/protected environment, không qua báo cáo/chat.
2. Staging thật để chạy migration, backup/restore, observability, rollback và smoke theo `PRODUCTION_READINESS.md`.
3. OpenAI project/key/spend limit/ZDR, privacy/legal/store và mobile signing/account.

Trạng thái chính thức: **AIP2 XONG (bằng chứng); AIP3 MỘT PHẦN; AIP4 XONG (bằng chứng); AIP7 MỘT PHẦN; `[OPEN] AIP7-OPENAPI-01` — NO-GO production**.

---

## Phụ lục A — cập nhật AIP2/AIP3 ngày 2026-11-12

### A.1. Bảng trạng thái AIP2/3/4/7

| Gói | Nhãn | Bằng chứng/phần thiếu |
|---|---|---|
| AIP2 | **MỘT PHẦN** | Foundation xanh: baseline 852/852; unit AI 4/4; integration AIP2 17/17; V12 clean/upgrade 9/9; catalog 1/1. AI-04/06..09 còn phụ thuộc context/chat AIP3 |
| AIP3 | **CHƯA LÀM tại thời điểm phụ lục này** | Khi đó tạm dừng ở `AIP3-SPRING-AI-01`; mục này đã được `[RESOLVED]` ngày 2026-11-13 và trạng thái mới nằm ở Phụ lục B |
| AIP4 | **CHƯA LÀM** | Phụ thuộc AIP3; chưa có plan/breakdown/insight/note summary runtime |
| AIP7 | **CHƯA LÀM** | Chưa có injection AI-10..15, load/eval, privacy hardening và ba lượt full verify liên tiếp |

### A.2. Quyết định lịch sử — đã `[RESOLVED]` 2026-11-13

Spring AI 2.0.1 tương thích Spring Boot 4.1 nhưng không hỗ trợ OpenAI Responses API. Spring AI 2.1 thêm Responses API với `store=false` nhưng chuyển baseline sang Spring Boot 4.2 và tại thời điểm kiểm tra chưa phải lựa chọn GA tương thích Boot 4.1 đã duyệt. Ba hướng đã được trình người dùng:

1. Giữ Boot 4.1 và cho phép AIP3/AIP4 fake-only, hoãn adapter thật.
2. Cho phép nâng backend lên Boot 4.2 và chọn Spring AI 2.1 GA phù hợp sau regression.
3. Cho phép ngoại lệ bỏ Spring AI ở adapter, dùng OpenAI Java SDK/HTTP Responses API sau `LLMProvider`.

Người dùng đã duyệt hướng 3 ngày 2026-11-13: giữ Boot 4.1 và dùng SDK chính thức sau `LLMProvider`. Chi tiết/bằng chứng nguồn chính thức nằm tại `DEPLOY_AI_PROGRESS.md` §13.

### A.3. Bằng chứng quota/budget/fake provider

- AI-16: rate/quota chặn trước provider, trả error code ổn định.
- AI-17: cost dùng integer micro-USD, không `double`; vector 1.000.000/1.000.000 token = 12.000.000 micro-USD.
- AI-18: kill switch và budget hard stop trả mã ổn định; core API không bị sửa.
- Fake provider trả token/latency xác định và không network. Giá trị latency `3 ms` là fixture giả, không phải benchmark provider thật.
- AI-10..15 chưa chạy; không tuyên bố chống injection runtime.

### A.4. OpenAPI và production

Hai marker overlay AIP1 và ngoại lệ `OpenApiExportIT` **chưa xóa** vì runtime mới có 4/18 path (5 operation); xóa sớm sẽ làm mất hợp đồng Flutter. Chỉ xóa khi controller AIP3/AIP4 và phần AIP6 hoãn có thể sinh đúng 18 path, 42 schema, 15 mã lỗi hoặc khi người dùng duyệt cách xử lý contract của AIP6.

Render, secret, ZDR, live provider smoke, backup/restore staging, observability drill, signing và store vẫn **NO-GO chờ con người**; không có secret nào được ghi vào repo/log/báo cáo/chat.

---

## Phụ lục B — kết quả AIP2/AIP3/AIP4/AIP7 ngày 2026-11-14

Phụ lục này là trạng thái mới nhất và thay thế các trạng thái lịch sử trong Phụ lục A.

### B.1. Bảng trạng thái cuối phiên

| Gói | Nhãn | Bằng chứng |
|---|---|---|
| AIP2 | **XONG (bằng chứng)** | V12/catalog/export-purge/preferences/consent/quota/budget/fake; AI-01..05/09/16..18/29 xanh. |
| AIP3 | **MỘT PHẦN** | SDK chính thức sau `LLMProvider`; Responses `store=false`, no tools; chat/history/context/delete, exact-minimal context xanh; live smoke/ZDR/resilience chưa chạy. |
| AIP4 | **XONG (bằng chứng)** | Daily/weekly plan, breakdown, Habit/Finance insight, Note summary; strict schema/allowlist/sanitizer; AI-09..15 xanh. |
| AIP7 | **MỘT PHẦN** | AI-20/32/36..38, A15, 40 prompt tiếng Việt, secret scan và 3 full verify x 879 test xanh; live load/metrics/restore/ZDR và OpenAPI 18/18 còn thiếu. |

AI test riêng: **28/28**, 0 failure/error/skipped. Tất cả dùng fake/client giả; không gọi OpenAI thật, không cần key và không bật tools. Fake fixture là 11 input, 7 output, 3 ms, 92 micro-USD; đây không phải số đo provider thật.

### B.2. Ba lượt full verify xanh liên tiếp

```text
Run 1
[INFO] Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 712, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  08:32 min
[INFO] Finished at: 2026-11-14T22:31:00+07:00

Run 2
[INFO] Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 712, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  07:02 min
[INFO] Finished at: 2026-11-14T22:38:19+07:00

Run 3
[INFO] Tests run: 167, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 712, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  07:05 min
[INFO] Finished at: 2026-11-14T22:45:42+07:00
```

Tổng mỗi lượt: **879 test**, lớn hơn 847. Một lượt trước chuỗi đỏ do `MailpitDeliveryIT` timeout SMTP/container; test chạy riêng sau đó xanh 1/1, chuỗi được reset và ba lượt trên là ba lượt xanh liên tiếp.

### B.3. OpenAPI và `[OPEN]`

Runtime sinh chính xác **14 AI path/17 operation ID** của AIP2–AIP4; `OpenApiExportIT` khóa tập này và vẫn kiểm byte-equivalent toàn bộ contract ngoài AI. Contract Flutter đã công bố vẫn là **18 path/42 schema/15 mã lỗi**. Bốn path action proposal thuộc AIP6 chưa có runtime vì AIP6 hoãn, nên marker overlay và ngoại lệ hẹp chưa thể xóa mà không làm mất hoặc thay đổi contract đã duyệt.

`[OPEN] AIP7-OPENAPI-01`: người dùng cần chọn giữ overlay cho bốn path đến khi mở AIP6, hoặc duyệt tạm loại bốn path khỏi contract Flutter. Không có quyết định này, agent dừng đúng §11 và không tự đổi contract.

### B.4. NO-GO live

Giữ nguyên: Render service/DB/TLS, secret thật, OpenAI project/key và live provider smoke, xác nhận ZDR, backup/restore staging, private observability/alert drill, signed AAB/IPA và store submission đều chờ con người. Secret literal scan cho kết quả **0 file**; tên biến/placeholder và vector injection không phải secret.
