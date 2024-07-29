UPDATE product_reg_v1 SET expired = '2026-01-01 00:00:00' WHERE expired IS NULL;
ALTER TABLE product_reg_v1 ALTER COLUMN expired SET NOT NULL;