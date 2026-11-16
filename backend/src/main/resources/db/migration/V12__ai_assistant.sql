-- Phase 8 AI Assistant (D260-D299). Additive, server-only; V1-V11 are immutable.

CREATE TABLE ai_user_preferences (
    user_id                    uuid        NOT NULL,
    allow_task_calendar        boolean     NOT NULL DEFAULT false,
    allow_habit                boolean     NOT NULL DEFAULT false,
    allow_goal                 boolean     NOT NULL DEFAULT false,
    allow_notes                boolean     NOT NULL DEFAULT false,
    allow_finance              boolean     NOT NULL DEFAULT false,
    policy_version             varchar(20) NOT NULL,
    accepted_provider_terms_at timestamptz,
    created_at                 timestamptz NOT NULL DEFAULT now(),
    updated_at                 timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_ai_user_preferences PRIMARY KEY (user_id),
    CONSTRAINT fk_ai_user_preferences_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE ai_conversations (
    id              uuid         NOT NULL,
    user_id         uuid         NOT NULL,
    title           varchar(200) NOT NULL,
    status          varchar(10)  NOT NULL,
    last_message_at timestamptz  NOT NULL,
    expires_at      timestamptz  NOT NULL,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    updated_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_ai_conversations PRIMARY KEY (id),
    CONSTRAINT fk_ai_conversations_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_ai_conversations_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);
CREATE INDEX ix_ai_conversations_user_last ON ai_conversations (user_id, last_message_at DESC, id DESC);
CREATE INDEX ix_ai_conversations_expiry ON ai_conversations (expires_at, id);

CREATE TABLE ai_messages (
    id                 uuid        NOT NULL,
    user_id            uuid        NOT NULL,
    conversation_id    uuid        NOT NULL,
    role               varchar(9)  NOT NULL,
    kind               varchar(20) NOT NULL,
    content            text        NOT NULL,
    structured_payload jsonb,
    context_snapshot   jsonb,
    prompt_version     varchar(40),
    provider           varchar(30),
    model              varchar(80),
    created_at         timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_ai_messages PRIMARY KEY (id),
    CONSTRAINT fk_ai_messages_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ai_messages_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations (id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_messages_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_ai_messages_content CHECK (char_length(content) <= 16000),
    CONSTRAINT ck_ai_messages_structured_object CHECK (structured_payload IS NULL OR jsonb_typeof(structured_payload) = 'object'),
    CONSTRAINT ck_ai_messages_context_object CHECK (context_snapshot IS NULL OR jsonb_typeof(context_snapshot) = 'object')
);
CREATE INDEX ix_ai_messages_conversation ON ai_messages (user_id, conversation_id, created_at, id);

CREATE TABLE ai_usage_logs (
    id                      uuid        NOT NULL,
    user_id                 uuid        NOT NULL,
    conversation_id         uuid,
    message_id              uuid,
    provider                varchar(30) NOT NULL,
    model                   varchar(80) NOT NULL,
    input_tokens            bigint      NOT NULL,
    cached_input_tokens     bigint      NOT NULL DEFAULT 0,
    output_tokens           bigint      NOT NULL,
    reasoning_tokens        bigint      NOT NULL DEFAULT 0,
    estimated_cost_microusd bigint      NOT NULL,
    latency_ms              bigint      NOT NULL,
    result_code             varchar(40) NOT NULL,
    correlation_id          uuid,
    created_at              timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_ai_usage_logs PRIMARY KEY (id),
    CONSTRAINT fk_ai_usage_logs_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ai_usage_logs_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations (id) ON DELETE SET NULL,
    CONSTRAINT fk_ai_usage_logs_message FOREIGN KEY (message_id) REFERENCES ai_messages (id) ON DELETE SET NULL,
    CONSTRAINT ck_ai_usage_nonnegative CHECK (input_tokens >= 0 AND cached_input_tokens >= 0 AND output_tokens >= 0
        AND reasoning_tokens >= 0 AND estimated_cost_microusd >= 0 AND latency_ms >= 0)
);
CREATE INDEX ix_ai_usage_user_created ON ai_usage_logs (user_id, created_at DESC);
CREATE INDEX ix_ai_usage_created ON ai_usage_logs (created_at DESC);

CREATE TABLE ai_action_proposals (
    id                uuid        NOT NULL,
    user_id           uuid        NOT NULL,
    conversation_id   uuid,
    source_message_id uuid,
    action_type       varchar(40) NOT NULL,
    payload           jsonb       NOT NULL,
    expected_versions jsonb       NOT NULL DEFAULT '{}'::jsonb,
    payload_hash      char(64)    NOT NULL,
    execution_key     uuid        NOT NULL,
    status            varchar(20) NOT NULL,
    version           bigint      NOT NULL DEFAULT 1,
    expires_at        timestamptz NOT NULL,
    approved_at       timestamptz,
    executed_at       timestamptz,
    result            jsonb,
    failure_code      varchar(50),
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_ai_action_proposals PRIMARY KEY (id),
    CONSTRAINT ux_ai_action_proposals_execution UNIQUE (execution_key),
    CONSTRAINT fk_ai_action_proposals_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ai_action_proposals_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_action_proposals_message FOREIGN KEY (source_message_id) REFERENCES ai_messages (id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_action_proposals_type CHECK (action_type IN ('CREATE_TASK', 'UPDATE_TASK_PRIORITY', 'UPDATE_TASK_SCHEDULE')),
    CONSTRAINT ck_ai_action_proposals_status CHECK (status IN ('PROPOSED', 'APPROVED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'EXECUTING', 'EXECUTED', 'FAILED')),
    CONSTRAINT ck_ai_action_proposals_version CHECK (version >= 1),
    CONSTRAINT ck_ai_action_proposals_payload CHECK (jsonb_typeof(payload) = 'object' AND jsonb_typeof(expected_versions) = 'object'),
    CONSTRAINT ck_ai_action_proposals_hash CHECK (payload_hash ~ '^[a-f0-9]{64}$')
);
CREATE INDEX ix_ai_proposals_user_status ON ai_action_proposals (user_id, status, expires_at);
