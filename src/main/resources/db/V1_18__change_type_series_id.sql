ALTER TABLE product_agreement_reg_v1 DROP COLUMN series_id;
ALTER TABLE product_agreement_reg_v1 ADD COLUMN series_uuid uuid;

CREATE INDEX product_agreement_reg_v1_series_uuid_idx ON product_agreement_reg_v1 (series_uuid);
