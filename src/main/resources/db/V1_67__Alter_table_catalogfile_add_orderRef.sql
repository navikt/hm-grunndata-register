ALTER TABLE catalog_file_v1 ADD COLUMN order_ref VARCHAR(255) NOT NULL DEFAULT '0';
UPDATE catalog_file_v1 SET order_ref = catalog_list->0->>'bestillingsNr';