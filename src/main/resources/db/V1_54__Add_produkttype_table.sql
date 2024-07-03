CREATE TABLE IF NOT EXISTS produkttype_reg_v1 (
    id UUID NOT NULL PRIMARY KEY,
    isokode VARCHAR(255) NOT NULL,
    produkttype VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    updated_by_user VARCHAR(255) NOT NULL,
    created_by_user VARCHAR(255) NOT NULL,
    created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated TIMESTAMP,
    UNIQUE (sortiment_kategori, post_id)
);
