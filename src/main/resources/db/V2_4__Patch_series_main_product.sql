UPDATE series_reg_v1
SET main_product = false
WHERE id IN (
    SELECT s.id
    FROM series_reg_v1 s
             JOIN product_reg_v1 p ON p.series_uuid = s.id AND p.main_product != s.main_product
    );

