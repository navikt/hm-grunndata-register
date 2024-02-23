CREATE TABLE delkontrakt_reg_v1(
    id UUID PRIMARY KEY,
    agreement_id UUID,
    delkontrakt_data JSONB NOT NULL,
    created_by VARCHAR(32),
    updated_by VARCHAR(32),
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
