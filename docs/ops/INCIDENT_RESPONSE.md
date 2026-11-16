# LifeOS — Incident response

## Nguyên tắc chung

1. Xác nhận incident, thời gian, môi trường và phạm vi; không chép PII/secret vào ticket/chat.
2. Ưu tiên cô lập và ngăn ghi sai/mất dữ liệu; không chạy lệnh phá hủy để “sửa nhanh”.
3. Bảo toàn log/metric đã redaction, deploy ID, image digest, migration version và checksum backup.
4. Chỉ rollback runtime khi schema backward-compatible. Restore luôn vào DB mới, xác minh rồi mới chuyển endpoint.
5. Sau phục hồi: xác nhận health/auth/sync/jobs/export, đánh giá privacy notification, ghi timeline và hành động phòng ngừa.

## Provider AI timeout/429/5xx

- Tắt `lifeos.ai.enabled` hoặc cờ mode liên quan; chat/planning phải fail closed, không fallback sang provider chưa duyệt.
- Kiểm circuit, latency, quota/budget và provider status bằng dữ liệu không chứa prompt.
- Không retry vô hạn; giữ idempotency và không trừ quota hai lần.
- Chỉ bật lại từng phần sau smoke bằng dữ liệu giả và ngân sách staging.

## AI hết budget hoặc lộ key

- Budget 100%: hard stop; không nâng cap trong incident nếu chưa có phê duyệt.
- Key nghi lộ: tắt AI, revoke key tại provider, tạo key mới cho đúng environment/project, cập nhật secret manager, deploy, xác minh key cũ không dùng được.
- Tìm nguồn lộ trong log/artifact/history mà không phát tán lại key; kiểm usage bất thường và liên hệ provider nếu cần.
- Notes/Finance production giữ tắt nếu ZDR chưa được xác nhận.

## Database/pool/disk

- Giảm traffic hoặc đưa service maintenance nếu pending/latency/đầy disk gây lỗi ghi.
- Kiểm connection leak, long transaction, disk growth và deploy gần nhất. Không tăng pool vượt khả năng DB.
- Nếu nghi corruption/mất dữ liệu: dừng deploy/ghi, lập snapshot, chuyển sang runbook backup/restore; không restore đè DB nguồn.

## Outbox/reminder/job

- Xác định producer hay worker dừng, lock/DB/provider lỗi; giữ record để điều tra.
- Retry qua cơ chế idempotent của ứng dụng. Không cập nhật trạng thái hàng loạt bằng SQL ad-hoc.
- Đối chiếu last-success/lag sau khi metric producer được triển khai; trước đó dùng log đã redaction và DB read-only query được review.

## Backup/restore

- Backup stale: kiểm runner, S3/KMS/IAM/TLS/dung lượng, chạy backup có giám sát và xác minh checksum/object.
- Restore thất bại: giữ nguyên DB đích để điều tra nếu an toàn, tạo DB mới cho lần sau; không dùng `--clean`.
- Không đóng incident chỉ vì `pg_dump` thành công; phải có restore verification theo `BACKUP_RESTORE.md`.

## Privacy/data leak

- Cô lập endpoint/provider/key và dừng luồng dữ liệu liên quan.
- Bảo toàn bằng chứng tối thiểu, hạn chế quyền truy cập; không sao chép dữ liệu nhạy cảm sang hệ thống incident không được duyệt.
- Đánh giá loại dữ liệu, user/environment/phạm vi/thời gian, nghĩa vụ thông báo và xóa; tham vấn pháp lý/chính sách địa phương.
- Rotate credential, vá nguyên nhân, xác minh export/delete/consent; chỉ phục hồi sau phê duyệt incident owner.
