UPDATE series_reg_v1 s
SET status = 'ACTIVE'
WHERE EXISTS(SELECT 1
             FROM product_reg_v1 p
             WHERE p.series_uuid = s.id
               AND (p.registration_status = 'ACTIVE'));