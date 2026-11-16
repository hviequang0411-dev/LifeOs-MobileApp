# LifeOS — Production readiness review

> Kết luận ngày 2026-11-10: **NO-GO**. Artifact chuẩn bị đã có, nhưng chưa có bằng chứng staging/live bắt buộc. Đây là gate an toàn, không phải đánh giá thất bại của source.

## Gate và bằng chứng

| Gate | Trạng thái | Bằng chứng cần để GO |
|---|---|---|
| Quyết định hosting/SLO/secret | READY trên tài liệu | Ghi region/domain/vendor/tier thực tế và owner secret |
| Image/config fail-fast | SOURCE READY | Java 21 `clean verify`, build/scan/SBOM, container smoke, missing-secret failure |
| Render staging/TLS/private DB | NO-GO | Service/DB ID, region, TLS, DB private, DML role từ chối DDL, XFF spoof test |
| Migration | NO-GO | One-off job ID, checksum/validate, lock/timeout evidence |
| Backup/restore | NO-GO | Object/checksum/KMS, restore vào DB mới, RPO ≤24h/RTO ≤4h, app verification |
| CI/CD/rollback | NO-GO | GitHub run URL, immutable digest từ staging, protected production approval, rollback rehearsal |
| Observability | NO-GO | Private/authenticated scrape, target UP, core alerts fire/recover, no-data/disk source |
| Backend review/AIP2+ | BLOCKED | Người dùng báo kết quả review; AIP2 trở đi hoàn tất theo Phase 8 |
| Mobile release | NO-GO | Release IDs/signing/privacy manifest, signed AAB/IPA, Play Internal/TestFlight evidence |
| Privacy/legal/store | NO-GO | Legal identity/contact/URLs, processor/ZDR status, reviewed policy/data safety/content rating |

## Go-live rehearsal bắt buộc

1. Dùng dữ liệu giả, backup staging và ghi checksum/restore point.
2. Build/scan hai image, deploy migration one-off rồi runtime bằng immutable digest.
3. Smoke readiness, auth/IDOR, offline sync push/pull, outbox/reminder/jobs, export/delete; AI chỉ khi gói tương ứng hoàn tất và feature flag bật có chủ đích.
4. Kích hoạt có kiểm soát 5xx/pool/backup-stale signal và xác minh alert không chứa PII.
5. Rollback runtime về digest trước; xác minh schema tương thích và core smoke.
6. Restore backup vào DB mới, `flyway validate`, kiểm isolation/Finance/sync/outbox/export/AI catalog, đo RPO/RTO.
7. Ghi run URL, Render deploy/job IDs, image digests, backup checksum, thời gian và người phê duyệt vào final report.

Không diễn tập destructive trên production. Không chạy `down -v`, down migration hoặc restore đè DB.

## Chi phí beta dự kiến

Theo baseline đã duyệt trong master plan ngày 2026-11-10: Render app + managed PostgreSQL khoảng 31–40 USD/tháng; AI hard cap 25 USD/tháng, tổng nền tảng+AI khoảng 56–65 USD/tháng trước thuế. Chưa gồm domain, SMTP, object storage/KMS/egress, observability managed nếu chọn, persistent disk và phí/tài khoản store. Phải lấy quote/region/tier thực tế trước GO và cấu hình budget alert.

## Điều kiện chuyển NO-GO thành GO

Mọi gate NO-GO ở bảng phải có bằng chứng; không có Critical/High CVE chưa chấp nhận; restore/rollback đạt; privacy/store claims khớp binary; backend review đã giải quyết; tất cả `[PROPOSED]` ảnh hưởng production được người dùng duyệt. GO cần sign-off con người, không được suy ra chỉ từ script/config parse thành công.
