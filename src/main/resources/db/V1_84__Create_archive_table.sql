CREATE TABLE IF NOT EXISTS archive_v1 (
    id UUID PRIMARY KEY,
    oid UUID NOT NULL,
    keywords TEXT NOT NULL DEFAULT '',
    payload TEXT NOT NULL DEFAULT '{}',
    type VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created TIMESTAMP NOT NULL,
    dispose_time TIMESTAMP NOT NULL,
    archived_by_user VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_archive_v1_oid ON archive_v1 (oid);
CREATE INDEX IF NOT EXISTS idx_archive_v1_status ON archive_v1 (status);
CREATE INDEX IF NOT EXISTS idx_archive_v1_keywords ON archive_v1 (keywords);