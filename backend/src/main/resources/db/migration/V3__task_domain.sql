CREATE TABLE categories (
    id         uuid         NOT NULL,                  -- client tạo
    user_id    uuid         NOT NULL,
    name       varchar(50)  NOT NULL,
    version    bigint       NOT NULL DEFAULT 1,
    deleted_at timestamptz,
    created_at timestamptz  NOT NULL DEFAULT now(),
    updated_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT fk_categories_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_categories_name CHECK (length(btrim(name)) > 0)
);
CREATE UNIQUE INDEX ux_categories_user_name ON categories (user_id, lower(name)) WHERE deleted_at IS NULL;

CREATE TABLE projects (
    id          uuid         NOT NULL,                 -- client tạo
    user_id     uuid         NOT NULL,
    name        varchar(100) NOT NULL,
    description text,
    archived_at timestamptz,
    version     bigint       NOT NULL DEFAULT 1,
    deleted_at  timestamptz,
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_projects_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_projects_name CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_projects_description CHECK (description IS NULL OR char_length(description) <= 10000)
);
CREATE INDEX ix_projects_user_active ON projects (user_id) WHERE deleted_at IS NULL;

CREATE TABLE tasks (
    id           uuid         NOT NULL,                -- client tạo
    user_id      uuid         NOT NULL,
    title        varchar(200) NOT NULL,
    description  text,
    status       varchar(10)  NOT NULL DEFAULT 'OPEN',
    priority     varchar(10)  NOT NULL DEFAULT 'NONE',
    due_date     date,
    due_time     time,                                 -- giờ địa phương theo timezone user, không có múi giờ
    completed_at timestamptz,
    project_id   uuid,
    category_id  uuid,
    version      bigint       NOT NULL DEFAULT 1,
    deleted_at   timestamptz,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT fk_tasks_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_tasks_project_id FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_tasks_category_id FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_tasks_title CHECK (length(btrim(title)) > 0),
    CONSTRAINT ck_tasks_description CHECK (description IS NULL OR char_length(description) <= 10000),
    CONSTRAINT ck_tasks_status CHECK (status IN ('OPEN', 'DONE')),
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('NONE', 'LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT ck_tasks_done_completed CHECK ((status = 'DONE') = (completed_at IS NOT NULL)),
    CONSTRAINT ck_tasks_due_time_needs_date CHECK (due_time IS NULL OR due_date IS NOT NULL)
);
CREATE INDEX ix_tasks_user_list ON tasks (user_id, status, due_date) WHERE deleted_at IS NULL;
CREATE INDEX ix_tasks_project ON tasks (project_id) WHERE deleted_at IS NULL AND project_id IS NOT NULL;
CREATE INDEX ix_tasks_category ON tasks (category_id) WHERE deleted_at IS NULL AND category_id IS NOT NULL;
