CREATE EXTENSION pgcrypto;

CREATE TABLE IF NOT EXISTS user_v1 (
    uuid uuid NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    supplier_uuid uuid NOT NULL,
    token TEXT NOT NULL
);


CREATE TABLE IF NOT EXISTS supplier_v1 (
    uuid uuid NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    homepage VARCHAR(512),
    phone VARCHAR(32),
    email VARCHAR(255),
    identifier VARCHAR(128) NOT NULL,
    unique(name),
    unique(identifier)
);


CREATE TABLE IF NOT EXISTS product_reg_v1 (
    uuid uuid NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    supplier_uuid uuid NOT NULL,
    supplier_ref VARHCAR(255) NOT NULL,
    hms_artnr VARCHAR(255),
    title VARCHAR(512) NOT NULL,
    draft VARCHAR(32) NOT NULL,
    admin_status VARCHAR(32) NOT NULL,
    message TEXT,
    admin_info JSONB,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published TIMESTAMP,
    expired TIMESTAMP,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_by_admin BOOLEAN NOT NULL,
    product_dto JSONB NOT NULL,
    UNIQUE (supplier_id, supplier_ref)
);
