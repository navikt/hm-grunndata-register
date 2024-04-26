ALTER TABLE product_reg_v1
    ADD CONSTRAINT articleNameSeriesUUIDUnique UNIQUE (article_name, series_uuid, registration_status);