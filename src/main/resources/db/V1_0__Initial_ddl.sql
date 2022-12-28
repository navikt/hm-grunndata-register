CREATE EXTENSION pgcrypto;


CREATE TABLE IF NOT EXISTS supplier_v1 (
    id uuid NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    info JSONB NOT NULL,
    identifier VARCHAR(128) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unique(name),
    unique(identifier)
);

CREATE TABLE IF NOT EXISTS user_v1 (
    id uuid NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    roles JSONB NOT NULL,
    attributes JSONB NOT NULL,
    token TEXT NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(email)
);

CREATE TABLE IF NOT EXISTS product_reg_v1 (
    id uuid NOT NULL PRIMARY KEY,
    supplier_id uuid NOT NULL,
    supplier_ref VARCHAR(255) NOT NULL,
    hms_artnr VARCHAR(255),
    title VARCHAR(512) NOT NULL,
    draft VARCHAR(32) NOT NULL,
    admin_status VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    message TEXT,
    admin_info JSONB,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published TIMESTAMP,
    expired TIMESTAMP,
    created_by_user VARCHAR(255) NOT NULL,
    updated_by_user VARCHAR(255) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_by_admin BOOLEAN NOT NULL,
    product_dto JSONB NOT NULL,
    version BIGINT NOT NULL,
    UNIQUE (supplier_id, supplier_ref)
);
