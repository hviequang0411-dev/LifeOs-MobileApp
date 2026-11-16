CREATE TABLE users (
    id                uuid         NOT NULL DEFAULT gen_random_uuid(),
    email             varchar(254) NOT NULL,           -- đã chuẩn hóa: trim + lowercase
    password_hash     varchar(255) NOT NULL,
    display_name      varchar(100) NOT NULL,
    email_verified_at timestamptz,
    status            varchar(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at        timestamptz  NOT NULL DEFAULT now(),
    updated_at        timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

CREATE TABLE user_settings (                -- entity đồng bộ, đúng 1 dòng / user
    id               uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id          uuid        NOT NULL,
    timezone         varchar(64) NOT NULL,           -- IANA; kiểm tra bằng ZoneId ở service
    locale           varchar(10) NOT NULL DEFAULT 'vi-VN',
    default_currency char(3)     NOT NULL DEFAULT 'VND',
    week_start_day   smallint    NOT NULL DEFAULT 1, -- 1 = Thứ Hai … 7 = Chủ Nhật (ISO-8601)
    version          bigint      NOT NULL DEFAULT 1,
    deleted_at       timestamptz,
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_settings PRIMARY KEY (id),
    CONSTRAINT fk_user_settings_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ux_user_settings_user UNIQUE (user_id),
    CONSTRAINT ck_user_settings_week_start CHECK (week_start_day BETWEEN 1 AND 7),
    CONSTRAINT ck_user_settings_currency CHECK (default_currency ~ '^[A-Z]{3}$')
);

CREATE TABLE devices (
    id           uuid        NOT NULL DEFAULT gen_random_uuid(),   -- server cấp, client lưu
    user_id      uuid        NOT NULL,
    name         varchar(100) NOT NULL,
    platform     varchar(10) NOT NULL,
    app_version  varchar(32),
    last_seen_at timestamptz NOT NULL DEFAULT now(),
    revoked_at   timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_devices PRIMARY KEY (id),
    CONSTRAINT fk_devices_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_devices_platform CHECK (platform IN ('ANDROID', 'IOS'))
);
CREATE INDEX ix_devices_user_active ON devices (user_id) WHERE revoked_at IS NULL;

CREATE TABLE refresh_tokens (
    id             uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id        uuid        NOT NULL,
    device_id      uuid        NOT NULL,
    family_id      uuid        NOT NULL,            -- một chuỗi xoay vòng = một family
    token_hash     char(64)    NOT NULL,            -- SHA-256 hex của token ngẫu nhiên
    family_started_at timestamptz NOT NULL,         -- thời điểm tạo family (đăng nhập); giữ nguyên khi xoay vòng
    issued_at      timestamptz NOT NULL DEFAULT now(),
    expires_at     timestamptz NOT NULL,            -- min(issued_at + 30 ngày, family_started_at + 90 ngày) (D71)
    used_at        timestamptz,                     -- đã xoay vòng (đã đổi lấy token mới)
    revoked_at     timestamptz,
    replaced_by_id uuid,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_refresh_tokens_device_id FOREIGN KEY (device_id) REFERENCES devices (id),
    CONSTRAINT ux_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_device ON refresh_tokens (device_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_expires ON refresh_tokens (expires_at);

CREATE TABLE email_verification_tokens (
    id         uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL,
    token_hash char(64)    NOT NULL,
    expires_at timestamptz NOT NULL,               -- +24 giờ
    used_at    timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id),
    CONSTRAINT fk_email_verification_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ux_email_verification_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_email_verification_tokens_user ON email_verification_tokens (user_id);
CREATE INDEX ix_email_verification_tokens_expires ON email_verification_tokens (expires_at);

CREATE TABLE password_reset_tokens (
    id         uuid        NOT NULL DEFAULT gen_random_uuid(),
    user_id    uuid        NOT NULL,
    token_hash char(64)    NOT NULL,
    expires_at timestamptz NOT NULL,               -- +1 giờ
    used_at    timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_tokens_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ux_password_reset_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_password_reset_tokens_user ON password_reset_tokens (user_id);
CREATE INDEX ix_password_reset_tokens_expires ON password_reset_tokens (expires_at);
