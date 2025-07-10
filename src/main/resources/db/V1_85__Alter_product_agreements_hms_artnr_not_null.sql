DELETE FROM product_agreement_reg_v1
WHERE hms_artnr IS NULL;
ALTER TABLE product_agreement_reg_v1
    ALTER COLUMN hms_artnr SET NOT NULL;