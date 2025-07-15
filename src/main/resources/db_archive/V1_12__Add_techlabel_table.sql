CREATE TABLE IF NOT EXISTS techlabel_reg_v1
(
    id              UUID          NOT NULL,
    identifier      VARCHAR(255)  NOT NULL,
    label           VARCHAR(255)  NOT NULL,
    guide           VARCHAR(1024) NOT NULL,
    iso_code        VARCHAR(255)  NOT NULL,
    type            VARCHAR(32)   NOT NULL,
    unit            VARCHAR(64),
    is_active       BOOLEAN       NOT NULL,
    created_by_user VARCHAR(32)   NOT NULL,
    updated_by_user VARCHAR(32)   NOT NULL,
    created_by      VARCHAR(32)   NOT NULL,
    updated_by      VARCHAR(32)   NOT NULL,
    updated         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (label),
    UNIQUE (identifier)
);

