ALTER TABLE product_agreement_reg_v1 ADD COLUMN series_id uuid;
ALTER TABLE product_agreement_reg_v1 ADD COLUMN article_name VARCHAR(255);


CREATE INDEX product_agreement_reg_v1_series_id_idx ON product_agreement_reg_v1 (series_id);