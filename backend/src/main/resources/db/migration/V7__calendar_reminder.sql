-- Phase 4a/4c/4d (docs/phases/PHASE_4_CALENDAR_NOTIFICATION.md §5; D124–D134). Đặc tả gọi là V6/V7; đánh số V7/V8 vì
-- V5, V6 đã dùng. Lần xuất hiện của sự kiện lặp được TÍNH khi đọc, không lưu (D125).

CREATE TABLE calendar_events (
    id               uuid          NOT NULL,
    user_id          uuid          NOT NULL,
    title            varchar(200)  NOT NULL,
    description      varchar(2000),
    location         varchar(200),
    all_day          boolean       NOT NULL DEFAULT false,
    timezone         varchar(64)   NOT NULL,
    start_local      timestamp,
    end_local        timestamp,
    start_date       date,
    end_date         date,
    start_at         timestamptz   NOT NULL,
    end_at           timestamptz   NOT NULL,
    recurrence       jsonb,
    recurrence_until timestamptz,
    version          bigint        NOT NULL DEFAULT 1,
    deleted_at       timestamptz,
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_at       timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_calendar_events PRIMARY KEY (id),
    CONSTRAINT fk_calendar_events_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_calendar_events_title CHECK (length(btrim(title)) > 0),
    CONSTRAINT ck_calendar_events_shape CHECK (
        (all_day AND start_date IS NOT NULL AND end_date IS NOT NULL AND end_date >= start_date
            AND start_local IS NULL AND end_local IS NULL)
        OR (NOT all_day AND start_local IS NOT NULL AND end_local IS NOT NULL AND end_local > start_local
            AND start_date IS NULL AND end_date IS NULL))
);
CREATE INDEX ix_calendar_events_user_start
    ON calendar_events (user_id, start_at) WHERE deleted_at IS NULL AND recurrence IS NULL;
CREATE INDEX ix_calendar_events_user_recurring
    ON calendar_events (user_id) WHERE deleted_at IS NULL AND recurrence IS NOT NULL;
