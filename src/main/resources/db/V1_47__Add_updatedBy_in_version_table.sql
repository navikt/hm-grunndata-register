ALTER TABLE series_reg_version_v1 ADD updated_by VARCHAR(64) DEFAULT 'system' NOT NULL;
ALTER TABLE product_reg_version_v1 ADD updated_by VARCHAR(64) DEFAULT 'system' NOT NULL;