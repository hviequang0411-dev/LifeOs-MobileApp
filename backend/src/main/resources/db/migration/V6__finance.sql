-- Phase 3 Personal Finance (docs/phases/PHASE_3_FINANCE.md §5; D100–D119). Đặc tả gọi là "V5__finance" nhưng V5 đã
-- dùng cho Phase 2 nên migration này là V6 (không sửa V1–V5). Tiền: NUMERIC(19,4), luôn dương (< 10^13) trừ số dư đầu
-- kỳ (D100). Ràng buộc chéo (danh mục đúng loại, tài khoản chưa lưu trữ, tiền tệ khớp tài khoản) kiểm ở service.
-- FK không có ON DELETE: hệ thống chỉ xóa mềm.

CREATE TABLE financial_accounts (
    id              uuid          NOT NULL,
    user_id         uuid          NOT NULL,
    name            varchar(60)   NOT NULL,
    account_type    varchar(12)   NOT NULL,
    currency        char(3)       NOT NULL,
    opening_balance numeric(19,4) NOT NULL DEFAULT 0,
    archived_at     timestamptz,
    version         bigint        NOT NULL DEFAULT 1,
    deleted_at      timestamptz,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_financial_accounts PRIMARY KEY (id),
    CONSTRAINT fk_financial_accounts_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_financial_accounts_name CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_financial_accounts_type CHECK (account_type IN ('CASH', 'BANK', 'E_WALLET', 'CREDIT_CARD', 'OTHER')),
    CONSTRAINT ck_financial_accounts_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_financial_accounts_opening
        CHECK (opening_balance > -10000000000000 AND opening_balance < 10000000000000)
);
CREATE UNIQUE INDEX ux_financial_accounts_user_name
    ON financial_accounts (user_id, lower(name)) WHERE deleted_at IS NULL;
