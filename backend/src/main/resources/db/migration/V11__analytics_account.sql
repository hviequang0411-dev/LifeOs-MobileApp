-- Phase 7 Life Intelligence & Analytics (docs/phases/PHASE_7_INTELLIGENCE_ANALYTICS.md §5; D190–D209). Đặc tả gọi là
-- V10; đánh số V11. Thêm bảng mã xác nhận xóa tài khoản và nhật ký kiểm toán (BACKEND_MASTER_PLAN §4 mục 7).

CREATE TABLE reviews (
    id          uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    period_type varchar(5)  NOT NULL,
    period_key  date        NOT NULL,
    reflection  text        NOT NULL DEFAULT '',
    mood        smallint,
    version     bigint      NOT NULL DEFAULT 1,
    deleted_at  timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_reviews PRIMARY KEY (id),
    CONSTRAINT fk_reviews_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_reviews_period_type CHECK (period_type IN ('DAY', 'WEEK', 'MONTH')),
    CONSTRAINT ck_reviews_reflection CHECK (char_length(reflection) <= 5000),
    CONSTRAINT ck_reviews_mood CHECK (mood IS NULL OR mood BETWEEN 1 AND 5)
);
CREATE UNIQUE INDEX ux_reviews_period ON reviews (user_id, period_type, period_key) WHERE deleted_at IS NULL;
CREATE INDEX ix_reviews_tombstone ON reviews (deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE data_exports (
    id             uuid         NOT NULL,
    user_id        uuid         NOT NULL,
    status         varchar(7)   NOT NULL,
    requested_at   timestamptz  NOT NULL,
    started_at     timestamptz,
    finished_at    timestamptz,
    expires_at     timestamptz,
    file_ref       varchar(200),
    size_bytes     bigint,
    error_code     varchar(40),
    download_count integer      NOT NULL DEFAULT 0,
    CONSTRAINT pk_data_exports PRIMARY KEY (id),
    CONSTRAINT fk_data_exports_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_data_exports_status CHECK (status IN ('PENDING', 'RUNNING', 'READY', 'FAILED', 'EXPIRED'))
);
CREATE INDEX ix_data_exports_user ON data_exports (user_id, requested_at DESC);
CREATE INDEX ix_data_exports_queue ON data_exports (status, requested_at) WHERE status IN ('PENDING', 'RUNNING');

ALTER TABLE users ADD COLUMN deletion_scheduled_at timestamptz;
CREATE INDEX ix_users_deletion ON users (deletion_scheduled_at) WHERE deletion_scheduled_at IS NOT NULL;

CREATE TABLE account_deletion_codes (
    id          uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    code_hash   varchar(64) NOT NULL,
    expires_at  timestamptz NOT NULL,
    used_at     timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_account_deletion_codes PRIMARY KEY (id),
    CONSTRAINT fk_account_deletion_codes_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX ix_account_deletion_codes_user ON account_deletion_codes (user_id, created_at DESC);

-- Nhật ký kiểm toán KHÔNG chứa dữ liệu cá nhân (D205): chỉ băm SHA-256 của userId và thời điểm; giữ 365 ngày (§4).
CREATE TABLE account_deletion_audit (
    id         uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_hash  varchar(64) NOT NULL,
    deleted_at timestamptz NOT NULL,
    table_rows integer     NOT NULL,
    CONSTRAINT pk_account_deletion_audit PRIMARY KEY (id)
);
CREATE INDEX ix_account_deletion_audit_time ON account_deletion_audit (deleted_at);

-- D207: chỉ mục cho thống kê Task theo kỳ (completed_at); bằng chứng EXPLAIN ở AnalyticsQueryPlanIT.
CREATE INDEX ix_tasks_user_completed ON tasks (user_id, completed_at) WHERE deleted_at IS NULL AND completed_at IS NOT NULL;

ALTER TABLE change_logs DROP CONSTRAINT ck_change_logs_entity_type;
ALTER TABLE change_logs ADD CONSTRAINT ck_change_logs_entity_type CHECK (entity_type IN (
    'task', 'project', 'category', 'user_settings',
    'habit', 'habit_checkin', 'routine', 'routine_step', 'routine_log', 'routine_step_log',
    'financial_account', 'financial_category', 'financial_transaction', 'account_transfer', 'budget',
    'recurring_transaction', 'saving_goal', 'saving_contribution',
    'calendar_event', 'event_exception', 'time_block', 'reminder',
    'note', 'tag', 'note_tag', 'note_link', 'goal', 'milestone',
    'review'));
