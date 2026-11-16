-- Đợt sửa backend 1 (docs/review/BACKEND_FIX_REPORT.md): F5 và F8. Prompt gốc gọi là V12; V12 đã thuộc module ai nên
-- dùng số kế tiếp còn trống. V1–V12 không đổi.

-- F5 (PHASE_7 D204): tối đa MỘT yêu cầu xuất dữ liệu đang chờ/chạy cho mỗi người dùng, bảo đảm ở mức DB (lớp ứng dụng
-- còn khóa dòng users trước khi kiểm/chèn). Dữ liệu cũ có thể đã trùng do đua trước đợt sửa: giữ yêu cầu sớm nhất,
-- các yêu cầu trùng còn lại chuyển FAILED/DUPLICATE (vẫn tính vào hạn mức 2 lần/24 giờ như mọi yêu cầu đã tạo).
UPDATE data_exports e
SET status = 'FAILED', error_code = 'DUPLICATE', finished_at = now()
WHERE e.status IN ('PENDING', 'RUNNING')
  AND EXISTS (SELECT 1 FROM data_exports o
              WHERE o.user_id = e.user_id AND o.status IN ('PENDING', 'RUNNING')
                AND (o.requested_at, o.id) < (e.requested_at, e.id));
CREATE UNIQUE INDEX ux_data_exports_user_active ON data_exports (user_id) WHERE status IN ('PENDING', 'RUNNING');

-- F8 (PHASE_7 D205): sự kiện outbox ghi rõ người dùng sở hữu để xóa tài khoản xóa được MỌI sự kiện còn mang userId
-- (trước đây chỉ xóa theo aggregate_type = 'user'; BUDGET_THRESHOLD có aggregate là ngân sách nên bị sót). Không khóa
-- ngoại: sự kiện hệ thống không thuộc người dùng nào có user_id NULL, và xóa tài khoản xóa outbox trước dòng users.
ALTER TABLE outbox_events ADD COLUMN user_id uuid;
-- Điền cho sự kiện hiện có khi suy ra được: aggregate là user, hoặc payload còn userId dạng UUID (payload của sự kiện
-- đã kết thúc là {} nên phần còn lại để NULL).
UPDATE outbox_events
SET user_id = CASE
        WHEN aggregate_type = 'user' THEN aggregate_id
        ELSE (payload ->> 'userId')::uuid
    END
WHERE aggregate_type = 'user'
   OR payload ->> 'userId' ~* '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$';
CREATE INDEX ix_outbox_events_user ON outbox_events (user_id) WHERE user_id IS NOT NULL;
