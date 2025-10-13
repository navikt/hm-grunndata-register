CREATE TABLE IF NOT EXISTS hidden_part_v1 (
    product_id UUID PRIMARY KEY REFERENCES product_reg_v1(id) ON DELETE CASCADE,
    reason TEXT NULL,
    created TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS hidden_part_v1_created_idx ON hidden_part_v1(created);

