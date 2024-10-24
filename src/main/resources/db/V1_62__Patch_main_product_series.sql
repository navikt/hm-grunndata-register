
ALTER TABLE series_reg_v1 ADD COLUMN main_product BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE series_reg_v1 s
SET main_product = false
WHERE EXISTS
          (SELECT 1 FROM product_reg_v1  p WHERE p.series_uuid = s.id AND ( p.accessory is true OR p.spare_part is true));