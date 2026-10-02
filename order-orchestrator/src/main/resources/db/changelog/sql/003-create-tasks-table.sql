-- changeset init-orders-:003
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS step VARCHAR(32) NOT NULL;
