CREATE TABLE IF NOT EXISTS bestillingsordning_reg_v1 (
    id UUID NOT NULL PRIMARY KEY,
    hms_art_nr VARCHAR(255) NOT NULL,
    navn VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    updated_by_user VARCHAR(255) NOT NULL,
    created_by_user VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated TIMESTAMP,
    UNIQUE (hms_art_nr)
)