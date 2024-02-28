ALTER TABLE delkontrakt_reg_v1 ALTER COLUMN agreement_id SET NOT NULL;
CREATE INDEX delkontrakt_reg_v1_agreement_id_idx ON delkontrakt_reg_v1 (agreement_id);