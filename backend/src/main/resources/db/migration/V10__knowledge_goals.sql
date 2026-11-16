-- Phase 6 Knowledge & Goals (docs/phases/PHASE_6_KNOWLEDGE_GOALS.md §5; D180–D189). Đặc tả gọi là V9; đánh số V10.
-- BACKEND_MASTER_PLAN §5 #7: extension unaccent + cấu hình 'simple'.

CREATE EXTENSION IF NOT EXISTS unaccent;

-- unaccent() chỉ STABLE nên không dùng trực tiếp được trong cột GENERATED; bọc bằng hàm IMMUTABLE gọi từ điển cố định
-- (cách chuẩn của PostgreSQL). Dùng cùng hàm này ở truy vấn để chỉ mục GIN khớp.
CREATE OR REPLACE FUNCTION lifeos_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    AS $$ SELECT public.unaccent('public.unaccent'::regdictionary, $1) $$;

CREATE TABLE notes (
    id            uuid         NOT NULL,
    user_id       uuid         NOT NULL,
    title         varchar(200),
    content       text         NOT NULL DEFAULT '',
    pinned        boolean      NOT NULL DEFAULT false,
    archived_at   timestamptz,
    search_vector tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('simple', lifeos_unaccent(coalesce(title, ''))), 'A')
        || setweight(to_tsvector('simple', lifeos_unaccent(content)), 'B')) STORED,
    version       bigint       NOT NULL DEFAULT 1,
    deleted_at    timestamptz,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_notes PRIMARY KEY (id),
    CONSTRAINT fk_notes_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_notes_content CHECK (char_length(content) <= 50000)
);
CREATE INDEX ix_notes_search ON notes USING gin (search_vector) WHERE deleted_at IS NULL;
CREATE INDEX ix_notes_user_updated ON notes (user_id, updated_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_notes_tombstone ON notes (deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE tags (
    id         uuid        NOT NULL,
    user_id    uuid        NOT NULL,
    name       varchar(30) NOT NULL,
    color      char(7),
    version    bigint      NOT NULL DEFAULT 1,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_tags PRIMARY KEY (id),
    CONSTRAINT fk_tags_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_tags_name CHECK (name = btrim(name) AND char_length(name) >= 1),
    CONSTRAINT ck_tags_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);
CREATE UNIQUE INDEX ux_tags_user_name ON tags (user_id, lower(name)) WHERE deleted_at IS NULL;
CREATE INDEX ix_tags_tombstone ON tags (deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE note_tags (
    id         uuid        NOT NULL,
    user_id    uuid        NOT NULL,
    note_id    uuid        NOT NULL,
    tag_id     uuid        NOT NULL,
    version    bigint      NOT NULL DEFAULT 1,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_note_tags PRIMARY KEY (id),
    CONSTRAINT fk_note_tags_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_note_tags_note_id FOREIGN KEY (note_id) REFERENCES notes (id),
    CONSTRAINT fk_note_tags_tag_id FOREIGN KEY (tag_id) REFERENCES tags (id)
);
CREATE UNIQUE INDEX ux_note_tags_pair ON note_tags (note_id, tag_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_note_tags_user_tag ON note_tags (user_id, tag_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_note_tags_tombstone ON note_tags (deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE goals (
    id                uuid          NOT NULL,
    user_id           uuid          NOT NULL,
    title             varchar(200)  NOT NULL,
    description       varchar(2000),
    start_on          date,
    target_on         date,
    status            varchar(9)    NOT NULL DEFAULT 'ACTIVE',
    status_changed_at timestamptz,
    version           bigint        NOT NULL DEFAULT 1,
    deleted_at        timestamptz,
    created_at        timestamptz   NOT NULL DEFAULT now(),
    updated_at        timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_goals PRIMARY KEY (id),
    CONSTRAINT fk_goals_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_goals_title CHECK (length(btrim(title)) > 0),
    CONSTRAINT ck_goals_status CHECK (status IN ('ACTIVE', 'PAUSED', 'ACHIEVED', 'ABANDONED')),
    CONSTRAINT ck_goals_range CHECK (target_on IS NULL OR start_on IS NULL OR target_on >= start_on)
);
CREATE INDEX ix_goals_user_live ON goals (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_goals_tombstone ON goals (deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE milestones (
    id           uuid         NOT NULL,
    user_id      uuid         NOT NULL,
    goal_id      uuid         NOT NULL,
    title        varchar(200) NOT NULL,
    due_on       date,
    position     bigint       NOT NULL,
    completed_at timestamptz,
    version      bigint       NOT NULL DEFAULT 1,
    deleted_at   timestamptz,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_milestones PRIMARY KEY (id),
    CONSTRAINT fk_milestones_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_milestones_goal_id FOREIGN KEY (goal_id) REFERENCES goals (id),
    CONSTRAINT ck_milestones_title CHECK (length(btrim(title)) > 0)
);
CREATE UNIQUE INDEX ux_milestones_goal_position ON milestones (goal_id, position) WHERE deleted_at IS NULL;
CREATE INDEX ix_milestones_goal ON milestones (goal_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_milestones_tombstone ON milestones (deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE note_links (
    id          uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    note_id     uuid        NOT NULL,
    target_type varchar(4)  NOT NULL,
    target_id   uuid        NOT NULL,
    version     bigint      NOT NULL DEFAULT 1,
    deleted_at  timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_note_links PRIMARY KEY (id),
    CONSTRAINT fk_note_links_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_note_links_note_id FOREIGN KEY (note_id) REFERENCES notes (id),
    CONSTRAINT ck_note_links_target CHECK (target_type IN ('TASK', 'GOAL'))
);
CREATE UNIQUE INDEX ux_note_links_triple ON note_links (note_id, target_type, target_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_note_links_target ON note_links (user_id, target_type, target_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_note_links_tombstone ON note_links (deleted_at) WHERE deleted_at IS NOT NULL;

-- BACKEND_MASTER_PLAN §5 #2 (D186): task thuộc tối đa một milestone (và qua đó một goal).
ALTER TABLE tasks ADD COLUMN goal_id uuid;
ALTER TABLE tasks ADD COLUMN milestone_id uuid;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_goal_id FOREIGN KEY (goal_id) REFERENCES goals (id);
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_milestone_id FOREIGN KEY (milestone_id) REFERENCES milestones (id);
ALTER TABLE tasks ADD CONSTRAINT ck_tasks_milestone_needs_goal CHECK (milestone_id IS NULL OR goal_id IS NOT NULL);
CREATE INDEX ix_tasks_user_goal ON tasks (user_id, goal_id) WHERE deleted_at IS NULL AND goal_id IS NOT NULL;
CREATE INDEX ix_tasks_user_milestone ON tasks (user_id, milestone_id) WHERE deleted_at IS NULL AND milestone_id IS NOT NULL;

ALTER TABLE change_logs DROP CONSTRAINT ck_change_logs_entity_type;
ALTER TABLE change_logs ADD CONSTRAINT ck_change_logs_entity_type CHECK (entity_type IN (
    'task', 'project', 'category', 'user_settings',
    'habit', 'habit_checkin', 'routine', 'routine_step', 'routine_log', 'routine_step_log',
    'financial_account', 'financial_category', 'financial_transaction', 'account_transfer', 'budget',
    'recurring_transaction', 'saving_goal', 'saving_contribution',
    'calendar_event', 'event_exception', 'time_block', 'reminder',
    'note', 'tag', 'note_tag', 'note_link', 'goal', 'milestone'));
