ALTER TABLE product_agreement_reg_v1 ADD COLUMN spare_part BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE product_agreement_reg_v1 ADD COLUMN accessory BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE product_agreement_reg_v1
    ADD CONSTRAINT fk_product_reg_v1_product_agreement_reg_v1
        FOREIGN KEY (product_id) REFERENCES product_reg_v1 (id) ON DELETE RESTRICT;