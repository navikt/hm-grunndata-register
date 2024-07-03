CREATE TABLE IF NOT EXISTS paakrevdgodkjenningskurs_reg_v1 (
    id UUID NOT NULL PRIMARY KEY,
    isokode VARCHAR(255) NOT NULL,
    tittel VARCHAR(255) NOT NULL,
    kurs_id NUMERIC NOT NULL,
    status VARCHAR(32) NOT NULL,
    updated_by_user VARCHAR(255) NOT NULL,
    created_by_user VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated TIMESTAMP,
    UNIQUE (isokode)
);
