UPDATE series_reg_v1 s SET admin_status = 'APPROVED'
WHERE s.status = 'ACTIVE'
  AND s.admin_status = 'PENDING'
  AND NOT EXISTS(
    SELECT 1 FROM product_reg_v1 p
    WHERE p.registration_status = 'ACTIVE'
      AND p.admin_status = 'PENDING'
      AND p.series_uuid = s.id
)
  AND EXISTS(
    SELECT 1 FROM product_reg_v1 p2
    WHERE p2.registration_status = 'ACTIVE'
      AND p2.admin_status = 'APPROVED'
      AND p2.series_uuid = s.id
);