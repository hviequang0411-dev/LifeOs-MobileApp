-- Phase 2 Habit & Routine (docs/phases/PHASE_2_HABIT_ROUTINE.md §5; D230–D254). Cột chuẩn đồng bộ như projects/tasks.
-- Ràng buộc chéo (ngày check-in theo D231, bước thuộc đúng routine của lượt, giới hạn D237/D241) kiểm ở service.

CREATE TABLE habits (
    id             uuid         NOT NULL,
    user_id        uuid         NOT NULL,
    name           varchar(100) NOT NULL,
    description    varchar(500),
    color          char(7),
    icon           varchar(30),
    schedule_type  varchar(14)  NOT NULL,
    weekdays       smallint,
    times_per_week smallint,
    interval_days  smallint,
    start_on       date         NOT NULL,
    end_on         date,
    reminder_time  time,
    archived_at    timestamptz,
    version        bigint       NOT NULL DEFAULT 1,
    deleted_at     timestamptz,
    created_at     timestamptz  NOT NULL DEFAULT now(),
    updated_at     timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_habits PRIMARY KEY (id),
    CONSTRAINT fk_habits_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_habits_name CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_habits_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_habits_schedule_type
        CHECK (schedule_type IN ('DAILY', 'WEEKDAYS', 'TIMES_PER_WEEK', 'EVERY_N_DAYS')),
    CONSTRAINT ck_habits_weekdays CHECK (weekdays BETWEEN 1 AND 127),
    CONSTRAINT ck_habits_times_per_week CHECK (times_per_week BETWEEN 1 AND 7),
    CONSTRAINT ck_habits_interval_days CHECK (interval_days BETWEEN 2 AND 30),
    CONSTRAINT ck_habits_end_after_start CHECK (end_on IS NULL OR end_on >= start_on),
    CONSTRAINT ck_habits_weekdays_iff CHECK ((schedule_type = 'WEEKDAYS') = (weekdays IS NOT NULL)),
    CONSTRAINT ck_habits_times_iff CHECK ((schedule_type = 'TIMES_PER_WEEK') = (times_per_week IS NOT NULL)),
    CONSTRAINT ck_habits_interval_iff CHECK ((schedule_type = 'EVERY_N_DAYS') = (interval_days IS NOT NULL))
);
CREATE INDEX ix_habits_user_active ON habits (user_id) WHERE deleted_at IS NULL AND archived_at IS NULL;
CREATE INDEX ix_habits_user_live ON habits (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;

CREATE TABLE habit_checkins (
    id         uuid         NOT NULL,
    user_id    uuid         NOT NULL,
    habit_id   uuid         NOT NULL,
    local_date date         NOT NULL,
    status     varchar(7)   NOT NULL,
    note       varchar(200),
    version    bigint       NOT NULL DEFAULT 1,
    deleted_at timestamptz,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_habit_checkins PRIMARY KEY (id),
    CONSTRAINT fk_habit_checkins_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_habit_checkins_habit_id FOREIGN KEY (habit_id) REFERENCES habits (id),
    CONSTRAINT ck_habit_checkins_status CHECK (status IN ('DONE', 'SKIPPED'))
);
CREATE UNIQUE INDEX ux_habit_checkins_habit_date ON habit_checkins (habit_id, local_date) WHERE deleted_at IS NULL;
CREATE INDEX ix_habit_checkins_user_habit_date
    ON habit_checkins (user_id, habit_id, local_date DESC) WHERE deleted_at IS NULL;

CREATE TABLE routines (
    id            uuid         NOT NULL,
    user_id       uuid         NOT NULL,
    name          varchar(100) NOT NULL,
    kind          varchar(6)   NOT NULL,
    weekdays      smallint,
    reminder_time time,
    archived_at   timestamptz,
    version       bigint       NOT NULL DEFAULT 1,
    deleted_at    timestamptz,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_routines PRIMARY KEY (id),
    CONSTRAINT fk_routines_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_routines_name CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_routines_kind CHECK (kind IN ('DAILY', 'WEEKLY')),
    CONSTRAINT ck_routines_weekdays CHECK (weekdays BETWEEN 1 AND 127),
    CONSTRAINT ck_routines_weekdays_iff CHECK ((kind = 'WEEKLY') = (weekdays IS NOT NULL))
);
CREATE INDEX ix_routines_user_live ON routines (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;

CREATE TABLE routine_steps (
    id               uuid         NOT NULL,
    user_id          uuid         NOT NULL,
    routine_id       uuid         NOT NULL,
    title            varchar(100) NOT NULL,
    position         bigint       NOT NULL,
    duration_minutes smallint,
    version          bigint       NOT NULL DEFAULT 1,
    deleted_at       timestamptz,
    created_at       timestamptz  NOT NULL DEFAULT now(),
    updated_at       timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_routine_steps PRIMARY KEY (id),
    CONSTRAINT fk_routine_steps_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_routine_steps_routine_id FOREIGN KEY (routine_id) REFERENCES routines (id),
    CONSTRAINT ck_routine_steps_title CHECK (length(btrim(title)) > 0),
    CONSTRAINT ck_routine_steps_duration CHECK (duration_minutes BETWEEN 1 AND 240)
);
CREATE UNIQUE INDEX ux_routine_steps_routine_position ON routine_steps (routine_id, position) WHERE deleted_at IS NULL;

CREATE TABLE routine_logs (
    id           uuid        NOT NULL,
    user_id      uuid        NOT NULL,
    routine_id   uuid        NOT NULL,
    local_date   date        NOT NULL,
    total_steps  smallint    NOT NULL,
    started_at   timestamptz NOT NULL,
    completed_at timestamptz,
    version      bigint      NOT NULL DEFAULT 1,
    deleted_at   timestamptz,
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_routine_logs PRIMARY KEY (id),
    CONSTRAINT fk_routine_logs_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_routine_logs_routine_id FOREIGN KEY (routine_id) REFERENCES routines (id),
    CONSTRAINT ck_routine_logs_total_steps CHECK (total_steps BETWEEN 1 AND 30)
);
CREATE UNIQUE INDEX ux_routine_logs_routine_date ON routine_logs (routine_id, local_date) WHERE deleted_at IS NULL;
CREATE INDEX ix_routine_logs_user_routine_date
    ON routine_logs (user_id, routine_id, local_date DESC) WHERE deleted_at IS NULL;

CREATE TABLE routine_step_logs (
    id             uuid        NOT NULL,
    user_id        uuid        NOT NULL,
    routine_log_id uuid        NOT NULL,
    step_id        uuid        NOT NULL,
    status         varchar(7)  NOT NULL,
    version        bigint      NOT NULL DEFAULT 1,
    deleted_at     timestamptz,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_routine_step_logs PRIMARY KEY (id),
    CONSTRAINT fk_routine_step_logs_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_routine_step_logs_log_id FOREIGN KEY (routine_log_id) REFERENCES routine_logs (id),
    CONSTRAINT fk_routine_step_logs_step_id FOREIGN KEY (step_id) REFERENCES routine_steps (id),
    CONSTRAINT ck_routine_step_logs_status CHECK (status IN ('DONE', 'SKIPPED'))
);
CREATE UNIQUE INDEX ux_routine_step_logs_log_step
    ON routine_step_logs (routine_log_id, step_id) WHERE deleted_at IS NULL;

-- entity_type mới cho change_logs (V2 chỉ cho task/project/category/user_settings). Không sửa V2: thay ràng buộc.
ALTER TABLE change_logs DROP CONSTRAINT ck_change_logs_entity_type;
ALTER TABLE change_logs ADD CONSTRAINT ck_change_logs_entity_type CHECK (entity_type IN (
    'task', 'project', 'category', 'user_settings',
    'habit', 'habit_checkin', 'routine', 'routine_step', 'routine_log', 'routine_step_log'));
