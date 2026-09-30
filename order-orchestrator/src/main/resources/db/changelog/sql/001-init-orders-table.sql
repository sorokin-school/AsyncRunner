-- changeset init-orders-:001

-- TODO остальные поля, необходимые индексы
CREATE TABLE IF NOT EXISTS orders
(
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),
    address             TEXT             NOT NULL,
    description         TEXT             NOT NULL,
    client_estimate     NUMERIC(19,2)    NOT NULL,
    authorized_amount   NUMERIC(19,2),
    captured_amount     NUMERIC(19,2),
    final_amount        NUMERIC(19,2),
    payment_status      VARCHAR(32)      NOT NULL,
    failure_reason      TEXT,
    created_at          TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_orders_payment_status ON orders (payment_status);