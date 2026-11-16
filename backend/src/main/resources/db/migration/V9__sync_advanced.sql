-- Phase 5a/5d phía server (docs/phases/PHASE_5_SYNC_ADVANCED.md §5; D150–D172). Đặc tả gọi là V8; đánh số V9 vì V8
-- đã dùng. Bảng devices đã có từ V1 (identity, PRODUCT_CONTEXT §19.2) nên KHÔNG tạo lại (D162).

-- D150, D153: cursor nhỏ hơn giá trị này → 410 SYNC_FULL_RESYNC_REQUIRED.
ALTER TABLE user_sync_state ADD COLUMN min_retained_seq bigint NOT NULL DEFAULT 0;
ALTER TABLE user_sync_state ADD CONSTRAINT ck_user_sync_state_min_retained
    CHECK (min_retained_seq >= 0 AND min_retained_seq <= last_seq);

-- D163: dấu vết thiết bị để chẩn đoán (không FK cứng: thiết bị có thể bị xóa; không dùng cho logic).
ALTER TABLE sync_mutations ADD COLUMN device_id uuid;

-- D154: retention sync_mutations theo created_at; D153: change_logs theo created_at.
CREATE INDEX ix_sync_mutations_created ON sync_mutations (created_at);
CREATE INDEX ix_change_logs_created ON change_logs (created_at);

-- D170: báo cáo chẩn đoán do client gửi (không chứa dữ liệu trường), giữ 30 ngày.
CREATE TABLE sync_diagnostics (
    id         uuid        NOT NULL,
    user_id    uuid        NOT NULL,
    payload    jsonb       NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT pk_sync_diagnostics PRIMARY KEY (id),
    CONSTRAINT fk_sync_diagnostics_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX ix_sync_diagnostics_created ON sync_diagnostics (created_at);
CREATE INDEX ix_sync_diagnostics_user ON sync_diagnostics (user_id);

-- D156: xóa cứng tombstone hết hạn — chỉ mục một phần theo deleted_at cho mọi bảng đồng bộ.
CREATE INDEX ix_tasks_tombstone ON tasks (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_projects_tombstone ON projects (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_categories_tombstone ON categories (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_habits_tombstone ON habits (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_habit_checkins_tombstone ON habit_checkins (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_routines_tombstone ON routines (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_routine_steps_tombstone ON routine_steps (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_routine_logs_tombstone ON routine_logs (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_routine_step_logs_tombstone ON routine_step_logs (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_financial_accounts_tombstone ON financial_accounts (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_financial_categories_tombstone ON financial_categories (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_financial_transactions_tombstone ON financial_transactions (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_account_transfers_tombstone ON account_transfers (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_budgets_tombstone ON budgets (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_recurring_transactions_tombstone ON recurring_transactions (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_saving_goals_tombstone ON saving_goals (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_saving_contributions_tombstone ON saving_contributions (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_calendar_events_tombstone ON calendar_events (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_event_exceptions_tombstone ON event_exceptions (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_time_blocks_tombstone ON time_blocks (deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX ix_reminders_tombstone ON reminders (deleted_at) WHERE deleted_at IS NOT NULL;
