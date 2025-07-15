ALTER TABLE series_reg_v1
    ADD COLUMN count_drafts INT DEFAULT 0;

UPDATE series_reg_v1
SET count_drafts = b.b_count FROM (SELECT series_uuid, count(*) as b_count
     FROM product_reg_v1
     WHERE draft_status = 'DRAFT'
      AND registration_status = 'ACTIVE'
      AND admin_status != 'REJECTED'
     GROUP BY series_uuid)  AS b
WHERE id = b.series_uuid;

ALTER TABLE series_reg_v1
    ADD COLUMN count_published INT DEFAULT 0;

UPDATE series_reg_v1
SET count_published = b.b_count FROM (SELECT series_uuid, count(*) as b_count
     FROM product_reg_v1
     WHERE draft_status = 'DONE'
      AND registration_status = 'ACTIVE'
      AND admin_status = 'APPROVED'
     GROUP BY series_uuid)  AS b
WHERE id = b.series_uuid;

ALTER TABLE series_reg_v1
    ADD COLUMN count_pending INT DEFAULT 0;

UPDATE series_reg_v1
SET count_pending = b.b_count FROM (SELECT series_uuid, count(*) as b_count
     FROM product_reg_v1
     WHERE draft_status = 'DONE'
       AND registration_status = 'ACTIVE'
       AND admin_status = 'PENDING'
     GROUP BY series_uuid)  AS b
WHERE id = b.series_uuid;

ALTER TABLE series_reg_v1
    ADD COLUMN count_declined INT DEFAULT 0;

UPDATE series_reg_v1
SET count_declined = b.b_count FROM (SELECT series_uuid, count(*) as b_count
     FROM product_reg_v1
     WHERE draft_status = 'DRAFT'
       AND registration_status = 'ACTIVE'
       AND admin_status = 'REJECTED'
     GROUP BY series_uuid)  AS b
WHERE id = b.series_uuid;