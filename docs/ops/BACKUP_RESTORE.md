# PostgreSQL backup, restore drill và export lifecycle

## Mục tiêu đã duyệt

- Managed PITR nếu plan hỗ trợ.
- Nightly logical backup mã hóa sang account/region khác: 7 daily, 4 weekly, 6 monthly.
- RPO ≤ 24 giờ ngay cả khi PITR không dùng được; RTO ≤ 4 giờ.
- Restore thật vào DB mới/cô lập mỗi tháng và trước release lớn.

## Backup job

`ops/backup/postgres-backup.sh` dùng `pg_dump` custom format, checksum SHA-256 và upload S3 với SSE-KMS. DB credential đi qua biến `PG*`, không xuất hiện trong command line/log. Job cần:

- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`, `PGSSLMODE=require`;
- `BACKUP_WORK_DIR` là thư mục tạm riêng, không phải `/`/`/var`;
- `BACKUP_S3_URI=s3://bucket/prefix` và `BACKUP_KMS_KEY_ID`;
- IAM chỉ được `PutObject/GetObject` trong prefix backup, không quyền DB/runtime khác.

Bucket phải bật versioning, block public access, KMS, audit và lifecycle: daily hết hạn sau 7 ngày, weekly sau 28 ngày, monthly sau 183 ngày. Không đưa bucket/key/account ID thật vào repo.

## Restore drill

1. Tạo DB mới, rỗng, cô lập; không trỏ app production vào DB này.
2. Cấp restore role tạm; đặt `RESTORE_CONFIRM_NEW_DATABASE` đúng bằng tên DB đích.
3. Chạy `postgres-restore-drill.sh`; script từ chối DB có table trong schema `public`, kiểm checksum và không dùng `--clean`.
4. Chạy tiếp `flyway validate`, khởi động app bằng runtime role, smoke read-only, kiểm isolation/Finance/sync/outbox/export và bảng AI khi đã có.
5. Ghi thời gian, tuổi backup, checksum object, row counts tổng hợp và lỗi vào final report; không ghi dữ liệu hàng.
6. Thu hồi restore credential và xóa DB drill sau khi xác nhận đúng target.

Lệnh mẫu chỉ dùng tên biến, không dùng giá trị:

```bash
bash ops/backup/postgres-backup.sh
bash ops/backup/postgres-restore-drill.sh
```

## Export orphan cleanup

`ops/export/cleanup-orphans.sh` chỉ xem file UUID `.zip` cũ hơn 26 giờ trong đúng canonical `EXPORT_DIR`. Mặc định dry-run; chỉ xóa khi `APPLY=true`, file không phải symlink và UUID không còn được `data_exports.file_ref` tham chiếu.

Chạy dry-run trước, kiểm metric/log tên file chỉ là UUID, sau đó mới lên lịch apply. Script không thay record DB và không thay job expire trong app.

## Điều kiện dừng

Checksum sai, restore vào DB không rỗng, row/constraint/isolation sai, backup quá RPO hoặc dấu hiệu mất dữ liệu → dừng deploy theo §11. Không `flyway repair`, `pg_restore --clean` hay ghi đè production để chữa nhanh.
