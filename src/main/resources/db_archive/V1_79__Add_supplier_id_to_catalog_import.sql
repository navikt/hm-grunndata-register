ALTER TABLE catalog_import_v1 add column supplier_id uuid;
UPDATE catalog_import_v1
SET supplier_id = cf.supplier_id
FROM catalog_file_v1 cf
WHERE catalog_import_v1.order_ref = cf.order_ref;
ALTER TABLE catalog_import_v1 ALTER COLUMN supplier_id SET NOT NULL;