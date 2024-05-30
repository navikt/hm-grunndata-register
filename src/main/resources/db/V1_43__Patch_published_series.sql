UPDATE series_reg_v1
SET published = product_reg_v1.published FROM product_reg_v1
WHERE product_reg_v1.series_uuid = series_reg_v1.id
  AND series_reg_v1.published is NULL
  AND product_reg_v1.published IS NOT NULL;