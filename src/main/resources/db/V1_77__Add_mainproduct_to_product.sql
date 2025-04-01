ALTER TABLE product_reg_v1
    ADD COLUMN main_product BOOLEAN NOT NULL DEFAULT true;
UPDATE product_reg_v1
SET main_product = CASE
    WHEN accessory = false AND spare_part = false THEN true
    ELSE false
END;