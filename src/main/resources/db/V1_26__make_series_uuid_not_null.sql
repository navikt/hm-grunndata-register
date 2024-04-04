UPDATE product_reg_v1 SET series_uuid = id WHERE series_uuid IS NULL;
ALTER TABLE product_reg_v1 ALTER COLUMN series_uuid SET NOT NULL;
