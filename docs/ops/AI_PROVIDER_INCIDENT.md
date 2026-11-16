# LifeOS — AI provider incident

## Trigger

Provider timeout/429/5xx >10%, p95 >25 giây, circuit mở >5 phút, chi phí 80/100%, schema/refusal bất thường hoặc nghi lộ API key. Không đưa prompt/context/output/user ID vào alert hoặc incident record.

## Cô lập

1. Tắt `lifeos.ai.enabled` hoặc mode/domain bị ảnh hưởng; core CRUD/sync phải tiếp tục.
2. Nếu nghi lộ key: revoke ngay tại provider, kiểm usage/project/spend cap, tạo key mới trong secret manager đúng environment; không dán key vào chat/ticket.
3. 100% budget là hard stop. Không nâng cap trong incident nếu chưa có phê duyệt.
4. Notes/Finance giữ tắt nếu ZDR chưa được xác nhận; không fallback sang provider/model chưa duyệt.

## Điều tra và phục hồi

- Ghi thời gian, environment, provider status, request result bucket, latency/circuit/quota/cost aggregate, release và config version; không ghi nội dung.
- Kiểm timeout/retry/idempotency; không retry vô hạn hoặc trừ quota hai lần.
- Rotate key theo `SECRET_ROTATION.md`, smoke staging bằng dữ liệu giả, xác minh redaction/metric/budget, rồi bật lại rollout nhỏ từng mode.
- Nếu có khả năng dữ liệu bị lộ, chuyển sang privacy incident trong `INCIDENT_RESPONSE.md`, đánh giá notification/pháp lý.

## Sau sự cố

Đối soát provider usage với `ai_usage`; xác minh export/delete/consent không bị phá; cập nhật test/eval/runbook. Không tuyên bố sẵn sàng production trước AIP2/AIP3 vì metric/runtime AI hiện chưa tồn tại.
