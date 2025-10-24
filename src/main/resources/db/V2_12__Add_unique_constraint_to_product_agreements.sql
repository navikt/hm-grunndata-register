ALTER TABLE product_agreement_reg_v1 ADD CONSTRAINT uq_product_agreement_reg_v1_productid_agreementid_postid
    UNIQUE (product_id, agreement_id, post_id);