CREATE TABLE catalog_import_v1 (
    id uuid NOT NULL PRIMARY KEY,
    agreement_action VARCHAR(32) NOT NULL,
    order_ref VARCHAR(255) NOT NULL,
    hms_art_nr VARCHAR(255) NOT NULL,
    iso VARCHAR(32) NOT NULL,
    title TEXT NOT NULL,
    supplier_ref VARCHAR(255) NOT NULL,
    reference VARCHAR(255) NOT NULL,
    post_nr VARCHAR(32) NOT NULL,
    date_from VARCHAR(32) NOT NULL,
    date_to VARCHAR(32) NOT NULL,
    article_action VARCHAR(32) NOT NULL,
    article_type VARCHAR(32) NOT NULL,
    functional_change VARCHAR(32) NOT NULL,
    for_children VARCHAR(32) NOT NULL,
    supplier_name VARCHAR(255) NOT NULL,
    supplier_city VARCHAR(255) NOT NULL,
    main_product BOOLEAN NOT NULL,
    spare_part BOOLEAN NOT NULL,
    accessory BOOLEAN NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX catalog_import_v1_order_ref_idx ON catalog_import_v1 (order_ref);
CREATE INDEX catalog_import_v1_hms_art_nr_idx ON catalog_import_v1 (hms_art_nr);
CREATE UNIQUE INDEX catalog_import_v1_order_ref_supplier_ref_idx ON catalog_import_v1 (order_ref, supplier_ref);
