CREATE TABLE IF NOT EXISTS digitalsoknadsortiment_reg_v1 (
    id UUID NOT NULL PRIMARY KEY,
    sortiment_kategori VARCHAR(255) NOT NULL,
    post_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    updated_by_user VARCHAR(255) NOT NULL,
    created_by_user VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated TIMESTAMP,
    UNIQUE (sortiment_kategori, post_id)
);
