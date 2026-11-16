# LifeOS — Rollback

## Nguyên tắc

- Rollback mặc định là **runtime image về digest trước**; không build lại và không dùng mutable tag.
- Không chạy down migration tự động. Chỉ rollback runtime nếu schema mới còn backward-compatible.
- Không dùng `docker compose down -v`, không xóa persistent disk/DB và không restore đè production.
- AI/RAG/action có kill switch riêng; tắt feature trước khi rollback toàn ứng dụng nếu sự cố chỉ nằm ở tính năng đó.

## Trước deploy

Ghi release/run ID, runtime+migration digest hiện tại và mới, Flyway version/checksum, backup/restore point, config/feature flags, người phê duyệt. Xác minh image mới đã qua staging và production workflow dùng chính digest đó.

## Migration thất bại trước runtime deploy

1. Dừng pipeline; không deploy runtime mới.
2. Giữ runtime cũ và DB để điều tra; bảo toàn migration job ID/log đã redaction.
3. Nếu migration transaction rollback sạch, sửa bằng migration forward mới sau review; không sửa checksum file đã áp dụng.
4. Nếu có dấu hiệu mất/corrupt dữ liệu, dừng theo §11.6, snapshot và restore vào DB mới để đánh giá.

## Runtime mới lỗi, schema tương thích

1. Tắt rollout/AI flag liên quan.
2. Deploy runtime digest trước bằng workflow rollback/manual Render API đã duyệt; không chạy migration.
3. Chờ live/readiness, rồi smoke login/auth, CRUD read-only, sync, outbox/jobs/export.
4. So 5xx/latency/DB pool với baseline; ghi deploy ID/digest/timeline.

## Schema không tương thích hoặc dữ liệu sai

Không deploy app cũ mù. Dừng ghi nếu cần, tạo DB mới, restore backup/PITR, chạy `flyway validate` và toàn bộ verification trong `BACKUP_RESTORE.md`; chỉ chuyển `DB_URL` sau phê duyệt và smoke cô lập. Giữ DB cũ read-only để điều tra theo retention/incident policy.

## Xác nhận kết thúc

Incident chỉ đóng khi core API ổn định, queue/job không tăng lỗi, backup vẫn hoạt động, không có privacy impact chưa xử lý và có owner cho root-cause/follow-up. Rollback production thật đang `CHƯA CHẠY` cho đến khi có Render deploy ID và staging rehearsal.
