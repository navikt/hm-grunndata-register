ALTER TABLE agreement_reg_v1
    ALTER COLUMN agreement_key SET NOT NULL;
ALTER TABLE agreement_reg_v1
    ADD CONSTRAINT agreement_key_unique UNIQUE (agreement_key);