CREATE INDEX ix_financial_accounts_user_live
    ON financial_accounts (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;

CREATE TABLE financial_categories (
    id          uuid        NOT NULL,
    user_id     uuid        NOT NULL,
    name        varchar(50) NOT NULL,
    kind        varchar(7)  NOT NULL,
    system_key  varchar(30),
    archived_at timestamptz,
    version     bigint      NOT NULL DEFAULT 1,
    deleted_at  timestamptz,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_financial_categories PRIMARY KEY (id),
    CONSTRAINT fk_financial_categories_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_financial_categories_name CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_financial_categories_kind CHECK (kind IN ('INCOME', 'EXPENSE'))
);
CREATE UNIQUE INDEX ux_financial_categories_user_kind_name
    ON financial_categories (user_id, kind, lower(name)) WHERE deleted_at IS NULL;
-- D113: danh mục hệ thống không trùng theo system_key (job bù idempotent).
CREATE UNIQUE INDEX ux_financial_categories_user_kind_system_key
    ON financial_categories (user_id, kind, system_key) WHERE system_key IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX ix_financial_categories_user_live
    ON financial_categories (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;

CREATE TABLE recurring_transactions (
    id             uuid          NOT NULL,
    user_id        uuid          NOT NULL,
    account_id     uuid          NOT NULL,
    category_id    uuid,
    kind           varchar(7)    NOT NULL,
    amount         numeric(19,4) NOT NULL,
    currency       char(3)       NOT NULL,
    note           varchar(500),
    frequency      varchar(7)    NOT NULL,
    interval_count smallint      NOT NULL DEFAULT 1,
    start_on       date          NOT NULL,
    end_on         date,
    next_run_on    date          NOT NULL,
    status         varchar(6)    NOT NULL DEFAULT 'ACTIVE',
    version        bigint        NOT NULL DEFAULT 1,
    deleted_at     timestamptz,
    created_at     timestamptz   NOT NULL DEFAULT now(),
    updated_at     timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_recurring_transactions PRIMARY KEY (id),
    CONSTRAINT fk_recurring_transactions_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_recurring_transactions_account_id FOREIGN KEY (account_id) REFERENCES financial_accounts (id),
    CONSTRAINT fk_recurring_transactions_category_id FOREIGN KEY (category_id) REFERENCES financial_categories (id),
    CONSTRAINT ck_recurring_transactions_kind CHECK (kind IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_recurring_transactions_amount CHECK (amount > 0 AND amount < 10000000000000),
    CONSTRAINT ck_recurring_transactions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_recurring_transactions_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    CONSTRAINT ck_recurring_transactions_interval CHECK (interval_count BETWEEN 1 AND 365),
    CONSTRAINT ck_recurring_transactions_status CHECK (status IN ('ACTIVE', 'PAUSED')),
    CONSTRAINT ck_recurring_transactions_end CHECK (end_on IS NULL OR end_on >= start_on)
);
-- Phục vụ RecurringTransactionJob (§7).
CREATE INDEX ix_recurring_transactions_due
    ON recurring_transactions (status, next_run_on) WHERE deleted_at IS NULL AND status = 'ACTIVE';
CREATE INDEX ix_recurring_transactions_user_live
    ON recurring_transactions (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_recurring_transactions_account
    ON recurring_transactions (user_id, account_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_recurring_transactions_category
    ON recurring_transactions (user_id, category_id) WHERE deleted_at IS NULL AND category_id IS NOT NULL;

CREATE TABLE financial_transactions (
    id                       uuid          NOT NULL,
    user_id                  uuid          NOT NULL,
    account_id               uuid          NOT NULL,
    category_id              uuid,
    kind                     varchar(7)    NOT NULL,
    amount                   numeric(19,4) NOT NULL,
    currency                 char(3)       NOT NULL,
    occurred_on              date          NOT NULL,
    note                     varchar(500),
    recurring_transaction_id uuid,
    recurrence_date          date,
    version                  bigint        NOT NULL DEFAULT 1,
    deleted_at               timestamptz,
    created_at               timestamptz   NOT NULL DEFAULT now(),
    updated_at               timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_financial_transactions PRIMARY KEY (id),
    CONSTRAINT fk_financial_transactions_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_financial_transactions_account_id FOREIGN KEY (account_id) REFERENCES financial_accounts (id),
    CONSTRAINT fk_financial_transactions_category_id FOREIGN KEY (category_id) REFERENCES financial_categories (id),
    CONSTRAINT fk_financial_transactions_recurring_id
        FOREIGN KEY (recurring_transaction_id) REFERENCES recurring_transactions (id),
    CONSTRAINT ck_financial_transactions_kind CHECK (kind IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_financial_transactions_amount CHECK (amount > 0 AND amount < 10000000000000),
    CONSTRAINT ck_financial_transactions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_financial_transactions_recurrence
        CHECK ((recurring_transaction_id IS NULL) = (recurrence_date IS NULL))
);
-- D110: kể cả tombstone, để xóa một giao dịch đã sinh không làm job sinh lại.
CREATE UNIQUE INDEX ux_financial_transactions_recurrence
    ON financial_transactions (recurring_transaction_id, recurrence_date) WHERE recurring_transaction_id IS NOT NULL;
CREATE INDEX ix_financial_transactions_user_account_date
    ON financial_transactions (user_id, account_id, occurred_on) WHERE deleted_at IS NULL;
CREATE INDEX ix_financial_transactions_user_date
    ON financial_transactions (user_id, occurred_on DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_financial_transactions_user_category_date
    ON financial_transactions (user_id, category_id, occurred_on) WHERE deleted_at IS NULL AND category_id IS NOT NULL;

CREATE TABLE account_transfers (
    id              uuid          NOT NULL,
    user_id         uuid          NOT NULL,
    from_account_id uuid          NOT NULL,
    to_account_id   uuid          NOT NULL,
    amount          numeric(19,4) NOT NULL,
    currency        char(3)       NOT NULL,
    occurred_on     date          NOT NULL,
    note            varchar(500),
    version         bigint        NOT NULL DEFAULT 1,
    deleted_at      timestamptz,
    created_at      timestamptz   NOT NULL DEFAULT now(),
    updated_at      timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_account_transfers PRIMARY KEY (id),
    CONSTRAINT fk_account_transfers_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_account_transfers_from FOREIGN KEY (from_account_id) REFERENCES financial_accounts (id),
    CONSTRAINT fk_account_transfers_to FOREIGN KEY (to_account_id) REFERENCES financial_accounts (id),
    CONSTRAINT ck_account_transfers_amount CHECK (amount > 0 AND amount < 10000000000000),
    CONSTRAINT ck_account_transfers_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_account_transfers_accounts CHECK (from_account_id <> to_account_id)
);
CREATE INDEX ix_account_transfers_user_from_date
    ON account_transfers (user_id, from_account_id, occurred_on) WHERE deleted_at IS NULL;
CREATE INDEX ix_account_transfers_user_to_date
    ON account_transfers (user_id, to_account_id, occurred_on) WHERE deleted_at IS NULL;
CREATE INDEX ix_account_transfers_user_date
    ON account_transfers (user_id, occurred_on DESC, id DESC) WHERE deleted_at IS NULL;

CREATE TABLE budgets (
    id          uuid          NOT NULL,
    user_id     uuid          NOT NULL,
    category_id uuid,
    currency    char(3)       NOT NULL,
    amount      numeric(19,4) NOT NULL,
    archived_at timestamptz,
    version     bigint        NOT NULL DEFAULT 1,
    deleted_at  timestamptz,
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT fk_budgets_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_budgets_category_id FOREIGN KEY (category_id) REFERENCES financial_categories (id),
    CONSTRAINT ck_budgets_amount CHECK (amount > 0 AND amount < 10000000000000),
    CONSTRAINT ck_budgets_currency CHECK (currency ~ '^[A-Z]{3}$')
);
-- D109: một ngân sách đang hoạt động cho mỗi (user, danh mục hoặc tổng, tiền tệ).
CREATE UNIQUE INDEX ux_budgets_active
    ON budgets (user_id, COALESCE(category_id, '00000000-0000-0000-0000-000000000000'::uuid), currency)
    WHERE deleted_at IS NULL AND archived_at IS NULL;
CREATE INDEX ix_budgets_user_live ON budgets (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_budgets_category ON budgets (user_id, category_id) WHERE deleted_at IS NULL AND category_id IS NOT NULL;

-- UC-FIN-06 / FIN-13: ngưỡng 80%/100% báo đúng một lần mỗi (ngân sách, tháng, ngưỡng). Không đồng bộ.
CREATE TABLE budget_alerts (
    id         uuid        NOT NULL,
    user_id    uuid        NOT NULL,
    budget_id  uuid        NOT NULL,
    month      date        NOT NULL,
    threshold  smallint    NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT pk_budget_alerts PRIMARY KEY (id),
    CONSTRAINT fk_budget_alerts_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_budget_alerts_budget_id FOREIGN KEY (budget_id) REFERENCES budgets (id),
    CONSTRAINT ck_budget_alerts_threshold CHECK (threshold IN (80, 100)),
    CONSTRAINT ck_budget_alerts_month CHECK (extract(day FROM month) = 1),
    CONSTRAINT ux_budget_alerts_budget_month_threshold UNIQUE (budget_id, month, threshold)
);
CREATE INDEX ix_budget_alerts_user ON budget_alerts (user_id);

CREATE TABLE saving_goals (
    id            uuid          NOT NULL,
    user_id       uuid          NOT NULL,
    name          varchar(80)   NOT NULL,
    currency      char(3)       NOT NULL,
    target_amount numeric(19,4) NOT NULL,
    target_on     date,
    status        varchar(8)    NOT NULL DEFAULT 'ACTIVE',
    version       bigint        NOT NULL DEFAULT 1,
    deleted_at    timestamptz,
    created_at    timestamptz   NOT NULL DEFAULT now(),
    updated_at    timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_saving_goals PRIMARY KEY (id),
    CONSTRAINT fk_saving_goals_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_saving_goals_name CHECK (length(btrim(name)) > 0),
    CONSTRAINT ck_saving_goals_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_saving_goals_target CHECK (target_amount > 0 AND target_amount < 10000000000000),
    CONSTRAINT ck_saving_goals_status CHECK (status IN ('ACTIVE', 'ACHIEVED', 'ARCHIVED'))
);
CREATE INDEX ix_saving_goals_user_live ON saving_goals (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL;

CREATE TABLE saving_contributions (
    id          uuid          NOT NULL,
    user_id     uuid          NOT NULL,
    goal_id     uuid          NOT NULL,
    amount      numeric(19,4) NOT NULL,
    occurred_on date          NOT NULL,
    note        varchar(500),
    version     bigint        NOT NULL DEFAULT 1,
    deleted_at  timestamptz,
    created_at  timestamptz   NOT NULL DEFAULT now(),
    updated_at  timestamptz   NOT NULL DEFAULT now(),
    CONSTRAINT pk_saving_contributions PRIMARY KEY (id),
    CONSTRAINT fk_saving_contributions_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_saving_contributions_goal_id FOREIGN KEY (goal_id) REFERENCES saving_goals (id),
    CONSTRAINT ck_saving_contributions_amount CHECK (amount > 0 AND amount < 10000000000000)
);
CREATE INDEX ix_saving_contributions_user_goal
    ON saving_contributions (user_id, goal_id, occurred_on DESC, id DESC) WHERE deleted_at IS NULL;

ALTER TABLE change_logs DROP CONSTRAINT ck_change_logs_entity_type;
ALTER TABLE change_logs ADD CONSTRAINT ck_change_logs_entity_type CHECK (entity_type IN (
    'task', 'project', 'category', 'user_settings',
    'habit', 'habit_checkin', 'routine', 'routine_step', 'routine_log', 'routine_step_log',
    'financial_account', 'financial_category', 'financial_transaction', 'account_transfer', 'budget',
    'recurring_transaction', 'saving_goal', 'saving_contribution'));
