-- Phase 4a/4b (PHASE_4 §5; D121, D122, D135–D143, D147). Dữ liệu dẫn xuất/thiết bị/tùy chọn: KHÔNG có cột đồng bộ,
-- không đi qua change_logs.

CREATE TABLE notifications (
    id           uuid         NOT NULL,
    user_id      uuid         NOT NULL,
    category     varchar(10)  NOT NULL,
    template_key varchar(60)  NOT NULL,
    title        varchar(200) NOT NULL,
    body         varchar(500) NOT NULL,
    data         jsonb        NOT NULL DEFAULT '{}',
    dedupe_key   varchar(200) NOT NULL,
    read_at      timestamptz,
    created_at   timestamptz  NOT NULL,
    expires_at   timestamptz  NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ux_notifications_dedupe UNIQUE (dedupe_key),
    CONSTRAINT ck_notifications_category
        CHECK (category IN ('SECURITY', 'REMINDER', 'TASK', 'HABIT', 'FINANCE', 'SYSTEM'))
);
CREATE INDEX ix_notifications_user_created ON notifications (user_id, created_at DESC, id DESC);
CREATE INDEX ix_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;
CREATE INDEX ix_notifications_expires ON notifications (expires_at);

CREATE TABLE device_push_tokens (
    id           uuid         NOT NULL,
    user_id      uuid         NOT NULL,
    device_id    uuid         NOT NULL,
    platform     varchar(7)   NOT NULL,
    token        varchar(512) NOT NULL,
    created_at   timestamptz  NOT NULL,
    last_seen_at timestamptz  NOT NULL,
    CONSTRAINT pk_device_push_tokens PRIMARY KEY (id),
    CONSTRAINT fk_device_push_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_device_push_tokens_device_id FOREIGN KEY (device_id) REFERENCES devices (id),
    CONSTRAINT ux_device_push_tokens_token UNIQUE (token),
    CONSTRAINT ux_device_push_tokens_device UNIQUE (device_id),
    CONSTRAINT ck_device_push_tokens_platform CHECK (platform IN ('ANDROID', 'IOS'))
);
CREATE INDEX ix_device_push_tokens_user ON device_push_tokens (user_id, last_seen_at);

CREATE TABLE notification_deliveries (
    id              uuid        NOT NULL,
    user_id         uuid        NOT NULL,
    notification_id uuid        NOT NULL,
    channel         varchar(6)  NOT NULL,
    device_token_id uuid,
    status          varchar(9)  NOT NULL,
    scheduled_for   timestamptz NOT NULL,
    attempt_count   smallint    NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL,
    last_error_code varchar(40),
    sent_at         timestamptz,
    created_at      timestamptz NOT NULL,
    CONSTRAINT pk_notification_deliveries PRIMARY KEY (id),
    CONSTRAINT fk_notification_deliveries_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_notification_deliveries_notification
        FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_deliveries_token
        FOREIGN KEY (device_token_id) REFERENCES device_push_tokens (id) ON DELETE SET NULL,
    CONSTRAINT ck_notification_deliveries_channel CHECK (channel IN ('PUSH', 'EMAIL')),
    CONSTRAINT ck_notification_deliveries_status
        CHECK (status IN ('PENDING', 'IN_FLIGHT', 'SENT', 'FAILED', 'SKIPPED')),
    CONSTRAINT ck_notification_deliveries_attempts CHECK (attempt_count >= 0)
);
CREATE INDEX ix_notification_deliveries_due
    ON notification_deliveries (next_attempt_at) WHERE status IN ('PENDING', 'IN_FLIGHT');
CREATE INDEX ix_notification_deliveries_notification ON notification_deliveries (notification_id);

CREATE TABLE notification_preferences (
    user_id  uuid        NOT NULL,
    category varchar(10) NOT NULL,
    channel  varchar(6)  NOT NULL,
    enabled  boolean     NOT NULL,
    CONSTRAINT pk_notification_preferences PRIMARY KEY (user_id, category, channel),
    CONSTRAINT fk_notification_preferences_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_notification_preferences_category
        CHECK (category IN ('SECURITY', 'REMINDER', 'TASK', 'HABIT', 'FINANCE', 'SYSTEM')),
    CONSTRAINT ck_notification_preferences_channel CHECK (channel IN ('IN_APP', 'PUSH', 'EMAIL'))
);
