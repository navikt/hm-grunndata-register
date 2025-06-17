UPDATE product_reg_v1
SET created_by_admin = true
WHERE main_product IS false
AND created_by_admin IS false;

UPDATE series_reg_v1
SET created_by_admin = true
WHERE main_product IS false
AND created_by_admin IS false;