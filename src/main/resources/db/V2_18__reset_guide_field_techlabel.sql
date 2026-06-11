UPDATE techlabel_reg_v1 SET guide = '';
ALTER TABLE techlabel_reg_v1 ALTER COLUMN guide TYPE text;
ALTER TABLE techlabel_reg_v1 ALTER COLUMN guide SET DEFAULT '';
ALTER TABLE techlabel_reg_v1 ALTER COLUMN guide SET NOT NULL;
