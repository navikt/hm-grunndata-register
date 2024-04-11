ALTER TABLE series_reg_v1 ADD COLUMN count INT DEFAULT 0;
UPDATE series_reg_v1 SET count = b.b_count FROM (SELECT series_uuid, count(*) as b_count FROM product_reg_v1 GROUP BY series_uuid) AS b WHERE id = b.series_uuid;
