ALTER TABLE product_reg_v1 ADD COLUMN series_uuid uuid;

CREATE INDEX product_reg_v1_series_uuid_idx ON product_reg_v1 (series_uuid);