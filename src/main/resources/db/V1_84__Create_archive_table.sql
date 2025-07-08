CREATE TABLE IF NOT EXISTS archive (
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

CREATE INDEX IF NOT EXISTS idx_archive_oid ON archive (oid);
CREATE INDEX IF NOT EXISTS idx_archive_status ON archive (status);