CREATE INDEX ix_calendar_events_user_live ON calendar_events (user_id, created_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE event_exceptions (
    id                   uuid          NOT NULL,
    user_id              uuid          NOT NULL,
    event_id             uuid          NOT NULL,
    original_start_local timestamp     NOT NULL,
    kind                 varchar(9)    NOT NULL,
    title                varchar(200),
    description          varchar(2000),
    location             varchar(200),
    start_local          timestamp,
    end_local            timestamp,
    version              bigint        NOT NULL DEFAULT 1,
    deleted_at           timestamptz,
    created_at           timestamptz   NOT NULL DEFAULT now(),
    updated_at           timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_event_exceptions PRIMARY KEY (id),
    CONSTRAINT fk_event_exceptions_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_event_exceptions_event_id FOREIGN KEY (event_id) REFERENCES calendar_events (id),
    CONSTRAINT ck_event_exceptions_kind CHECK (kind IN ('CANCELLED', 'MODIFIED')),
    CONSTRAINT ck_event_exceptions_times CHECK (end_local IS NULL OR start_local IS NULL OR end_local > start_local)
);
CREATE UNIQUE INDEX ux_event_exceptions_event_start
    ON event_exceptions (event_id, original_start_local) WHERE deleted_at IS NULL;

CREATE TABLE time_blocks (
    id         uuid         NOT NULL,
    user_id    uuid         NOT NULL,
    task_id    uuid         NOT NULL,
    start_at   timestamptz  NOT NULL,
    end_at     timestamptz  NOT NULL,
    note       varchar(200),
    version    bigint       NOT NULL DEFAULT 1,
    deleted_at timestamptz,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_time_blocks PRIMARY KEY (id),
    CONSTRAINT fk_time_blocks_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_time_blocks_task_id FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT ck_time_blocks_range CHECK (end_at > start_at AND end_at - start_at <= interval '24 hours')
);
CREATE INDEX ix_time_blocks_user_start ON time_blocks (user_id, start_at) WHERE deleted_at IS NULL;
CREATE INDEX ix_time_blocks_task ON time_blocks (task_id) WHERE deleted_at IS NULL;

CREATE TABLE reminders (
    id             uuid        NOT NULL,
    user_id        uuid        NOT NULL,
    target_type    varchar(7)  NOT NULL,
    target_id      uuid,
    mode           varchar(8)  NOT NULL,
    offset_minutes integer,
    local_time     time,
    local_date     date,
    repeat_rule    jsonb,
    urgent         boolean     NOT NULL DEFAULT false,
    status         varchar(6)  NOT NULL DEFAULT 'ACTIVE',
    next_fire_at   timestamptz,
    last_fired_at  timestamptz,
    version        bigint      NOT NULL DEFAULT 1,
    deleted_at     timestamptz,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_reminders PRIMARY KEY (id),
    CONSTRAINT fk_reminders_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_reminders_target_type CHECK (target_type IN ('TASK', 'EVENT', 'HABIT', 'ROUTINE', 'NONE')),
    CONSTRAINT ck_reminders_target CHECK ((target_type = 'NONE') = (target_id IS NULL)),
    CONSTRAINT ck_reminders_mode CHECK (mode IN ('OFFSET', 'LOCAL_AT')),
    CONSTRAINT ck_reminders_offset CHECK (offset_minutes BETWEEN 0 AND 525600),
    CONSTRAINT ck_reminders_mode_fields CHECK (
        (mode = 'OFFSET' AND offset_minutes IS NOT NULL AND local_time IS NULL)
        OR (mode = 'LOCAL_AT' AND local_time IS NOT NULL AND offset_minutes IS NULL)),
    CONSTRAINT ck_reminders_status CHECK (status IN ('ACTIVE', 'DONE', 'PAUSED'))
);
CREATE INDEX ix_reminders_due ON reminders (next_fire_at) WHERE deleted_at IS NULL AND status = 'ACTIVE';
CREATE INDEX ix_reminders_target ON reminders (user_id, target_type, target_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_reminders_user_live ON reminders (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;

-- BACKEND_MASTER_PLAN §5 #1: quiet hours và hiện chi tiết trên màn hình khóa thuộc user_settings (đồng bộ).
ALTER TABLE user_settings ADD COLUMN quiet_start time;
ALTER TABLE user_settings ADD COLUMN quiet_end time;
ALTER TABLE user_settings ADD COLUMN show_details_on_lock_screen boolean NOT NULL DEFAULT false;
ALTER TABLE user_settings ADD CONSTRAINT ck_user_settings_quiet
    CHECK ((quiet_start IS NULL) = (quiet_end IS NULL) AND (quiet_start IS NULL OR quiet_start <> quiet_end));

-- PHASE_2 D250 / §4 kế hoạch: reminder_time của Habit/Routine chuyển thành reminder LOCAL_AT hằng ngày (một lần, khi
-- nâng cấp). next_fire_at = NULL: ReminderScheduler/ReminderService tính lần đầu khi khởi động (ReminderBackfill).
INSERT INTO reminders (id, user_id, target_type, target_id, mode, local_time, status, created_at, updated_at)
SELECT gen_random_uuid(), h.user_id, 'HABIT', h.id, 'LOCAL_AT', h.reminder_time, 'ACTIVE', now(), now()
FROM habits h WHERE h.reminder_time IS NOT NULL AND h.deleted_at IS NULL;
INSERT INTO reminders (id, user_id, target_type, target_id, mode, local_time, status, created_at, updated_at)
SELECT gen_random_uuid(), r.user_id, 'ROUTINE', r.id, 'LOCAL_AT', r.reminder_time, 'ACTIVE', now(), now()
FROM routines r WHERE r.reminder_time IS NOT NULL AND r.deleted_at IS NULL;

ALTER TABLE change_logs DROP CONSTRAINT ck_change_logs_entity_type;
ALTER TABLE change_logs ADD CONSTRAINT ck_change_logs_entity_type CHECK (entity_type IN (
    'task', 'project', 'category', 'user_settings',
    'habit', 'habit_checkin', 'routine', 'routine_step', 'routine_log', 'routine_step_log',
    'financial_account', 'financial_category', 'financial_transaction', 'account_transfer', 'budget',
    'recurring_transaction', 'saving_goal', 'saving_contribution',
    'calendar_event', 'event_exception', 'time_block', 'reminder'));
