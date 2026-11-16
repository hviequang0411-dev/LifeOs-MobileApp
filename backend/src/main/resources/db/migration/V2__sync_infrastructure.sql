CREATE TABLE user_sync_state (
    user_id    uuid        NOT NULL,
    last_seq   bigint      NOT NULL DEFAULT 0,
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_user_sync_state PRIMARY KEY (user_id),
    CONSTRAINT fk_user_sync_state_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_user_sync_state_seq CHECK (last_seq >= 0)
);

CREATE TABLE change_logs (
    user_id        uuid        NOT NULL,
    seq            bigint      NOT NULL,
    entity_type    varchar(30) NOT NULL,
    entity_id      uuid        NOT NULL,
    operation      varchar(10) NOT NULL,
    entity_version bigint      NOT NULL,
    mutation_id    uuid,                               -- NULL nếu thay đổi đến từ REST hoặc cascade
    created_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_change_logs PRIMARY KEY (user_id, seq),
    CONSTRAINT fk_change_logs_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_change_logs_operation CHECK (operation IN ('CREATE', 'UPDATE', 'DELETE')),
    CONSTRAINT ck_change_logs_entity_type CHECK (entity_type IN ('task', 'project', 'category', 'user_settings'))
);
CREATE INDEX ix_change_logs_entity ON change_logs (user_id, entity_type, entity_id);

CREATE TABLE sync_mutations (
    user_id        uuid        NOT NULL,
    mutation_id    uuid        NOT NULL,
    entity_type    varchar(30) NOT NULL,
    entity_id      uuid        NOT NULL,
    operation      varchar(10) NOT NULL,
    base_version   bigint      NOT NULL,
    status         varchar(10) NOT NULL,
    result_version bigint,
    error_code     varchar(60),
    result         jsonb       NOT NULL,              -- phản hồi đã trả, để phát lại khi DUPLICATE
    created_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_sync_mutations PRIMARY KEY (user_id, mutation_id),
    CONSTRAINT fk_sync_mutations_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_sync_mutations_status CHECK (status IN ('APPLIED', 'CONFLICT', 'REJECTED')),
    CONSTRAINT ck_sync_mutations_operation CHECK (operation IN ('CREATE', 'UPDATE', 'DELETE'))
);
CREATE INDEX ix_sync_mutations_entity ON sync_mutations (user_id, entity_type, entity_id);
