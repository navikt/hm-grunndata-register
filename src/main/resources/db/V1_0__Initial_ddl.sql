
CREATE SEQUENCE IF NOT EXISTS product_reg_v1_id_seq START WITH 1000;

CREATE TABLE IF NOT EXISTS product_reg_v1 (
    id NUMERIC(19,0) NOT NULL DEFAULT NEXTVAL('product_reg_v1_id_seq')
    uuid VARCHAR(36) NOT NULL,
    supplier_uuid VARCHAR(36) NOT NULL,
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
    PRIMARY KEY(id),
    UNIQUE(uuid),
    UNIQUE (supplier_id, supplier_ref)
)
