-- changeset init-orders-:002


CREATE TABLE IF NOT EXISTS tasks
(
            id                  BIGSERIAL PRIMARY KEY,
            order_id            UUID                NOT NULL,
            task_status         VARCHAR(32)         NOT NULL,
            attempts            INTEGER,
            next_attempt_at     TIMESTAMPTZ,
            created_at          TIMESTAMPTZ         NOT NULL,
            updated_at          TIMESTAMPTZ         NOT NULL

    );

CREATE INDEX IF NOT EXISTS idx_tasks_order_id ON tasks (order_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status_next_attempt ON tasks (task_status, next_attempt_at);
