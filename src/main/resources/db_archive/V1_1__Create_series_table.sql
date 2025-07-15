CREATE TABLE IF NOT EXISTS series_reg_v1 (
    id uuid NOT NULL PRIMARY KEY,
    supplier_id uuid NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    draft_status VARCHAR(32) NOT NULL,
    registration_status VARCHAR(32) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_user VARCHAR(255) NOT NULL,
    updated_by_user VARCHAR(255) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_by_admin BOOLEAN NOT NULL,
    version BIGINT NOT NULL,
    UNIQUE (identifier)
);