DELETE FROM catalog_import_v1 WHERE id NOT IN (SELECT DISTINCT ON (hms_art_nr, order_ref) id
FROM catalog_import_v1 ORDER BY hms_art_nr, order_ref, updated DESC);

ALTER TABLE catalog_import_v1
    ADD CONSTRAINT catalog_import_v1_hms_art_nr_order_ref_unique
        UNIQUE (hms_art_nr, order_ref);