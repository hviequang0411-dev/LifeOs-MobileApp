CREATE TABLE outbox_events (
    id             uuid        NOT NULL DEFAULT gen_random_uuid(),
    event_type     varchar(50) NOT NULL,              -- EMAIL_VERIFICATION | PASSWORD_RESET
    aggregate_type varchar(30),
    aggregate_id   uuid,
    payload        jsonb       NOT NULL,
    status         varchar(12) NOT NULL DEFAULT 'PENDING',
    attempts       integer     NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    locked_until   timestamptz,
    last_error     text,
    created_at     timestamptz NOT NULL DEFAULT now(),
    processed_at   timestamptz,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT ck_outbox_events_status CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),
    CONSTRAINT ck_outbox_events_attempts CHECK (attempts >= 0)
);
CREATE INDEX ix_outbox_due ON outbox_events (next_attempt_at) WHERE status = 'PENDING';
CREATE INDEX ix_outbox_processing ON outbox_events (locked_until) WHERE status = 'PROCESSING';
CREATE INDEX ix_outbox_cleanup ON outbox_events (processed_at) WHERE status IN ('SENT', 'FAILED');
