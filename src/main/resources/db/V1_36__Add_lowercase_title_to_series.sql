ALTER TABLE series_reg_v1
    ADD COLUMN title_lowercase VARCHAR(255) NOT NULL DEFAULT '';

UPDATE series_reg_v1
SET title_lowercase = lower(title);
