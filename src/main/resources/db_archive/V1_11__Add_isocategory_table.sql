CREATE TABLE isocategory_reg_v1
(
    iso_code         VARCHAR(32)   NOT NULL PRIMARY KEY,
    iso_title        VARCHAR(1024) NOT NULL,
    iso_text         TEXT          NOT NULL,
    iso_text_short   TEXT          NOT NULL,
    iso_translations JSONB         NOT NULL,
    iso_level        INTEGER       NOT NULL,
    is_active        BOOLEAN       NOT NULL,
    show_tech        BOOLEAN       NOT NULL,
    allow_multi      BOOLEAN       NOT NULL,
    created_by_user  VARCHAR(32)   NOT NULL,
    updated_by_user  VARCHAR(32)   NOT NULL,
    created_by        VARCHAR(32)   NOT NULL,
    updated_by        VARCHAR(32)   NOT NULL,
    created          TIMESTAMP     NOT NULL,
    updated          TIMESTAMP     NOT NULL
)