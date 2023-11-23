CREATE TABLE product_agreement_reg_v1 (
    id uuid PRIMARY KEY,
    product_id uuid,
    supplier_id uuid NOT NULL,
    supplier_ref VARCHAR(255) NOT NULL,
    hms_artnr VARCHAR(255),
    agreement_id uuid NOT NULL,
    reference VARCHAR(255) NOT NULL,
    post INTEGER NOT NULL,
    rank INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT  CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT  CURRENT_TIMESTAMP,
    published TIMESTAMP NOT NULL DEFAULT  CURRENT_TIMESTAMP,
    expired TIMESTAMP NOT NULL DEFAULT  CURRENT_TIMESTAMP,
    UNIQUE (supplier_id, supplier_ref, agreement_id, reference, post, rank)
);

CREATE INDEX product_agreement_reg_v1_supplier_id_supplier_ref_idx ON product_agreement_reg_v1 (supplier_id, supplier_ref);
CREATE INDEX product_agreement_reg_v1_agreement_id_idx ON product_agreement_reg_v1 (agreement_id);
