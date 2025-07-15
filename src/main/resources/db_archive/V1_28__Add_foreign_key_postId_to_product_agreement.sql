ALTER TABLE product_agreement_reg_v1
    ADD CONSTRAINT fk_post_id FOREIGN KEY (post_id) REFERENCES delkontrakt_reg_v1 (id) ON DELETE CASCADE;